package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("内部成员学习总览查询参数")
public class InternalMemberPageQueryDTO {
    @ApiModelProperty("关键词，匹配用户名或邮箱")
    private String keyword;

    @ApiModelProperty("会员类型，如 vip / small_class")
    private String memberType;

    @ApiModelProperty("是否仅查询当前有效会员，true=是，false=否")
    private Boolean activeOnly;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public Boolean getActiveOnly() {
        return activeOnly;
    }

    public void setActiveOnly(Boolean activeOnly) {
        this.activeOnly = activeOnly;
    }
}
