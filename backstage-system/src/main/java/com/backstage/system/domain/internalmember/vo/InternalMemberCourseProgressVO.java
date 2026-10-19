package com.backstage.system.domain.internalmember.vo;

import java.math.BigDecimal;

public class InternalMemberCourseProgressVO {
    private Long courseId;
    private String courseTitle;
    private String courseCover;
    private Integer purchasedFlag;
    private Integer startedFlag;
    private Integer learnedSectionCount;
    private Integer totalSectionCount;
    private BigDecimal progressPercent;
    private Integer completedFlag;
    private Integer progressStatus;
    private String progressStatusName;
    private String purchaseTime;
    private String startLearnTime;
    private String lastLearnTime;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseCover() {
        return courseCover;
    }

    public void setCourseCover(String courseCover) {
        this.courseCover = courseCover;
    }

    public Integer getPurchasedFlag() {
        return purchasedFlag;
    }

    public void setPurchasedFlag(Integer purchasedFlag) {
        this.purchasedFlag = purchasedFlag;
    }

    public Integer getStartedFlag() {
        return startedFlag;
    }

    public void setStartedFlag(Integer startedFlag) {
        this.startedFlag = startedFlag;
    }

    public Integer getLearnedSectionCount() {
        return learnedSectionCount;
    }

    public void setLearnedSectionCount(Integer learnedSectionCount) {
        this.learnedSectionCount = learnedSectionCount;
    }

    public Integer getTotalSectionCount() {
        return totalSectionCount;
    }

    public void setTotalSectionCount(Integer totalSectionCount) {
        this.totalSectionCount = totalSectionCount;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getCompletedFlag() {
        return completedFlag;
    }

    public void setCompletedFlag(Integer completedFlag) {
        this.completedFlag = completedFlag;
    }

    public Integer getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(Integer progressStatus) {
        this.progressStatus = progressStatus;
    }

    public String getProgressStatusName() {
        return progressStatusName;
    }

    public void setProgressStatusName(String progressStatusName) {
        this.progressStatusName = progressStatusName;
    }

    public String getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(String purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public String getStartLearnTime() {
        return startLearnTime;
    }

    public void setStartLearnTime(String startLearnTime) {
        this.startLearnTime = startLearnTime;
    }

    public String getLastLearnTime() {
        return lastLearnTime;
    }

    public void setLastLearnTime(String lastLearnTime) {
        this.lastLearnTime = lastLearnTime;
    }
}
