package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@ApiModel("Internal member follow task status update request")
public class InternalMemberFollowTaskStatusUpdateDTO {
    @ApiModelProperty("Task status")
    @NotBlank(message = "任务状态不能为空")
    @Size(max = 32, message = "任务状态长度不能超过32")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
