package com.backstage.system.domain.vo.contentbg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel("内容背景风格选项")
public class ContentBgStyleVO {

    @ApiModelProperty("风格ID")
    private Long styleId;

    @ApiModelProperty("风格编码")
    private String styleCode;

    @ApiModelProperty("风格名称")
    private String name;

    @ApiModelProperty("风格描述")
    private String description;

    @ApiModelProperty("封面图URL")
    private String coverImageUrl;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("图片列表")
    private List<ContentBgImageVO> images = new ArrayList<>();

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<ContentBgImageVO> getImages() {
        return images;
    }

    public void setImages(List<ContentBgImageVO> images) {
        this.images = images;
    }
}
