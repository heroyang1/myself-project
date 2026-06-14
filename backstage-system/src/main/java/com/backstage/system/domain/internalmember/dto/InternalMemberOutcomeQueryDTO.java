package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("成员就业结果统计查询参数")
public class InternalMemberOutcomeQueryDTO {
    @ApiModelProperty("关键词，匹配用户名、邮箱、公司、岗位")
    private String keyword;

    @ApiModelProperty("就业状态：0未就业 1求职中 2已就业")
    private Integer jobStatus;

    @ApiModelProperty("最小涨薪金额，筛选涨薪超过多少")
    private BigDecimal minSalaryIncrease;

    @ApiModelProperty("是否适合做案例：0否 1是")
    private Integer caseEligible;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public BigDecimal getMinSalaryIncrease() {
        return minSalaryIncrease;
    }

    public void setMinSalaryIncrease(BigDecimal minSalaryIncrease) {
        this.minSalaryIncrease = minSalaryIncrease;
    }

    public Integer getCaseEligible() {
        return caseEligible;
    }

    public void setCaseEligible(Integer caseEligible) {
        this.caseEligible = caseEligible;
    }
}
