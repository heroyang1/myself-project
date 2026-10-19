package com.backstage.system.cart.vo;

/**
 * 加购结果视图（返回前端）。
 * capped=true 表示本次累加触达单 SKU 上限 99 被截断（R2）。
 */
public class CartItemVO {

    private Long skuId;
    private int quantity;
    private boolean capped;

    public CartItemVO(Long skuId, int quantity, boolean capped) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.capped = capped;
    }

    public Long getSkuId() {
        return skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isCapped() {
        return capped;
    }
}
