package com.backstage.system.domain.vo.contentbg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel("内容背景图上传返回")
public class ContentBgImageUploadVO {

    @ApiModelProperty("背景图ID")
    private Long imageId;

    @ApiModelProperty("风格ID")
    private Long styleId;

    @ApiModelProperty("风格编码")
    private String styleCode;

    @ApiModelProperty("背景图OSS相对路径")
    private String imagePath;

    @ApiModelProperty("背景图临时访问地址")
    private String imageUrl;

    @ApiModelProperty("缩略图OSS相对路径")
    private String previewPath;

    @ApiModelProperty("缩略图临时访问地址")
    private String previewUrl;

    @ApiModelProperty("建议遮罩色")
    private String overlayColor;

    @ApiModelProperty("建议透明度")
    private BigDecimal opacity;

    @ApiModelProperty("文件名称")
    private String fileName;

    @ApiModelProperty("文件大小")
    private Long fileSize;

    @ApiModelProperty("文件类型")
    private String fileType;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
