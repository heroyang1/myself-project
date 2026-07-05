package com.backstage.system.domain.bundle.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BundleDetailVO {
    private Long id;
    private String bundleNo;
    private String name;
    private String coverUrl;
    private String description;
    private String saleMode;
    private BigDecimal bundlePrice;
    private BigDecimal originalPrice;
    private Integer status;
    private Integer sortOrder;
    private Integer salesCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<BundleItemVO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSaleMode() { return saleMode; }
    public void setSaleMode(String saleMode) { this.saleMode = saleMode; }
    public BigDecimal getBundlePrice() { return bundlePrice; }
    public void setBundlePrice(BigDecimal bundlePrice) { this.bundlePrice = bundlePrice; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<BundleItemVO> getItems() { return items; }
    public void setItems(List<BundleItemVO> items) { this.items = items; }
}
