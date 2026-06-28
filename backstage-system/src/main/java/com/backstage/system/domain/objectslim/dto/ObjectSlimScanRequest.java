package com.backstage.system.domain.objectslim.dto;

public class ObjectSlimScanRequest {
    private String bucket;
    private String prefix;
    private Integer limit = 200;

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
