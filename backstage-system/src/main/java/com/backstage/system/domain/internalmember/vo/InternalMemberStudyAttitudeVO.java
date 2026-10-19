package com.backstage.system.domain.internalmember.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InternalMemberStudyAttitudeVO {
    private String attitude;
    private Integer score;
    private BigDecimal overallProgress;
    private Integer startedCourseCount;
    private Integer completedCourseCount;
    private Integer unstartedCourseCount;
    private String lastLearnTime;
    private List<String> signals = new ArrayList<>();

    public String getAttitude() {
        return attitude;
    }

    public void setAttitude(String attitude) {
        this.attitude = attitude;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public BigDecimal getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(BigDecimal overallProgress) {
        this.overallProgress = overallProgress;
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

    public Integer getUnstartedCourseCount() {
        return unstartedCourseCount;
    }

    public void setUnstartedCourseCount(Integer unstartedCourseCount) {
        this.unstartedCourseCount = unstartedCourseCount;
    }

    public String getLastLearnTime() {
        return lastLearnTime;
    }

    public void setLastLearnTime(String lastLearnTime) {
        this.lastLearnTime = lastLearnTime;
    }

    public List<String> getSignals() {
        return signals;
    }

    public void setSignals(List<String> signals) {
        this.signals = signals;
    }
}
