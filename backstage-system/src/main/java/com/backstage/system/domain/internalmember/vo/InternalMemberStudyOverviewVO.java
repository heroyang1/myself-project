package com.backstage.system.domain.internalmember.vo;

import java.math.BigDecimal;

public class InternalMemberStudyOverviewVO {
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String memberType;
    private String memberTypeName;
    private String planName;
    private Integer memberActive;
    private String memberExpireTime;
    private Integer totalCourseCount;
    private Integer startedCourseCount;
    private Integer completedCourseCount;
    private BigDecimal overallProgress;
    private BigDecimal startRate;
    private BigDecimal completionRate;
    private String lastLearnTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getMemberTypeName() {
        return memberTypeName;
    }

    public void setMemberTypeName(String memberTypeName) {
        this.memberTypeName = memberTypeName;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Integer getMemberActive() {
        return memberActive;
    }

    public void setMemberActive(Integer memberActive) {
        this.memberActive = memberActive;
    }

    public String getMemberExpireTime() {
        return memberExpireTime;
    }

    public void setMemberExpireTime(String memberExpireTime) {
        this.memberExpireTime = memberExpireTime;
    }

    public Integer getTotalCourseCount() {
        return totalCourseCount;
    }

    public void setTotalCourseCount(Integer totalCourseCount) {
        this.totalCourseCount = totalCourseCount;
    }

    public Integer getStartedCourseCount() {
        return startedCourseCount;
    }

    public void setStartedCourseCount(Integer startedCourseCount) {
        this.startedCourseCount = startedCourseCount;
    }

    public Integer getCompletedCourseCount() {
        return completedCourseCount;
    }

    public void setCompletedCourseCount(Integer completedCourseCount) {
        this.completedCourseCount = completedCourseCount;
    }

    public BigDecimal getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(BigDecimal overallProgress) {
        this.overallProgress = overallProgress;
    }

    public BigDecimal getStartRate() {
        return startRate;
    }

    public void setStartRate(BigDecimal startRate) {
        this.startRate = startRate;
    }

    public BigDecimal getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(BigDecimal completionRate) {
        this.completionRate = completionRate;
    }

    public String getLastLearnTime() {
        return lastLearnTime;
    }

    public void setLastLearnTime(String lastLearnTime) {
        this.lastLearnTime = lastLearnTime;
    }
}
