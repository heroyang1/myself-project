package com.backstage.common.constant;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 九转苍翎
 * Date: 2026/4/12
 * Time: 16:47
 */
public class KafkaConstants {
    public static final String USER_ACTION_TOPIC = "user-action";
    public static final String COURSE_INDEX_TOPIC = "osh.course.index";
    public static final String PAY_SUCCESS_TOPIC = "pay-success-topic";
    /** 秒杀订单创建 Topic */
    public static final String SECKILL_ORDER_CREATE_TOPIC = "seckill.order.create";
    public static final String TOOL_INDEX_TOPIC = "osh.tool.index";
    /** 秒杀商品明细索引 Topic */
    public static final String SECKILL_ITEM_INDEX_TOPIC = "osh.seckill.item.index";

    // ==================== 拼团模块 Topic ====================
    /** 拼团状态变更事件（成团/满员/状态变更），WebSocket 推送 + Flink 实时统计消费 */
    public static final String GROUP_STATUS_TOPIC = "osh.group.status";
    /** 拼团订单事件（下单/支付/取消），Flink 订单分析 + 对账消费 */
    public static final String GROUP_ORDER_TOPIC = "osh.group.order";
    /** 拼团活动索引同步事件，Flink → ES 搜索索引消费 */
    public static final String GROUP_ACTIVITY_INDEX_TOPIC = "osh.group.activity.index";
}
