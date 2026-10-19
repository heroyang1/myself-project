package com.backstage.contract.internal.oss;

import java.io.Serializable;

/**
 * 内部对象存储签名请求
 *
 * <p>映射到单体 {@code OssService.getLimitedUrl(path, minute)}。数据库只存 objectKey，
 * 由本端点按需生成有时效的临时访问 URL，避免存储过期 URL 导致裂图。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class OssSignRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对象存储 key（数据库中存储的相对路径，非空）
     */
    private String objectKey;

    /**
     * 签名有效期（分钟，非空且大于 0）
     */
    private Integer expireMinutes;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Integer getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(Integer expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}
