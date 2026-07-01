package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@ApiModel("Internal member follow record save request")
public class InternalMemberFollowRecordSaveDTO {
    @ApiModelProperty("Follow type, e.g. phone/wechat/interview/review")
    @NotBlank(message = "跟进类型不能为空")
    @Size(max = 64, message = "跟进类型长度不能超过64")
    private String followType;

    @ApiModelProperty("Follow content")
    @NotBlank(message = "跟进内容不能为空")
    @Size(max = 2000, message = "跟进内容长度不能超过2000")
    private String content;

    @ApiModelProperty("Next follow time, format: yyyy-MM-dd HH:mm:ss")
    private String nextFollowTime;

    public String getFollowType() {
        return followType;
    }

    public void setFollowType(String followType) {
        this.followType = followType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNextFollowTime() {
        return nextFollowTime;
    }

    public void setNextFollowTime(String nextFollowTime) {
        this.nextFollowTime = nextFollowTime;
    }
}
