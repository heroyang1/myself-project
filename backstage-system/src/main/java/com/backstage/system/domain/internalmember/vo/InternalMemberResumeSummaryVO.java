package com.backstage.system.domain.internalmember.vo;

public class InternalMemberResumeSummaryVO {
    private Long userId;
    private Integer resumeDeliveryCount;
    private String updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getResumeDeliveryCount() {
        return resumeDeliveryCount;
    }

    public void setResumeDeliveryCount(Integer resumeDeliveryCount) {
        this.resumeDeliveryCount = resumeDeliveryCount;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
