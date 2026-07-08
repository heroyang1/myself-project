package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ApiModel("Internal member growth story visibility update request")
public class InternalMemberGrowthStoryVisibilityUpdateDTO {
    @ApiModelProperty("Whether the story is public: 0-no, 1-yes")
    @NotNull(message = "isPublic cannot be empty")
    @Min(value = 0, message = "isPublic must be 0 or 1")
    @Max(value = 1, message = "isPublic must be 0 or 1")
    private Integer isPublic;

    public Integer getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Integer isPublic) {
        this.isPublic = isPublic;
    }
}
