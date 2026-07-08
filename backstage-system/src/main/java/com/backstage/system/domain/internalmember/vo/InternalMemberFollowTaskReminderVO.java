package com.backstage.system.domain.internalmember.vo;

public class InternalMemberFollowTaskReminderVO {
    private Long id;
    private Long userId;
    private String content;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String responsibleUserEmail;
    private String plannedFollowTime;
    private String remindTime;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public String getResponsibleUserName() {
        return responsibleUserName;
    }

    public void setResponsibleUserName(String responsibleUserName) {
        this.responsibleUserName = responsibleUserName;
    }

    public String getResponsibleUserEmail() {
        return responsibleUserEmail;
    }

    public void setResponsibleUserEmail(String responsibleUserEmail) {
        this.responsibleUserEmail = responsibleUserEmail;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
