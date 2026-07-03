package com.backstage.system.domain.servergroup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务器拼团参与记录实体
 * 
 * @author system
 * @date 2026-04-18
 */
public class OshGroupWork {
    
    /** 参团记录ID（主键） */
    private Long id;
    
    /** 关联拼团活动ID */
    private Long groupActivityId;
    
    /** 团长用户ID */
    private Long leaderUserId;
    
    /** 参团用户ID */
    private Long userId;
    
    /** 关联订单ID */
    private Long orderId;
    
    /** 用户实际支付价格（根据剩余月数动态计算） */
    private BigDecimal actualPrice;
    
    /** 参团时剩余可使用月数 */
    private BigDecimal remainingMonths;
    
    /** 组团状态：0-待支付 1-已支付（待成团） 2-已成团 3-已结束/已取消 */
    private Integer groupStatus;
    
    /** 参团时间 */
    private LocalDateTime joinTime;
    
    /** 服务器开始使用时间（成团后有值） */
    private LocalDateTime serverStartTime;
    
    /** 服务器到期时间（成团后有值） */
    private LocalDateTime serverExpireTime;
    
    /** SSH连接服务器IP */
    private String serverIp;
    
    /** SSH连接端口 */
    private Integer sshPort;
    
    /** SSH连接用户名 */
    private String sshUsername;
    
    /** SSH连接密码 */
    private String sshPassword;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    // Getter and Setter
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getGroupActivityId() {
        return groupActivityId;
    }
    
    public void setGroupActivityId(Long groupActivityId) {
        this.groupActivityId = groupActivityId;
    }
    
    public Long getLeaderUserId() {
        return leaderUserId;
    }
    
    public void setLeaderUserId(Long leaderUserId) {
        this.leaderUserId = leaderUserId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public BigDecimal getActualPrice() {
        return actualPrice;
    }
    
    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }
    
    public BigDecimal getRemainingMonths() {
        return remainingMonths;
    }
    
    public void setRemainingMonths(BigDecimal remainingMonths) {
        this.remainingMonths = remainingMonths;
    }
    
    public Integer getGroupStatus() {
        return groupStatus;
    }
    
    public void setGroupStatus(Integer groupStatus) {
        this.groupStatus = groupStatus;
    }
    
    public LocalDateTime getJoinTime() {
        return joinTime;
    }
    
    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
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
    
    public String getServerIp() {
        return serverIp;
    }
    
    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
    
    public Integer getSshPort() {
        return sshPort;
    }
    
    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
    
    public String getSshUsername() {
        return sshUsername;
    }
    
    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }
    
    public String getSshPassword() {
        return sshPassword;
    }
    
    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
