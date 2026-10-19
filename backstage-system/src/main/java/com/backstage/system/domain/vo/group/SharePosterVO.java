package com.backstage.system.domain.vo.group;

import java.math.BigDecimal;

/**
 * 分享海报数据 VO
 * 前端根据此数据渲染 Canvas 海报或展示分享卡片
 * 
 * @author system
 * @date 2026-06-24
 */
public class SharePosterVO {

    /** 活动标题 */
    private String title;

    /** 活动描述 */
    private String description;

    /** 服务器配置摘要（如 "4核8G 100GB"） */
    private String serverSpec;

    /** 活动封面图完整URL */
    private String coverUrl;

    /** 当前价格 */
    private BigDecimal currentPrice;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 当前参团人数 */
    private Integer currentNum;

    /** 最低成团人数 */
    private Integer groupMinNum;

    /** 拼团进度百分比 0-100 */
    private Integer progressPercent;

    /** 分享链接（用于二维码生成） */
    private String shareUrl;

    /** 活动ID（前端拼接分享链接用） */
    private Long activityId;

    /** 活动类型：system-系统活动 / initiated-用户发起 */
    private String activityType;

    /** 邀请码（可选，用于邀请绑定） */
    private String inviteCode;

    // Getter and Setter

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServerSpec() {
        return serverSpec;
    }

    public void setServerSpec(String serverSpec) {
        this.serverSpec = serverSpec;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
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

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
