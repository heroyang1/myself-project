package com.backstage.system.domain.homepage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 首页热门课程悬浮层最新更新小节 VO
 *
 * @author jayTatum
 */
@ApiModel(description = "首页热门课程最新更新小节")
public class HotCourseLatestSectionVO {

    @ApiModelProperty("小节ID")
    private Long id;

    @ApiModelProperty("小节标题")
    private String title;

    @ApiModelProperty("所属章节标题")
    private String chapterTitle;

    @ApiModelProperty("更新时间")
    private Date updateTime;

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

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
