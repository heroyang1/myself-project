package com.backstage.system.cart.port;

/**
 * 库存查询端口（跨域只读）。生产由 stock 域的 Service 接口实现（R4：跨域只走对方 Service）。
 */
public interface StockQueryService {

    /**
     * 返回 SKU 可售库存。
     * 下游超时/宕机时抛 {@link DownstreamUnavailableException}（E4 与 E3 的分界）。
     */
    int getAvailable(Long skuId);
}
