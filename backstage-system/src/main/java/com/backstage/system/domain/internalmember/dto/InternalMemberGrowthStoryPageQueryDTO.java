package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("Internal member growth story page query")
public class InternalMemberGrowthStoryPageQueryDTO {
    @ApiModelProperty("Optional keyword for username/title/company/position search")
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
