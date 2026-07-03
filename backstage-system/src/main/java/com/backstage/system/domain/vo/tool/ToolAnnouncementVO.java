package com.backstage.system.domain.vo.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@ApiModel(description = "工具模块公告")
public class ToolAnnouncementVO {

    @ApiModelProperty(value = "公告ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "公告标题", example = "工具模块维护通知")
    private String title;

    @ApiModelProperty(value = "公告跳转链接", example = "https://example.com/tool/notice/1")
    private String link;

    @ApiModelProperty(value = "图标编码", example = "payment")
    private String iconCode;

    @ApiModelProperty(value = "公告栏目：1-系统公告，2-业务动态", example = "1")
    private Integer channel;

    @ApiModelProperty(value = "资源类型", example = "tool")
    private String resourceType;

    @ApiModelProperty(value = "创建时间", example = "2026-05-21 10:00:00")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
