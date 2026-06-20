package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("Internal member employment profile save request")
public class InternalMemberEmploymentProfileSaveDTO {
    @ApiModelProperty("Job status: 0-unemployed 1-looking 2-employed")
    private Integer jobStatus;

    @ApiModelProperty("Job status display name")
    private String jobStatusName;

    @ApiModelProperty("Company name")
    private String companyName;

    @ApiModelProperty("Position name")
    private String positionName;

    @ApiModelProperty("City")
    private String city;

    @ApiModelProperty("Salary before study")
    private BigDecimal salaryBefore;

    @ApiModelProperty("Current salary")
    private BigDecimal salaryAfter;

    @ApiModelProperty("Target salary")
    private BigDecimal targetSalary;

    @ApiModelProperty("Entry time, format: yyyy-MM-dd HH:mm:ss")
    private String entryTime;

    @ApiModelProperty("Case eligible: 0-no 1-yes")
    private Integer caseEligible;

    @ApiModelProperty("Remark")
    private String remark;

    @ApiModelProperty("Resume delivery count")
    private Integer resumeDeliveryCount;

    @ApiModelProperty("Interviewed company count")
    private Integer interviewCompanyCount;

    @ApiModelProperty("Pending interview company count")
    private Integer pendingInterviewCount;

    @ApiModelProperty("Completed interview company count")
    private Integer completedInterviewCount;

    @ApiModelProperty("Interview round count")
    private Integer interviewRoundCount;

    @ApiModelProperty("Interview status")
    private String interviewStatus;

    @ApiModelProperty("Interview result")
    private String interviewResult;

    @ApiModelProperty("Last interview time, format: yyyy-MM-dd HH:mm:ss")
    private String lastInterviewTime;

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
}
