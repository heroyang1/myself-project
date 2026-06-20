package com.backstage.system.domain.internalmember.vo;

public class InternalMemberInterviewSummaryVO {
    private Long userId;
    private Integer interviewCompanyCount;
    private Integer pendingInterviewCount;
    private Integer completedInterviewCount;
    private Integer interviewRoundCount;
    private String interviewStatus;
    private String interviewResult;
    private String lastInterviewTime;
    private String updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getInterviewCompanyCount() {
        return interviewCompanyCount;
    }

    public void setInterviewCompanyCount(Integer interviewCompanyCount) {
        this.interviewCompanyCount = interviewCompanyCount;
    }

    public Integer getPendingInterviewCount() {
        return pendingInterviewCount;
    }

    public void setPendingInterviewCount(Integer pendingInterviewCount) {
        this.pendingInterviewCount = pendingInterviewCount;
    }

    public Integer getCompletedInterviewCount() {
        return completedInterviewCount;
    }

    public void setCompletedInterviewCount(Integer completedInterviewCount) {
        this.completedInterviewCount = completedInterviewCount;
    }

    public Integer getInterviewRoundCount() {
        return interviewRoundCount;
    }

    public void setInterviewRoundCount(Integer interviewRoundCount) {
        this.interviewRoundCount = interviewRoundCount;
    }

    public String getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public String getInterviewResult() {
        return interviewResult;
    }

    public void setInterviewResult(String interviewResult) {
        this.interviewResult = interviewResult;
    }

    public String getLastInterviewTime() {
        return lastInterviewTime;
    }

    public void setLastInterviewTime(String lastInterviewTime) {
        this.lastInterviewTime = lastInterviewTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
