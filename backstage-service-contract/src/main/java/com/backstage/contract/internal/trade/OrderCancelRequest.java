package com.backstage.contract.internal.trade;

import java.io.Serializable;

/**
 * 内部关单请求
 *
 * <p>供 tool-service 在点数购买 SAGA 补偿或用户主动取消时，按订单号关闭待支付订单。
 * 映射到单体 {@code OrderService.cancelPaymentByOrderNo}。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class OrderCancelRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号（非空）
     */
    private String orderNo;

    /**
     * 关单原因（可空，用于审计日志）
     */
    private String reason;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
