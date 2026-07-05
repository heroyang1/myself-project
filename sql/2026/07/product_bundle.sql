-- Product bundle (套餐): combine multiple sellable resources into one offer.

CREATE TABLE IF NOT EXISTS `osh_product_bundle` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bundle_no` varchar(32) NOT NULL COMMENT '套餐编号',
    `name` varchar(128) NOT NULL COMMENT '套餐名称',
    `cover_url` varchar(512) DEFAULT NULL COMMENT '封面图',
    `description` varchar(2000) DEFAULT NULL COMMENT '描述',
    `sale_mode` varchar(32) NOT NULL COMMENT 'PRIMARY_GIFT=买主送副, FLAT_BUNDLE=打包优惠',
    `bundle_price` decimal(10, 2) NOT NULL DEFAULT '0.00' COMMENT '套餐售价',
    `original_price` decimal(10, 2) DEFAULT NULL COMMENT '单买参考总价',
    `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0草稿 1上架 2下架',
    `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
    `sales_count` int(11) NOT NULL DEFAULT '0' COMMENT '销量',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `create_by` bigint(20) DEFAULT NULL,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `update_by` bigint(20) DEFAULT NULL,
    `delete_flag` tinyint(4) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bundle_no` (`bundle_no`),
    KEY `idx_bundle_status` (`status`, `delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品套餐';

CREATE TABLE IF NOT EXISTS `osh_bundle_item` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bundle_id` bigint(20) NOT NULL COMMENT '套餐ID',
    `resource_type` varchar(32) NOT NULL COMMENT '资源类型 course/book/tool/exam...',
    `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
    `resource_name` varchar(256) DEFAULT NULL COMMENT '资源名称快照',
    `unit_price` decimal(10, 2) DEFAULT NULL COMMENT '单买参考价',
    `item_role` varchar(16) NOT NULL DEFAULT 'member' COMMENT 'primary/secondary/member',
    `is_gift` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否赠品 0否 1是',
    `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `create_by` bigint(20) DEFAULT NULL,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `update_by` bigint(20) DEFAULT NULL,
    `delete_flag` tinyint(4) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_bundle_item_bundle` (`bundle_id`, `delete_flag`),
    KEY `idx_bundle_item_resource` (`resource_type`, `resource_id`, `delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐商品明细';
