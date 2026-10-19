package com.backstage.system.domain.bundle.dto;

import java.math.BigDecimal;
import java.util.List;

public class BundleSaveRequest {
    private Long id;
    private String name;
    private String coverUrl;
    private String description;
    private String saleMode;
    private BigDecimal bundlePrice;
    private BigDecimal originalPrice;
    private Integer sortOrder;
    private List<BundleItemRequest> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public List<BundleItemRequest> getItems() { return items; }
    public void setItems(List<BundleItemRequest> items) { this.items = items; }
}
