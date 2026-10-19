package com.backstage.system.domain.bundle.dto;

public class BundlePageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String saleMode;
    private Integer status;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSaleMode() { return saleMode; }
    public void setSaleMode(String saleMode) { this.saleMode = saleMode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
