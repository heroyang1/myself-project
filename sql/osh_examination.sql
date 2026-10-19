-- ============================================================
-- 考试表
-- 用于课程服务人员申请审核、用户在线考试等场景
-- ============================================================

DROP TABLE IF EXISTS `osh_examination`;
CREATE TABLE `osh_examination` (
    `id`              bigint(20)    NOT NULL AUTO_INCREMENT               COMMENT '考试ID',
    `title`           varchar(255)  NOT NULL DEFAULT ''                   COMMENT '考试标题',
    `cover`           varchar(512)  DEFAULT ''                            COMMENT '封面图片URL',
    `description`     text          DEFAULT NULL                          COMMENT '考试描述',
    `total_score`     int(11)       NOT NULL DEFAULT 0                    COMMENT '总分',
    `pass_score`      int(11)       NOT NULL DEFAULT 0                    COMMENT '及格分数',
    `expire`          int(11)       NOT NULL DEFAULT 0                    COMMENT '答题时限（分钟）',
    `question_count`  int(11)       NOT NULL DEFAULT 0                    COMMENT '题目数量',
    `collect_count`   int(11)       NOT NULL DEFAULT 0                    COMMENT '收藏数量',
    `status`          tinyint(3)    NOT NULL DEFAULT 1                    COMMENT '状态：0-草稿 1-已发布',
    `delete_flag`     tinyint(1)    NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`       varchar(64)   DEFAULT ''                            COMMENT '创建者',
    `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP             COMMENT '创建时间',
    `update_by`       varchar(64)   DEFAULT ''                            COMMENT '更新者',
    `update_time`     datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status`      (`status`),
    KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试表';
