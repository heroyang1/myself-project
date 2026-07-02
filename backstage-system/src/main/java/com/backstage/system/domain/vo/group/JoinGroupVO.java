package com.backstage.system.domain.vo.group;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 参与拼团响应VO
 * 
 * @author system
 * @date 2026-05-14
 */
public class JoinGroupVO {
    
    /** 订单号 */
    private String orderNo;
    
    /** 是否需要支付 */
    private Boolean needPay;
    
    /** 订单状态：pending-待支付 success-参团成功 */
    private String status;
    
    /** 订单金额 */
    private BigDecimal price;
    
    /** 提示消息 */
    private String message;
    
    /** 订单创建时间 */
    private String createTime;
    
    /** 支付过期时间（订单创建后30分钟，前端展示倒计时用） */
    private LocalDateTime payExpireTime;
    
    /** 支付剩余秒数（前端展示倒计时用） */
    private Long payExpireSeconds;
    
    public JoinGroupVO() {
    }
    
    public JoinGroupVO(String orderNo, Boolean needPay, String status, BigDecimal price, String message) {
        this.orderNo = orderNo;
        this.needPay = needPay;
        this.status = status;
        this.price = price;
        this.message = message;
    }
    
    /**
     * 创建待支付订单响应（含支付过期时间）
     */
    public static JoinGroupVO pendingPayment(String orderNo, BigDecimal price) {
        JoinGroupVO vo = new JoinGroupVO();
        vo.setOrderNo(orderNo);
        vo.setNeedPay(true);
        vo.setStatus("pending");
        vo.setPrice(price);
        vo.setMessage("请在30分钟内完成支付");
        vo.setCreateTime(LocalDateTime.now().toString());
        // 设置支付过期时间为30分钟后
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);
        vo.setPayExpireTime(expireTime);
        vo.setPayExpireSeconds(30 * 60L);
        return vo;
    }
    
    /**
     * 创建免费拼团成功响应
     */
    public static JoinGroupVO success(String message) {
        JoinGroupVO vo = new JoinGroupVO();
        vo.setNeedPay(false);
        vo.setStatus("success");
        vo.setPrice(BigDecimal.ZERO);
        vo.setMessage(message != null ? message : "参团成功");
        return vo;
    }
    
    // Getter and Setter
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public Boolean getNeedPay() {
        return needPay;
    }
    
    public void setNeedPay(Boolean needPay) {
        this.needPay = needPay;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getPayExpireTime() {
        return payExpireTime;
    }
    
    public void setPayExpireTime(LocalDateTime payExpireTime) {
        this.payExpireTime = payExpireTime;
    }
    
    public Long getPayExpireSeconds() {
        return payExpireSeconds;
    }
    
    public void setPayExpireSeconds(Long payExpireSeconds) {
        this.payExpireSeconds = payExpireSeconds;
    }
}
