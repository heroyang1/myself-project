package com.backstage.system.cart.port;

/**
 * 商品查询端口（跨域只读）。生产由 product 域的 Service 接口实现。
 */
public interface ProductQueryService {

    /** SKU 是否存在（E1）。 */
    boolean exists(Long skuId);

    /** SKU 是否在售（R4）。 */
    boolean isOnSale(Long skuId);
}
