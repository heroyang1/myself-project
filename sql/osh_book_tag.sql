-- ============================================================
-- 电子书标签表
-- 用于 OshHomePageBookMapper.selectTagsByBookIds 查询
-- ============================================================

DROP TABLE IF EXISTS `osh_book_tag`;
CREATE TABLE `osh_book_tag` (
    `id`          bigint(20)    NOT NULL AUTO_INCREMENT               COMMENT '主键ID',
    `book_id`     bigint(20)    NOT NULL                              COMMENT '电子书ID',
    `tag_name`    varchar(64)   NOT NULL DEFAULT ''                   COMMENT '标签名称',
    `sort_order`  int(11)       NOT NULL DEFAULT 0                    COMMENT '排序值',
    `delete_flag` tinyint(1)    NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0-正常 1-删除',
    `create_time` datetime      DEFAULT CURRENT_TIMESTAMP             COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_id`     (`book_id`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子书标签表';
