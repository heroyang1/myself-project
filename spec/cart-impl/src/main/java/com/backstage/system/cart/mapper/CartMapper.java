package com.backstage.system.cart.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 购物车 Mapper（生产适配层，不在纯 JDK 单测编译范围内）。
 * 累加走单条 upsert，靠唯一索引 uk_user_sku 防双行；LEAST 施加单 SKU 上限（R2/E6）。
 * Service 层的 CartRepository 端口由一个薄适配器转调本 Mapper（示意，未单列文件）。
 */
public interface CartMapper {

    /**
     * INSERT INTO osh_cart(user_id, sku_id, quantity, ...) VALUES (...)
     * ON DUPLICATE KEY UPDATE quantity = LEAST(quantity + #{delta}, #{cap})
     * 见 resources/mapper/cart/CartMapper.xml。
     *
     * @return 影响行数（1=插入，2=更新，MySQL upsert 语义）
     */
    int upsertAdd(@Param("userId") Long userId,
                  @Param("skuId") Long skuId,
                  @Param("delta") int delta,
                  @Param("cap") int cap);

    /** 读取累加后的终态数量（upsert 后回查，用于构造 capped 判定）。 */
    Integer selectQuantity(@Param("userId") Long userId, @Param("skuId") Long skuId);
}
