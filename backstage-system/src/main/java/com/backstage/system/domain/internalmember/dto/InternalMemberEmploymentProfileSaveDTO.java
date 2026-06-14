package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("成员就业档案保存参数")
public class InternalMemberEmploymentProfileSaveDTO {
    @ApiModelProperty("就业状态：0未就业 1求职中 2已就业")
    private Integer jobStatus;

    @ApiModelProperty("就业状态名称，如 未就业、求职中、已就业")
    private String jobStatusName;

    @ApiModelProperty("公司名称")
    private String companyName;

    @ApiModelProperty("岗位名称")
    private String positionName;

    @ApiModelProperty("所在城市")
    private String city;

    @ApiModelProperty("学习前薪资")
    private BigDecimal salaryBefore;

    @ApiModelProperty("当前薪资")
    private BigDecimal salaryAfter;

    @ApiModelProperty("目标薪资")
    private BigDecimal targetSalary;

    @ApiModelProperty("入职时间，格式：yyyy-MM-dd HH:mm:ss")
    private String entryTime;

    @ApiModelProperty("是否适合做案例：0否 1是")
    private Integer caseEligible;

    @ApiModelProperty("备注")
    private String remark;

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
}
