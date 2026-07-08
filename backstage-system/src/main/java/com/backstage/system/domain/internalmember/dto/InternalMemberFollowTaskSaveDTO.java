package com.backstage.system.domain.internalmember.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@ApiModel("Internal member follow task save request")
public class InternalMemberFollowTaskSaveDTO {
    @ApiModelProperty("Follow content")
    @NotBlank(message = "跟进任务内容不能为空")
    @Size(max = 2000, message = "跟进任务内容长度不能超过2000")
    private String content;

    @ApiModelProperty("Responsible admin user id")
    @NotNull(message = "负责人不能为空")
    private Long responsibleUserId;

    @ApiModelProperty("Planned follow time, format: yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "计划跟进时间不能为空")
    private String plannedFollowTime;

    @ApiModelProperty("Reminder time, format: yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "提醒时间不能为空")
    private String remindTime;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(Long responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

    public String getPlannedFollowTime() {
        return plannedFollowTime;
    }

    public void setPlannedFollowTime(String plannedFollowTime) {
        this.plannedFollowTime = plannedFollowTime;
    }

    public String getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime = remindTime;
    }
}
