package com.backstage.system.domain.internalmember.vo;

import java.util.ArrayList;
import java.util.List;

public class InternalMemberExamSummaryVO {
    private Integer reportedCount;
    private Integer passedCount;
    private Integer failedCount;
    private Integer pendingReadCount;
    private Integer inProgressCount;
    private String lastExamTime;
    private List<InternalMemberExamItemVO> exams = new ArrayList<>();

    public Integer getReportedCount() {
        return reportedCount;
    }

    public void setReportedCount(Integer reportedCount) {
        this.reportedCount = reportedCount;
    }

    public Integer getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getPendingReadCount() {
        return pendingReadCount;
    }

    public void setPendingReadCount(Integer pendingReadCount) {
        this.pendingReadCount = pendingReadCount;
    }

    public Integer getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(Integer inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public String getLastExamTime() {
        return lastExamTime;
    }

    public void setLastExamTime(String lastExamTime) {
        this.lastExamTime = lastExamTime;
    }

    public List<InternalMemberExamItemVO> getExams() {
        return exams;
    }

    public void setExams(List<InternalMemberExamItemVO> exams) {
        this.exams = exams;
    }
}
