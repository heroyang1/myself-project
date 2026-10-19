package com.backstage.contract.internal.oss;

import java.io.Serializable;

/**
 * 内部对象存储签名响应
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class OssSignResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对象存储 key（回显，便于调用方关联）
     */
    private String objectKey;

    /**
     * 有时效的临时访问 URL
     */
    private String url;

    /**
     * 签名有效期（分钟）
     */
    private Integer expireMinutes;

    public OssSignResponse() {
    }

    public OssSignResponse(String objectKey, String url, Integer expireMinutes) {
        this.objectKey = objectKey;
        this.url = url;
        this.expireMinutes = expireMinutes;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(Integer expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}
