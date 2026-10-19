package com.backstage.system.cart.dto;

/**
 * 加购请求 DTO。
 * 生产接入时字段校验由 Controller 上的 @Validated + 注解（@NotNull/@Min）承担（E2·A-11 教训）；
 * 本沙盒为保持可独立编译，仅用 JDK 类型，Service 层再做一次防御性校验。
 */
public class CartAddDTO {

    private Long skuId;
    private Integer quantity;
    /** 幂等 token（客户端生成）；缺失即拒绝（E8）。 */
    private String idempotencyToken;

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

    public String getIdempotencyToken() {
        return idempotencyToken;
    }

    public void setIdempotencyToken(String idempotencyToken) {
        this.idempotencyToken = idempotencyToken;
    }
}
