package com.backstage.system.cart.port;

/**
 * 购物车数据访问端口。
 * 关键约束：累加必须是「原子 upsert」——一条 SQL 完成 INSERT ... ON DUPLICATE KEY UPDATE
 * quantity = LEAST(quantity + delta, cap)，靠 (user_id, sku_id) 唯一索引防双行（R1/E6）。
 * 严禁在 Service 层「先查后写」（A-01 教训）。
 */
public interface CartRepository {

    /**
     * 原子累加（或插入）某用户某 SKU 的数量，并施加单 SKU 上限 cap。
     *
     * @return 累加后的终态数量与是否触顶
     */
    Result addQuantityAtomically(Long userId, Long skuId, int delta, int cap);

    /** upsert 结果。 */
    class Result {
        private final int quantity;
        private final boolean capped;

        public Result(int quantity, boolean capped) {
            this.quantity = quantity;
            this.capped = capped;
        }

        public int getQuantity() {
            return quantity;
        }

        public boolean isCapped() {
            return capped;
        }
    }
}
