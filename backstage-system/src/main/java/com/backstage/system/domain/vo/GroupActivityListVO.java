package com.backstage.system.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团活动列表VO（C端展示用）
 * 
 * @author system
 * @date 2026-04-18
 */
public class GroupActivityListVO {
    
    /** 拼团ID */
    private Long id;
    
    /** 拼团标题 */
    private String title;
    
    /** 服务器CPU配置 */
    private String cpu;
    
    /** 服务器内存配置 */
    private String memory;
    
    /** 服务器存储配置 */
    private String storage;
    
    /** 拼团价格（按月计算，完整周期价格） */
    private BigDecimal basePrice;
    
    /** 当前参团实际价格（按剩余月数比例计算） */
    private BigDecimal currentPrice;
    
    /** 服务器总使用时长（月） */
    private Integer totalDuration;
    
    /** 剩余可使用月数（成团后动态计算，未成团为null） */
    private BigDecimal remainingMonths;
    
    /** 拼团开始时间 */
    private LocalDateTime startTime;
    
    /** 服务器开始使用时间（成团后有值，未成团为null） */
    private LocalDateTime serverStartTime;
    
    /** 服务器使用结束时间（成团后有值，未成团为null） */
    private LocalDateTime serverEndTime;
    
    /** 活动状态：1-进行中 2-拼团成功 3-已结束 */
    private Integer status;
    
    /** 当前已参团人数 */
    private Integer currentNum;
    
    /** 拼团所需最低人数 */
    private Integer groupMinNum;
    
    /** 拼团人数上限 */
    private Integer groupMaxNum;
    
    /** 还差几人成团 */
    private Integer remainNum;
    
    /** 招募截止时间（未成团时有效） */
    private LocalDateTime expireTime;
    
    /** 是否已成团（current_num >= group_min_num） */
    private Boolean isSuccess;
    
    /** 当前是否可参团 */
    private Boolean canJoin;
    
    /** 服务器配置教程URL */
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
    
    public LocalDateTime getServerEndTime() {
        return serverEndTime;
    }
    
    public void setServerEndTime(LocalDateTime serverEndTime) {
        this.serverEndTime = serverEndTime;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
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
    
    public LocalDateTime getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
    
    public Boolean getIsSuccess() {
        return isSuccess;
    }
    
    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
    
    public Boolean getCanJoin() {
        return canJoin;
    }
    
    public void setCanJoin(Boolean canJoin) {
        this.canJoin = canJoin;
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
