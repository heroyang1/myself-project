package com.backstage.system.controller.book;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class BookListReqVO {

    @ApiModelProperty(value = "当前页码")
    private Long pageNum;

    @ApiModelProperty(value = "每页数量")
    private Long pageSize;

    @ApiModelProperty(value = "电子书标题")
    private String title;

    @ApiModelProperty(value = "资源等级：1免费，2VIP，3小班，4内部成员，6创始人")
    private Integer level;

    @ApiModelProperty(value = "当前用户权限等级，后端自动设置", hidden = true)
    private Integer userLevel;

    @ApiModelProperty(value = "当前用户ID，后端自动设置", hidden = true)
    private Long userId;

    @ApiModelProperty(value = "是否包含逻辑删除电子书，仅 level>=4 生效")
    private Boolean includeDeleted;

    /** 标签名列表 */
    private List<String> tagNameList;

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getIncludeDeleted() {
        return includeDeleted;
    }

    public void setIncludeDeleted(Boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    public List<String> getTagNameList() {
        return tagNameList;
    }

    public void setTagNameList(List<String> tagNameList) {
        this.tagNameList = tagNameList;
    }
}
