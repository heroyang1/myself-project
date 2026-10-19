package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@ApiModel("Internal member employment profile save request")
public class InternalMemberEmploymentProfileSaveDTO {
    @ApiModelProperty("Job status: 0-unemployed 1-looking 2-employed")
    @NotNull(message = "求职状态不能为空")
    private Integer jobStatus;

    @ApiModelProperty("Job status display name")
    @Size(max = 64, message = "求职状态名称长度不能超过64")
    private String jobStatusName;

    @ApiModelProperty("Company name")
    @Size(max = 128, message = "公司名称长度不能超过128")
    private String companyName;

    @ApiModelProperty("Position name")
    @Size(max = 128, message = "岗位名称长度不能超过128")
    private String positionName;

    @ApiModelProperty("City")
    @Size(max = 64, message = "城市长度不能超过64")
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
    @Size(max = 1000, message = "备注长度不能超过1000")
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
    @Size(max = 64, message = "面试状态长度不能超过64")
    private String interviewStatus;

    @ApiModelProperty("Interview result")
    @Size(max = 64, message = "面试结果长度不能超过64")
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
