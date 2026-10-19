package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("内部成员课程进度明细查询参数")
public class InternalMemberCourseDetailQueryDTO {
    @ApiModelProperty("课程标题关键词")
    private String keyword;

    @ApiModelProperty("是否包含未开始课程，默认 false")
    private Boolean includeUnstarted;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getIncludeUnstarted() {
        return includeUnstarted;
    }

    public void setIncludeUnstarted(Boolean includeUnstarted) {
        this.includeUnstarted = includeUnstarted;
    }
}
