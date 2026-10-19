package com.backstage.framework.consumer;


import com.backstage.common.constant.KafkaConstants;
import com.backstage.system.domain.message.order.PaySuccessMessage;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.order.OrderEntitlementTaskService;
import com.backstage.system.service.order.OrderPaidHandlerRegistry;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 支付成功消息kafka消费者，
 */
@Component
public class PaymentSuccessConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSuccessConsumer.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private OshOrderMapper orderMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Resource
    private OrderPaidHandlerRegistry paidHandlerRegistry;

    @Resource
    private OrderEntitlementTaskService entitlementTaskService;

    @KafkaListener(
            topics = KafkaConstants.PAY_SUCCESS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeUserEvent(String message, Acknowledgment ack) {
        log.info("【Kafka消费者】收到支付成功消息：{}", message);

        // 消息解析失败属于不可恢复的脏数据：ack 丢弃，避免 Kafka 无限重投
        PaySuccessMessage successMessage;
        try {
            successMessage = objectMapper.readValue(message, PaySuccessMessage.class);
        } catch (Exception e) {
            log.error("【Kafka消费者】支付成功消息解析失败，丢弃，message={}", message, e);
            ack.acknowledge();
            return;
        }

        String orderNo = successMessage.getOrderNo();
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (Objects.isNull(order) || Objects.isNull(order.getProductType())) {
            log.warn("权益发放跳过，订单信息不完整: orderNo={}", orderNo);
            ack.acknowledge();
            return;
        }

        ProductTypeEnum productTypeEnum = ProductTypeEnum.fromCode(order.getProductType());
        if (Objects.isNull(productTypeEnum)) {
            log.warn("权益发放跳过，未知商品类型: orderNo={}, productType={}", orderNo, order.getProductType());
            // 脏数据（未知商品类型）重试无意义，ack 丢弃（原实现漏 ack 导致无限重投）
            ack.acknowledge();
            return;
        }

        // 权益发放：
        // - task 模式由任务表 + 定时兜底接管重试（processOrderSafely 内部吞异常）；
        // - 旧 Kafka 行为（非 task 模式）发放失败抛异常时不 ack，利用消息重投实现重试语义。
        try {
            if (entitlementTaskService.isTaskMode()) {
                // task 模式可接住升级前的 Kafka 积压：先补建唯一任务，再由 CAS 任务处理。
                entitlementTaskService.ensureTask(orderNo);
                entitlementTaskService.processOrderSafely(orderNo);
            } else {
                // 蓝绿兼容阶段保持旧 Kafka 行为，数据库任务只写不执行。
                paidHandlerRegistry.handle(productTypeEnum.getName(), orderNo);
            }
        } catch (Exception e) {
            log.error("【Kafka消费者】权益发放失败，等待重投重试，orderNo={}", orderNo, e);
            return;
        }

        // 通知失败与权益发放隔离：通知异常不应触发重投，避免权益被重复处理
        try {
            // 调用 websocket 让前端发送通知告知用户支付已完成。
            WsNotifyMessage msg = new WsNotifyMessage();
            msg.setType("PAY_SUCCESS_NOTIFY");
            msg.setTitle("您购买的 " + order.getProductName() + "支付成功！");
            msg.setContent(null);
            msg.setJumpUrl(null);
            msg.setBizId(orderNo);
            webSocketNotifyService.send(order.getUserId(), msg);
        } catch (Exception e) {
            log.warn("【Kafka消费者】支付成功通知发送失败，orderNo={}, error={}", orderNo, e.getMessage());
        }

        ack.acknowledge();
    }
}
