package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("成员跟进记录保存参数")
public class InternalMemberFollowRecordSaveDTO {
    @ApiModelProperty("跟进类型，如 电话、微信、面谈、复盘")
    private String followType;

    @ApiModelProperty("跟进内容")
    private String content;

    @ApiModelProperty("下次跟进时间，格式：yyyy-MM-dd HH:mm:ss")
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
