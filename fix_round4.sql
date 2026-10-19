-- ============================================================
-- Round 4 Schema Fix (P3 FAIL items)
-- 修复第三轮验证测试中剩余的 7 个 DB schema 级 FAIL
-- ============================================================
USE `ry-vue`;

-- --------------------------------------------------------
-- 1. osh_book_chapter: 补 is_free + delete_flag
--    Mapper SQL 使用 is_free（带下划线），但表中列名为 isfree
--    GlobalLogicDeleteInterceptor 要求 delete_flag
-- --------------------------------------------------------
ALTER TABLE osh_book_chapter
    ADD COLUMN is_free TINYINT NOT NULL DEFAULT 0 COMMENT '是否免费章节（0-否 1-是）' AFTER orderby,
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）' AFTER remark;

-- --------------------------------------------------------
-- 2. osh_announcement: 补 start_time / end_time
--    OshHomePageAnnouncementMapper.xml 查询这两个列做时间过滤
-- --------------------------------------------------------
ALTER TABLE osh_announcement
    ADD COLUMN start_time DATETIME DEFAULT NULL COMMENT '公告生效起始时间' AFTER sort,
    ADD COLUMN end_time   DATETIME DEFAULT NULL COMMENT '公告生效截止时间' AFTER start_time;

-- --------------------------------------------------------
-- 3. assistant_feedback_tag: 建表
--    AssistantFeedbackTag 实体字段: id, code, name, sortOrder, useCount, isEnabled, remark
--    继承 OSHBaseEntity: create_time, create_by, update_time, update_by, delete_flag
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `assistant_feedback_tag` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `code`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '标签编码',
    `name`        VARCHAR(128) NOT NULL DEFAULT '' COMMENT '标签名称',
    `sort_order`  INT          NOT NULL DEFAULT 0  COMMENT '排序值',
    `use_count`   INT          NOT NULL DEFAULT 0  COMMENT '使用次数',
    `is_enabled`  TINYINT      NOT NULL DEFAULT 1  COMMENT '是否启用（0-否 1-是）',
    `remark`      VARCHAR(500) DEFAULT NULL         COMMENT '标签备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   BIGINT       DEFAULT NULL         COMMENT '创建者',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`   BIGINT       DEFAULT NULL         COMMENT '更新者',
    `delete_flag` TINYINT      NOT NULL DEFAULT 0   COMMENT '删除标志（0-未删 1-已删）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈标签表';

-- --------------------------------------------------------
-- 4. assistant_feedback_category: 建表
--    AssistantFeedbackCategory 实体字段
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `assistant_feedback_category` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `code`            VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '分类代码',
    `name`            VARCHAR(128) NOT NULL DEFAULT '' COMMENT '分类名称',
    `description`     VARCHAR(500) DEFAULT NULL         COMMENT '分类描述',
    `drive_force`     VARCHAR(500) DEFAULT NULL         COMMENT '驱动力（用户提交该类型反馈的动机）',
    `expected_result` VARCHAR(500) DEFAULT NULL         COMMENT '期望结果',
    `tone_tendency`   VARCHAR(255) DEFAULT NULL         COMMENT '语气倾向',
    `icon`            VARCHAR(128) DEFAULT NULL         COMMENT '分类图标',
    `sort_order`      INT          NOT NULL DEFAULT 0  COMMENT '排序',
    `is_admin_only`   TINYINT      NOT NULL DEFAULT 0  COMMENT '是否仅管理员可用（0-否 1-是）',
    `allow_comment`   TINYINT      NOT NULL DEFAULT 1  COMMENT '是否允许评论（0-否 1-是）',
    `is_enabled`      TINYINT      NOT NULL DEFAULT 1  COMMENT '是否启用（0-否 1-是）',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈分类表';

-- --------------------------------------------------------
-- 5. assistant_feedback: 建表
--    AssistantFeedback 实体字段 + OSHBaseEntity 继承字段
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `assistant_feedback` (
    `id`              BIGINT        NOT NULL COMMENT '工单ID',
    `user_id`         BIGINT        NOT NULL COMMENT '提交用户ID',
    `category_id`     BIGINT        DEFAULT NULL COMMENT '分类ID',
    `ticket_no`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '工单编号',
    `title`           VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '反馈标题',
    `content`         TEXT          COMMENT '反馈详细内容',
    `images`          TEXT          COMMENT '反馈图片JSON数组',
    `status`          VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '反馈状态',
    `is_pinned`       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否置顶（0-否 1-是）',
    `pin_order`       INT           NOT NULL DEFAULT 0 COMMENT '置顶排序',
    `comment_count`   INT           NOT NULL DEFAULT 0 COMMENT '评论数量',
    `view_count`      INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `like_count`      INT           NOT NULL DEFAULT 0 COMMENT '点赞数量',
    `favorite_count`  INT           NOT NULL DEFAULT 0 COMMENT '收藏数量',
    `result`          TEXT          COMMENT '处理结果说明',
    `page_path`       VARCHAR(512)  DEFAULT NULL COMMENT '反馈来源页面路径',
    `handler_id`      BIGINT        DEFAULT NULL COMMENT '处理人ID',
    `handler_name`    VARCHAR(64)   DEFAULT NULL COMMENT '处理人名称',
    `handled_time`    DATETIME      DEFAULT NULL COMMENT '最近处理时间',
    `close_reason`    VARCHAR(500)  DEFAULT NULL COMMENT '关闭原因',
    `hot_score`       INT           NOT NULL DEFAULT 0 COMMENT '热度分',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       BIGINT        DEFAULT NULL COMMENT '创建者',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`       BIGINT        DEFAULT NULL COMMENT '更新者',
    `delete_flag`     TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈工单表';

-- --------------------------------------------------------
-- 6. osh_bbs_category: 补 delete_flag
--    GlobalLogicDeleteInterceptor 对所有 osh_ 表追加 delete_flag=0
--    表中已有 is_delete 列，但拦截器只认 delete_flag
-- --------------------------------------------------------
ALTER TABLE osh_bbs_category
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）' AFTER is_delete;

-- --------------------------------------------------------
-- 7. osh_info_gap: 建表
--    OshInfoGap 实体: id, no, userId, userName, title, tag, content,
--      goodCount, middleCount, badCount, collectCount, viewCount,
--      status, updateBy, createTime, updateTime, deleteFlag
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `osh_info_gap` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '信息差ID',
    `no`            VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '编号',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `user_name`     VARCHAR(64)   DEFAULT NULL COMMENT '用户名',
    `title`         VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '标题',
    `tag`           VARCHAR(255)  DEFAULT NULL COMMENT '标签（逗号分隔）',
    `content`       TEXT          COMMENT '内容',
    `good_count`    INT           NOT NULL DEFAULT 0 COMMENT '好评数',
    `middle_count`  INT           NOT NULL DEFAULT 0 COMMENT '中评数',
    `bad_count`     INT           NOT NULL DEFAULT 0 COMMENT '差评数',
    `collect_count` INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
    `view_count`    INT           NOT NULL DEFAULT 0 COMMENT '浏览数',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态（0-草稿 2-待审核 4-已发布 6-已下架）',
    `update_by`     BIGINT        DEFAULT NULL COMMENT '更新者',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`   TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信息差主表';
