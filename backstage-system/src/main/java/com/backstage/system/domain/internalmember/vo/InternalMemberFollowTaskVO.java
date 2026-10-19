package com.backstage.system.domain.internalmember.vo;

public class InternalMemberFollowTaskVO {
    private Long id;
    private Long userId;
    private String content;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String plannedFollowTime;
    private String remindTime;
    private String status;
    private String statusName;
    private String notifyChannel;
    private Integer notifySent;
    private String notifySentTime;
    private String completedTime;
    private Long completedBy;
    private String completedByName;
    private String resultNote;
    private String createTime;
    private String updateTime;

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

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getNotifyChannel() {
        return notifyChannel;
    }

    public void setNotifyChannel(String notifyChannel) {
        this.notifyChannel = notifyChannel;
    }

    public Integer getNotifySent() {
        return notifySent;
    }

    public void setNotifySent(Integer notifySent) {
        this.notifySent = notifySent;
    }

    public String getNotifySentTime() {
        return notifySentTime;
    }

    public void setNotifySentTime(String notifySentTime) {
        this.notifySentTime = notifySentTime;
    }

    public String getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(String completedTime) {
        this.completedTime = completedTime;
    }

    public Long getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Long completedBy) {
        this.completedBy = completedBy;
    }

    public String getCompletedByName() {
        return completedByName;
    }

    public void setCompletedByName(String completedByName) {
        this.completedByName = completedByName;
    }

    public String getResultNote() {
        return resultNote;
    }

    public void setResultNote(String resultNote) {
        this.resultNote = resultNote;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
