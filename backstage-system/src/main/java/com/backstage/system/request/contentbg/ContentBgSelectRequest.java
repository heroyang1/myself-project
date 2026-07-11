package com.backstage.system.request.contentbg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("内容背景切换请求")
public class ContentBgSelectRequest {

    @ApiModelProperty("风格ID")
    private Long styleId;

    @ApiModelProperty("背景图ID")
    private Long imageId;

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

}
