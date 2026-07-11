package com.backstage.system.domain.bundle.dto;

import java.math.BigDecimal;

public class BundleItemRequest {
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private BigDecimal unitPrice;
    private String itemRole;
    private Integer sortOrder;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getItemRole() { return itemRole; }
    public void setItemRole(String itemRole) { this.itemRole = itemRole; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
