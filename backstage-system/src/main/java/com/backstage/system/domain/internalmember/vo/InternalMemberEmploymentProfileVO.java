package com.backstage.system.domain.internalmember.vo;

import java.math.BigDecimal;

public class InternalMemberEmploymentProfileVO {
    private Long userId;
    private Integer jobStatus;
    private String jobStatusName;
    private String companyName;
    private String positionName;
    private String city;
    private BigDecimal salaryBefore;
    private BigDecimal salaryAfter;
    private BigDecimal targetSalary;
    private BigDecimal salaryIncrease;
    private String entryTime;
    private Integer caseEligible;
    private String remark;
    private Integer resumeDeliveryCount;
    private Integer interviewCompanyCount;
    private Integer pendingInterviewCount;
    private Integer completedInterviewCount;
    private Integer interviewRoundCount;
    private String interviewStatus;
    private String interviewResult;
    private String lastInterviewTime;
    private Long updateBy;
    private String updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getJobStatusName() {
        return jobStatusName;
    }

    public void setJobStatusName(String jobStatusName) {
        this.jobStatusName = jobStatusName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getSalaryBefore() {
        return salaryBefore;
    }

    public void setSalaryBefore(BigDecimal salaryBefore) {
        this.salaryBefore = salaryBefore;
    }

    public BigDecimal getSalaryAfter() {
        return salaryAfter;
    }

    public void setSalaryAfter(BigDecimal salaryAfter) {
        this.salaryAfter = salaryAfter;
    }

    public BigDecimal getTargetSalary() {
        return targetSalary;
    }

    public void setTargetSalary(BigDecimal targetSalary) {
        this.targetSalary = targetSalary;
    }

    public BigDecimal getSalaryIncrease() {
        return salaryIncrease;
    }

    public void setSalaryIncrease(BigDecimal salaryIncrease) {
        this.salaryIncrease = salaryIncrease;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public Integer getCaseEligible() {
        return caseEligible;
    }

    public void setCaseEligible(Integer caseEligible) {
        this.caseEligible = caseEligible;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getResumeDeliveryCount() {
        return resumeDeliveryCount;
    }

    public void setResumeDeliveryCount(Integer resumeDeliveryCount) {
        this.resumeDeliveryCount = resumeDeliveryCount;
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

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
