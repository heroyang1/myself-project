package com.backstage.system.domain.vo.group;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团订单状态查询响应VO
 * 
 * @author system
 * @date 2026-07-09
 */
public class GroupOrderStatusVO {

    /** 订单号 */
    private String orderNo;

    /** 订单状态（文字） */
    private String status;

    /** 支付时间 */
    private LocalDateTime paidTime;

    /** 应付金额 */
    private BigDecimal payableAmount;

    /** 商品名称 */
    private String productName;

    /** 支付倒计时（秒，仅待支付订单有值） */
    private Long countdownSeconds;

    public GroupOrderStatusVO() {}

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPaidTime() { return paidTime; }
    public void setPaidTime(LocalDateTime paidTime) { this.paidTime = paidTime; }

    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(Long countdownSeconds) { this.countdownSeconds = countdownSeconds; }
}
