package com.backstage.system.domain.internalmember.vo;

import java.math.BigDecimal;

public class InternalMemberSalarySummaryVO {
    private Long userId;
    private Integer jobStatus;
    private String jobStatusName;
    private String companyName;
    private String positionName;
    private BigDecimal salaryBefore;
    private BigDecimal salaryAfter;
    private BigDecimal targetSalary;
    private BigDecimal salaryIncrease;
    private BigDecimal salaryIncreaseRate;
    private BigDecimal gapToTargetSalary;
    private String entryTime;
    private Integer caseEligible;
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

    public BigDecimal getSalaryIncreaseRate() {
        return salaryIncreaseRate;
    }

    public void setSalaryIncreaseRate(BigDecimal salaryIncreaseRate) {
        this.salaryIncreaseRate = salaryIncreaseRate;
    }

    public BigDecimal getGapToTargetSalary() {
        return gapToTargetSalary;
    }

    public void setGapToTargetSalary(BigDecimal gapToTargetSalary) {
        this.gapToTargetSalary = gapToTargetSalary;
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

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
