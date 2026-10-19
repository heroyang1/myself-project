package com.backstage.contract.internal.trade;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 内部订单结算响应
 *
 * <p>映射自单体 {@code OrderCheckoutRespVO}，扁平化处理（不暴露嵌套支付对象），
 * 仅返回调用方后续流程所需字段。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class OrderCheckoutResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否需要支付：true-需要拉起支付 false-零元单/积分全额抵扣已直接完成
     */
    private boolean needPay;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 支付状态
     */
    private String payStatus;

    /**
     * 订单金额
     */
    private BigDecimal price;

    /**
     * 本单使用积分数量
     */
    private Long pointsUsed;

    /**
     * 积分抵扣金额
     */
    private BigDecimal pointsDeductAmount;

    /**
     * 抵扣后剩余积分
     */
    private Long remainingPoints;

    /**
     * 订单过期时间（字符串，格式与单体保持一致）
     */
    private String expireTime;

    /**
     * 关单倒计时（分钟）
     */
    private Integer closeExpireMinutes;

    public boolean isNeedPay() {
        return needPay;
    }

    public void setNeedPay(boolean needPay) {
        this.needPay = needPay;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(Long pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public BigDecimal getPointsDeductAmount() {
        return pointsDeductAmount;
    }

    public void setPointsDeductAmount(BigDecimal pointsDeductAmount) {
        this.pointsDeductAmount = pointsDeductAmount;
    }

    public Long getRemainingPoints() {
        return remainingPoints;
    }

    public void setRemainingPoints(Long remainingPoints) {
        this.remainingPoints = remainingPoints;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getCloseExpireMinutes() {
        return closeExpireMinutes;
    }

    public void setCloseExpireMinutes(Integer closeExpireMinutes) {
        this.closeExpireMinutes = closeExpireMinutes;
    }
}
