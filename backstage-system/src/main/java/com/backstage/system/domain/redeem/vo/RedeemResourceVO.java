package com.backstage.system.domain.redeem.vo;

import java.math.BigDecimal;

public class RedeemResourceVO {
    private String resourceNo;
    private String resourceType;
    private Integer productType;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal originalPrice;

    public String getResourceNo() { return resourceNo; }
    public void setResourceNo(String resourceNo) { this.resourceNo = resourceNo; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Integer getProductType() { return productType; }
    public void setProductType(Integer productType) { this.productType = productType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
}
