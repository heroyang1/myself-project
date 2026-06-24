package com.backstage.system.domain.announcement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 公告跑马灯条目通用 VO。
 * <p>
 * 对应统一公告表 osh_announcement 的展示型投影，不绑定具体业务模块，
 * 供任何"跑马灯式公告"场景复用。
 *
 * @author backstage
 */
@ApiModel(description = "公告跑马灯条目")
public class AnnouncementMarqueeVO {

    @ApiModelProperty("公告 ID")
    private Long id;

    @ApiModelProperty("公告标题（跑马灯展示文案）")
    private String title;

    @ApiModelProperty("文案前缀 emoji 图标")
    private String icon;

    @ApiModelProperty("点击跳转链接")
    private String link;

    @ApiModelProperty("圆点 / 文字色调 hex")
    private String color;

    @ApiModelProperty("栏目：1-公告 2-动态")
    private Integer channel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;

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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
