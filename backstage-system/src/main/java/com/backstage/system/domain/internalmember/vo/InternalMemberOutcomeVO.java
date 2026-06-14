package com.backstage.system.domain.internalmember.vo;

import java.math.BigDecimal;

public class InternalMemberOutcomeVO {
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String memberType;
    private String memberTypeName;
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
    private String lastFollowTime;
    private String nextFollowTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getMemberTypeName() {
        return memberTypeName;
    }

    public void setMemberTypeName(String memberTypeName) {
        this.memberTypeName = memberTypeName;
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

    public String getLastFollowTime() {
        return lastFollowTime;
    }

    public void setLastFollowTime(String lastFollowTime) {
        this.lastFollowTime = lastFollowTime;
    }

    public String getNextFollowTime() {
        return nextFollowTime;
    }

    public void setNextFollowTime(String nextFollowTime) {
        this.nextFollowTime = nextFollowTime;
    }
}
