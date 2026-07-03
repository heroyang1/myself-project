package com.backstage.system.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.backstage.system.domain.vo.ServerGroupUserVo;

/**
 * 拼团详情VO
 * 
 * @author system
 * @date 2026-04-18
 */
public class GroupDetailVO {
    
    /** 拼团活动ID */
    private Long id;
    
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
    
    /** 当前参团实际价格 */
    private BigDecimal currentPrice;
    
    /** 会员价格（当前价格的八折） */
    private BigDecimal memberPrice;
    
    /** 服务器总使用时长（月） */
    private Integer totalDuration;
    
    /** 剩余可使用月数 */
    private BigDecimal remainingMonths;
    
    /** 当前已参团人数 */
    private Integer currentNum;
    
    /** 拼团所需最低人数 */
    private Integer groupMinNum;
    
    /** 拼团人数上限 */
    private Integer groupMaxNum;
    
    /** 还差几人成团 */
    private Integer remainNum;
    
    /** 活动状态：1-进行中 2-拼团成功 3-已结束 */
    private Integer status;
    
    /** 组团状态：0-进行中（未成团） 1-已成团 2-已结束 */
    private Integer groupStatus;
    
    /** 组团状态文字描述 */
    private String groupStatusText;
    
    /** 拼团开始时间 */
    private LocalDateTime startTime;
    
    /** 招募截止时间（未成团时有效） */
    private LocalDateTime expireTime;
    
    /** 支付截止时间（待支付订单有效，创建后30分钟） */
    private LocalDateTime payExpireTime;
    
    /** 服务器开始使用时间（成团后有值） */
    private LocalDateTime serverStartTime;
    
    /** 服务器到期时间（成团后有值） */
    private LocalDateTime serverExpireTime;
    
    /** 参团用户列表 */
    private List<ServerGroupUserVo> users;
    
    /** 当前登录用户是否已参团 */
    private Boolean currentUserJoined;
    
    /** 当前是否可以参团 */
    private Boolean canJoin;
    
    /** 管理员联系方式 */
    private String adminContact;
    
    /** 服务器配置教程URL（成团后可见） */
    private String serverTutorialUrl;
    
    /** 封面图片URL */
    private String cover;
    
    // Getter and Setter
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public BigDecimal getMemberPrice() {
        return memberPrice;
    }
    
    public void setMemberPrice(BigDecimal memberPrice) {
        this.memberPrice = memberPrice;
    }
    
    public Integer getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    public BigDecimal getRemainingMonths() {
        return remainingMonths;
    }
    
    public void setRemainingMonths(BigDecimal remainingMonths) {
        this.remainingMonths = remainingMonths;
    }
    
    public Integer getCurrentNum() {
        return currentNum;
    }
    
    public void setCurrentNum(Integer currentNum) {
        this.currentNum = currentNum;
    }
    
    public Integer getGroupMinNum() {
        return groupMinNum;
    }
    
    public void setGroupMinNum(Integer groupMinNum) {
        this.groupMinNum = groupMinNum;
    }
    
    public Integer getGroupMaxNum() {
        return groupMaxNum;
    }
    
    public void setGroupMaxNum(Integer groupMaxNum) {
        this.groupMaxNum = groupMaxNum;
    }
    
    public Integer getRemainNum() {
        return remainNum;
    }
    
    public void setRemainNum(Integer remainNum) {
        this.remainNum = remainNum;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
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
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
    
    public LocalDateTime getPayExpireTime() {
        return payExpireTime;
    }
    
    public void setPayExpireTime(LocalDateTime payExpireTime) {
        this.payExpireTime = payExpireTime;
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
    
    public List<ServerGroupUserVo> getUsers() {
        return users;
    }
    
    public void setUsers(List<ServerGroupUserVo> users) {
        this.users = users;
    }
    
    public Boolean getCurrentUserJoined() {
        return currentUserJoined;
    }
    
    public void setCurrentUserJoined(Boolean currentUserJoined) {
        this.currentUserJoined = currentUserJoined;
    }
    
    public Boolean getCanJoin() {
        return canJoin;
    }
    
    public void setCanJoin(Boolean canJoin) {
        this.canJoin = canJoin;
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
    
    public String getCover() {
        return cover;
    }
    
    public void setCover(String cover) {
        this.cover = cover;
    }
}
