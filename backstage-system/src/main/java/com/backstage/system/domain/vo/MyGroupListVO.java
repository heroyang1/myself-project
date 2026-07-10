package com.backstage.system.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的拼团列表VO（C端用户个人参团记录）
 * 
 * @author system
 * @date 2026-04-18
 */
public class MyGroupListVO {
    
    /** 参团记录ID */
    private Long groupWorkId;

    /** 拼团来源：system/initiated */
    private String sourceType;

    /** 来源表中的稳定ID：系统活动ID或用户发起记录ID */
    private Long sourceId;
    
    /** 拼团活动ID */
    private Long activityId;
    
    /** 拼团标题 */
    private String title;
    
    /** 服务器CPU配置 */
    private String cpu;
    
    /** 服务器内存配置 */
    private String memory;
    
    /** 服务器存储配置 */
    private String storage;
    
    /** 基础拼团价格 */
    private BigDecimal basePrice;
    
    /** 实际支付价格 */
    private BigDecimal actualPrice;
    
    /** 服务器总使用时长（月） */
    private Integer totalDuration;
    
    /** 拼团开始时间 */
    private LocalDateTime startTime;
    
    /** 服务器开始使用时间（成团后有值） */
    private LocalDateTime serverStartTime;
    
    /** 服务器到期时间（成团后有值） */
    private LocalDateTime serverExpireTime;
    
    /** 组团状态：0-进行中 1-已成团 2-已结束 */
    private Integer groupStatus;
    
    /** 组团状态文字描述 */
    private String groupStatusText;
    
    /** 订单编号 */
    private String orderNo;
    
    /** 订单状态：pending-待支付 paid-已支付 success-拼团成功 refunded-已退款 cancelled-已取消 */
    private String orderStatus;
    
    /** 订单状态文字描述 */
    private String orderStatusText;
    
    /** 支付截止时间（待支付订单有效，创建后30分钟） */
    private LocalDateTime payExpireTime;
    
    /** 参团时间 */
    private LocalDateTime joinTime;
    
    /** 管理员联系方式（脱敏显示） */
    private String adminContact;
    
    /** 服务器配置教程URL（成团后可见） */
    private String serverTutorialUrl;
    
    // Getter and Setter
    
    public Long getGroupWorkId() {
        return groupWorkId;
    }
    
    public void setGroupWorkId(Long groupWorkId) {
        this.groupWorkId = groupWorkId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
    
    public Long getActivityId() {
        return activityId;
    }
    
    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getCpu() {
        return cpu;
    }
    
    public void setCpu(String cpu) {
        this.cpu = cpu;
    }
    
    public String getMemory() {
        return memory;
    }
    
    public void setMemory(String memory) {
        this.memory = memory;
    }
    
    public String getStorage() {
        return storage;
    }
    
    public void setStorage(String storage) {
        this.storage = storage;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public BigDecimal getActualPrice() {
        return actualPrice;
    }
    
    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }
    
    public Integer getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getServerStartTime() {
        return serverStartTime;
    }
    
    public void setServerStartTime(LocalDateTime serverStartTime) {
        this.serverStartTime = serverStartTime;
    }
    
    public LocalDateTime getServerExpireTime() {
        return serverExpireTime;
    }
    
    public void setServerExpireTime(LocalDateTime serverExpireTime) {
        this.serverExpireTime = serverExpireTime;
    }
    
    public Integer getGroupStatus() {
        return groupStatus;
    }
    
    public void setGroupStatus(Integer groupStatus) {
        this.groupStatus = groupStatus;
    }
    
    public String getGroupStatusText() {
        return groupStatusText;
    }
    
    public void setGroupStatusText(String groupStatusText) {
        this.groupStatusText = groupStatusText;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public String getOrderStatusText() {
        return orderStatusText;
    }
    
    public void setOrderStatusText(String orderStatusText) {
        this.orderStatusText = orderStatusText;
    }
    
    public LocalDateTime getPayExpireTime() {
        return payExpireTime;
    }
    
    public void setPayExpireTime(LocalDateTime payExpireTime) {
        this.payExpireTime = payExpireTime;
    }
    
    public LocalDateTime getJoinTime() {
        return joinTime;
    }
    
    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
    
    public String getAdminContact() {
        return adminContact;
    }
    
    public void setAdminContact(String adminContact) {
        this.adminContact = adminContact;
    }
    
    public String getServerTutorialUrl() {
        return serverTutorialUrl;
    }
    
    public void setServerTutorialUrl(String serverTutorialUrl) {
        this.serverTutorialUrl = serverTutorialUrl;
    }
}
