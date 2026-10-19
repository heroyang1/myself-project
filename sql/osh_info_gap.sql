-- ============================================================
-- 信息差主表
-- 用户发布的信息差内容，支持好评/中评/差评评分体系
-- ============================================================

DROP TABLE IF EXISTS `osh_info_gap`;
CREATE TABLE `osh_info_gap` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT             COMMENT '信息差ID',
    `no`            varchar(64)   NOT NULL DEFAULT ''                 COMMENT '编号',
    `user_id`       bigint(20)    NOT NULL                            COMMENT '用户ID',
    `user_name`     varchar(64)   DEFAULT NULL                        COMMENT '用户名',
    `title`         varchar(255)  NOT NULL DEFAULT ''                 COMMENT '标题',
    `tag`           varchar(255)  DEFAULT NULL                        COMMENT '标签（逗号分隔）',
    `content`       text          DEFAULT NULL                        COMMENT '内容',
    `good_count`    int(11)       NOT NULL DEFAULT 0                  COMMENT '好评数',
    `middle_count`  int(11)       NOT NULL DEFAULT 0                  COMMENT '中评数',
    `bad_count`     int(11)       NOT NULL DEFAULT 0                  COMMENT '差评数',
    `collect_count` int(11)       NOT NULL DEFAULT 0                  COMMENT '收藏数',
    `view_count`    int(11)       NOT NULL DEFAULT 0                  COMMENT '浏览数',
    `status`        tinyint(3)    NOT NULL DEFAULT 0                  COMMENT '状态（0-草稿 2-待审核 4-已发布 6-已下架）',
    `update_by`     bigint(20)    DEFAULT NULL                        COMMENT '更新者',
    `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_time`   datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`   tinyint(1)    NOT NULL DEFAULT 0                  COMMENT '逻辑删除：0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id`     (`user_id`),
    KEY `idx_status`      (`status`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信息差主表';
