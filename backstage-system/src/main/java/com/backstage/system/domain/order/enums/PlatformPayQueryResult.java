package com.backstage.system.domain.order.enums;

/**
 * 主动查单结果。UNKNOWN 不能当成未支付，更不能据此关闭本地订单。
 */
public enum PlatformPayQueryResult {
    PAID,
    /** 平台确认仍可支付，取消时还需要先调用平台关单。 */
    UNPAID,
    /** 平台确认已成功关单，本地应同步关闭并释放占用资源。 */
    CLOSED,
    /** 平台确认支付失败终态，本地可以直接失败并关闭，无需再次关单。 */
    FAILED,
    UNKNOWN
}
