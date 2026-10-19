package com.backstage.system.cart.port;

/**
 * 下游（库存/商品域）不可用信号——超时、宕机等。
 * 用于区分 E3「库存不足」（确定拒绝）与 E4「库存查询失败」（未知，放行加购）。
 */
public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException(String message) {
        super(message);
    }
}
