package com.backstage.system.cart.domain;

/**
 * 购物车行实体（对应表 osh_cart）。
 * 课程演练契约 spec/cart-add.md · 第 2 步「执行」产物；本仓库无 cart 域，勿当生产事实引用。
 */
public class CartItem {

    private Long id;
    private Long userId;
    private Long skuId;
    private Integer quantity;

    public CartItem() {
    }

    public CartItem(Long userId, Long skuId, Integer quantity) {
        this.userId = userId;
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
