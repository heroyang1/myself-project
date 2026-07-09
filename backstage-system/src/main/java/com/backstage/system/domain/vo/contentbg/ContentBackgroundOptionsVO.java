package com.backstage.system.domain.vo.contentbg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ApiModel("内容背景切换选项")
public class ContentBackgroundOptionsVO {

    @ApiModelProperty("当前风格ID")
    private Long currentStyleId;

    @ApiModelProperty("当前风格编码")
    private String currentStyleCode;

    @ApiModelProperty("当前风格名称")
    private String currentStyleName;

    @ApiModelProperty("当前背景图ID")
    private Long currentImageId;

    @ApiModelProperty("当前背景图URL")
    private String currentImageUrl;

    @ApiModelProperty("当前缩略图URL")
    private String currentPreviewUrl;

    @ApiModelProperty("当前建议遮罩色")
    private String currentOverlayColor;

    @ApiModelProperty("当前建议透明度")
    private BigDecimal currentOpacity;

    @ApiModelProperty("风格列表")
    private List<ContentBgStyleVO> styles = new ArrayList<>();

    public Long getCurrentStyleId() {
        return currentStyleId;
    }

    public void setCurrentStyleId(Long currentStyleId) {
        this.currentStyleId = currentStyleId;
    }

    public String getCurrentStyleCode() {
        return currentStyleCode;
    }

    public void setCurrentStyleCode(String currentStyleCode) {
        this.currentStyleCode = currentStyleCode;
    }

    public String getCurrentStyleName() {
        return currentStyleName;
    }

    public void setCurrentStyleName(String currentStyleName) {
        this.currentStyleName = currentStyleName;
    }

    public Long getCurrentImageId() {
        return currentImageId;
    }

    public void setCurrentImageId(Long currentImageId) {
        this.currentImageId = currentImageId;
    }

    public String getCurrentImageUrl() {
        return currentImageUrl;
    }

    public void setCurrentImageUrl(String currentImageUrl) {
        this.currentImageUrl = currentImageUrl;
    }

    public String getCurrentPreviewUrl() {
        return currentPreviewUrl;
    }

    public void setCurrentPreviewUrl(String currentPreviewUrl) {
        this.currentPreviewUrl = currentPreviewUrl;
    }

    public String getCurrentOverlayColor() {
        return currentOverlayColor;
    }

    public void setCurrentOverlayColor(String currentOverlayColor) {
        this.currentOverlayColor = currentOverlayColor;
    }

    public BigDecimal getCurrentOpacity() {
        return currentOpacity;
    }

    public void setCurrentOpacity(BigDecimal currentOpacity) {
        this.currentOpacity = currentOpacity;
    }

    public List<ContentBgStyleVO> getStyles() {
        return styles;
    }

    public void setStyles(List<ContentBgStyleVO> styles) {
        this.styles = styles;
    }
}
