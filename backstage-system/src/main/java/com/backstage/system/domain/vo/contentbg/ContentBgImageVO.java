package com.backstage.system.domain.vo.contentbg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("内容背景图选项")
public class ContentBgImageVO {

    @ApiModelProperty("背景图ID")
    private Long imageId;

    @ApiModelProperty("风格ID")
    private Long styleId;

    @ApiModelProperty("风格编码")
    private String styleCode;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("背景图OSS相对路径")
    private String imagePath;

    @ApiModelProperty("背景图临时访问地址")
    private String imageUrl;

    @ApiModelProperty("缩略图OSS相对路径")
    private String previewPath;

    @ApiModelProperty("缩略图临时访问地址")
    private String previewUrl;

    @ApiModelProperty("主色值")
    private String dominantColor;

    @ApiModelProperty("建议遮罩色")
    private String overlayColor;

    @ApiModelProperty("建议透明度")
    private BigDecimal opacity;

    @ApiModelProperty("是否默认")
    private Integer isDefault;

    @ApiModelProperty("是否已选中")
    private Boolean selected;

    @ApiModelProperty("排序")
    private Integer sort;

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getDominantColor() {
        return dominantColor;
    }

    public void setDominantColor(String dominantColor) {
        this.dominantColor = dominantColor;
    }

    public String getOverlayColor() {
        return overlayColor;
    }

    public void setOverlayColor(String overlayColor) {
        this.overlayColor = overlayColor;
    }

    public BigDecimal getOpacity() {
        return opacity;
    }

    public void setOpacity(BigDecimal opacity) {
        this.opacity = opacity;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
