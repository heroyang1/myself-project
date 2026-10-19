USE `ry-vue`;

-- 1. osh_course: 补 sales_count (Mapper 引用 GREATEST(sales_count, buy_count))
ALTER TABLE osh_course
    ADD COLUMN sales_count INT NOT NULL DEFAULT 0 COMMENT '销售数量' AFTER buy_count;

-- 2. osh_group_activity: 补 cover + delete_flag
ALTER TABLE osh_group_activity
    ADD COLUMN cover VARCHAR(512) DEFAULT NULL COMMENT '封面图片URL' AFTER sort_order,
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）' AFTER update_time;

-- 3. osh_book_tag: 建表 (OshHomePageBookMapper.selectTagsByBookIds 引用)
CREATE TABLE IF NOT EXISTS `osh_book_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `book_id`     BIGINT       NOT NULL COMMENT '电子书ID',
    `tag_name`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '标签名称',
    `sort_order`  INT          NOT NULL DEFAULT 0  COMMENT '排序值',
    `delete_flag` TINYINT      NOT NULL DEFAULT 0  COMMENT '删除标志（0-未删 1-已删）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子书标签表';

SELECT 'Round 4d applied OK' AS result;
