package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Size;

@ApiModel("Internal member follow task complete request")
public class InternalMemberFollowTaskCompleteDTO {
    @ApiModelProperty("Completion result note")
    @Size(max = 2000, message = "完成备注长度不能超过2000")
    private String resultNote;

    public String getResultNote() {
        return resultNote;
    }

    public void setResultNote(String resultNote) {
        this.resultNote = resultNote;
    }
}
