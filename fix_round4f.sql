USE `ry-vue`;

-- 1. osh_course_section: 补 parent_id / type / media_url
ALTER TABLE osh_course_section
    ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父章节ID' AFTER course_id,
    ADD COLUMN type VARCHAR(32) DEFAULT NULL COMMENT '章节类型（video/text/quiz）' AFTER sort_order,
    ADD COLUMN media_url VARCHAR(512) DEFAULT NULL COMMENT '媒体文件URL' AFTER type;

-- 2. osh_column_course: 补 delete_flag
ALTER TABLE osh_column_course
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）';

-- 3. osh_flashsale: 建表
CREATE TABLE IF NOT EXISTS `osh_flashsale` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `goods_id`    BIGINT         DEFAULT NULL COMMENT '商品ID',
    `flash_price` DECIMAL(10,2)  DEFAULT NULL COMMENT '秒杀价格',
    `s_num`       INT            DEFAULT NULL COMMENT '秒杀数量',
    `used_num`    INT            DEFAULT 0    COMMENT '已使用数量',
    `start_time`  VARCHAR(32)    DEFAULT NULL COMMENT '开始时间',
    `end_time`    VARCHAR(32)    DEFAULT NULL COMMENT '结束时间',
    `flash_type`  VARCHAR(32)    DEFAULT NULL COMMENT '秒杀类型',
    `create_by`   VARCHAR(64)    DEFAULT ''   COMMENT '创建者',
    `create_time` DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)    DEFAULT ''   COMMENT '更新者',
    `update_time` DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `delete_flag` TINYINT        NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

SELECT 'Round 4f applied OK' AS result;
