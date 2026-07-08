package com.backstage.system.domain.bundle.vo;

import java.math.BigDecimal;

public class BundleItemVO {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private BigDecimal unitPrice;
    private String itemRole;
    private Integer isGift;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Integer getIsGift() { return isGift; }
    public void setIsGift(Integer isGift) { this.isGift = isGift; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
