-- ============================================================
-- 秒杀活动表
-- SysFlashSale 实体对应表，SysFlashsaleMapper.xml 查询引用
-- ============================================================

DROP TABLE IF EXISTS `osh_flashsale`;
CREATE TABLE `osh_flashsale` (
    `id`          bigint(20)    NOT NULL AUTO_INCREMENT               COMMENT '主键ID',
    `goods_id`    bigint(20)    DEFAULT NULL                          COMMENT '商品ID（关联课程/专栏等）',
    `flash_price` decimal(10,2) DEFAULT NULL                          COMMENT '秒杀价格',
    `s_num`       int(11)       DEFAULT NULL                          COMMENT '秒杀总数量',
    `used_num`    int(11)       NOT NULL DEFAULT 0                    COMMENT '已使用（已售出）数量',
    `start_time`  varchar(32)   DEFAULT NULL                          COMMENT '开始时间',
    `end_time`    varchar(32)   DEFAULT NULL                          COMMENT '结束时间',
    `flash_type`  varchar(32)   DEFAULT NULL                          COMMENT '秒杀类型',
    `create_by`   varchar(64)   DEFAULT ''                            COMMENT '创建者',
    `create_time` datetime      DEFAULT CURRENT_TIMESTAMP             COMMENT '创建时间',
    `update_by`   varchar(64)   DEFAULT ''                            COMMENT '更新者',
    `update_time` datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500)  DEFAULT NULL                          COMMENT '备注',
    `delete_flag` tinyint(1)    NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id`    (`goods_id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';
