package com.backstage.system.service.servergroup.event;

import com.backstage.common.constant.KafkaConstants;
import com.backstage.common.utils.kafka.KafkaMessageUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.ttl.threadpool.TtlExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 拼团事件发布器
 *
 * 将拼团业务事件异步发布到 Kafka，供 WebSocket 推送服务、Flink 实时统计、ES 索引同步等下游消费。
 *
 * <p>事件类型（eventType）：</p>
 * <ul>
 *   <li>{@code GROUP_JOIN} — 用户参团（创建订单 + 参团记录）</li>
 *   <li>{@code ORDER_PAID} — 订单支付成功</li>
 *   <li>{@code GROUP_FORMED} — 拼团成功（达到最低成团人数）</li>
 *   <li>{@code GROUP_FULL} — 拼团满员结束（达到最大人数）</li>
 *   <li>{@code ORDER_CANCELLED} — 订单取消</li>
 *   <li>{@code STATUS_CHANGED} — 活动状态变更（定时任务触发）</li>
 *   <li>{@code ACTIVITY_INDEX_CREATE/UPDATE/DELETE} — 活动索引同步事件</li>
 * </ul>
 *
 * <p>消息均使用 {@code activityId} 作为 Kafka partition key，保证同一活动的事件有序。</p>
 *
 * @author system
 * @date 2026-07-12
 */
@Component
public class GroupEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(GroupEventPublisher.class);

    /** TTL 包装的异步执行器，确保 CompletableFuture.runAsync 中 ThreadLocal 上下文不丢失 */
    private static final Executor TTL_EXECUTOR =
            TtlExecutors.getTtlExecutor(ForkJoinPool.commonPool());

    // ==================== 状态变更事件（osh.group.status） ====================

    /**
     * 发布成团事件
     *
     * @param activityId 活动ID
     * @param currentNum 当前参与人数
     * @param userId     触发成团的用户ID
     * @param sourceType 来源：system / initiated
     */
    public void publishGroupFormed(Long activityId, Integer currentNum, Long userId, String sourceType) {
        publishStatusEvent("GROUP_FORMED", activityId, currentNum, userId, sourceType);
    }

    /**
     * 发布满员结束事件
     */
    public void publishGroupFull(Long activityId, Integer currentNum, Long userId, String sourceType) {
        publishStatusEvent("GROUP_FULL", activityId, currentNum, userId, sourceType);
    }

    /**
     * 发布活动状态变更事件（定时任务批量触发）
     */
    public void publishStatusChanged(Long activityId, Integer newStatus) {
        Map<String, Object> event = buildBaseEvent("STATUS_CHANGED", activityId, null);
        event.put("newStatus", newStatus);
        sendAsync(KafkaConstants.GROUP_STATUS_TOPIC, activityId, event);
    }

    // ==================== 订单事件（osh.group.order） ====================

    /**
     * 发布参团（下单）事件
     *
     * @param orderNo    订单号
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param price      订单金额
     * @param sourceType 来源
     */
    public void publishGroupJoin(String orderNo, Long activityId, Long userId,
                                 java.math.BigDecimal price, String sourceType) {
        Map<String, Object> event = buildOrderEvent("GROUP_JOIN", orderNo, activityId, userId, price, sourceType);
        sendAsync(KafkaConstants.GROUP_ORDER_TOPIC, activityId, event);
    }

    /**
     * 发布订单支付成功事件
     */
    public void publishOrderPaid(String orderNo, Long activityId, Long userId,
                                 java.math.BigDecimal price, String sourceType) {
        Map<String, Object> event = buildOrderEvent("ORDER_PAID", orderNo, activityId, userId, price, sourceType);
        sendAsync(KafkaConstants.GROUP_ORDER_TOPIC, activityId, event);
    }

    /**
     * 发布订单取消事件
     */
    public void publishOrderCancelled(String orderNo, Long activityId, Long userId, String sourceType) {
        Map<String, Object> event = buildOrderEvent("ORDER_CANCELLED", orderNo, activityId, userId, null, sourceType);
        sendAsync(KafkaConstants.GROUP_ORDER_TOPIC, activityId, event);
    }

    // ==================== 活动索引事件（osh.group.activity.index） ====================

    /**
     * 发布活动索引创建/更新事件（供 Flink → ES 同步）
     *
     * @param activity 活动实体（序列化为 JSON 作为消息体）
     * @param eventType CREATE / UPDATE / DELETE
     */
    public void publishActivityIndexEvent(Object activity, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());
            // 将活动实体平铺到事件 Map（Flink 侧需要直接读取字段）
            String activityJson = JSON.toJSONString(activity);
            event.putAll(JSON.parseObject(activityJson));
            Long activityId = JSON.parseObject(activityJson).getLong("id");
            sendAsync(KafkaConstants.GROUP_ACTIVITY_INDEX_TOPIC, activityId, event);
        } catch (Exception e) {
            logger.error("[拼团事件] 发布活动索引事件失败, eventType={}", eventType, e);
        }
    }

    // ==================== 内部方法 ====================

    private void publishStatusEvent(String eventType, Long activityId, Integer currentNum,
                                    Long userId, String sourceType) {
        Map<String, Object> event = buildBaseEvent(eventType, activityId, userId);
        event.put("currentNum", currentNum);
        event.put("sourceType", sourceType);
        sendAsync(KafkaConstants.GROUP_STATUS_TOPIC, activityId, event);
    }

    private Map<String, Object> buildBaseEvent(String eventType, Long activityId, Long userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("activityId", activityId);
        event.put("userId", userId);
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }

    private Map<String, Object> buildOrderEvent(String eventType, String orderNo, Long activityId,
                                                 Long userId, java.math.BigDecimal price, String sourceType) {
        Map<String, Object> event = buildBaseEvent(eventType, activityId, userId);
        event.put("orderNo", orderNo);
        event.put("price", price);
        event.put("sourceType", sourceType);
        return event;
    }

    /**
     * 异步发送 Kafka 消息（fire-and-forget），失败仅记录日志不抛异常。
     * partition key = activityId（保证同一活动的事件有序）。
     */
    private void sendAsync(String topic, Long activityId, Map<String, Object> event) {
        String key = activityId != null ? String.valueOf(activityId) : null;
        String value = JSON.toJSONString(event);
        CompletableFuture.runAsync(() -> {
            try {
                if (key != null) {
                    KafkaMessageUtil.sendMessage(topic, key, value);
                } else {
                    KafkaMessageUtil.sendMessage(topic, value);
                }
            } catch (Exception e) {
                logger.error("[拼团事件] Kafka 发送失败, topic={}, key={}, event={}", topic, key, value, e);
            }
        }, TTL_EXECUTOR);
    }
}
