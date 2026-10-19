-- 购物车建表脚本（课程演练沙盒，置于 spec/cart-impl/ddl/ 而非受 R2 保护的 sql/ 目录）。
-- 生产接入须作为独立 openspec change 落 sql/ 并过 DDL↔代码一致性守卫。
-- 满足 spec R1/E6：uk_user_sku 唯一索引防双行；必备六字段（database.md 公约）。
CREATE TABLE `osh_cart` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT 'C 端用户 id（osh_user）',
  `sku_id`      BIGINT       NOT NULL COMMENT '商品 SKU id',
  `quantity`    INT          NOT NULL DEFAULT 1 COMMENT '加购数量（单 SKU 上限 99，应用+SQL 双重截断）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`   VARCHAR(64)           DEFAULT '' COMMENT '创建者',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by`   VARCHAR(64)           DEFAULT '' COMMENT '更新者',
  `delete_flag` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删（新行必须默认 0）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';
