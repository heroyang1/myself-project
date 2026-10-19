-- ============================================================
-- 公告/动态表
-- 用于各模块（课程、电子书、开源项目、实用网站、信息差等）的公告与动态记录
-- ============================================================

DROP TABLE IF EXISTS `osh_announcement`;
CREATE TABLE `osh_announcement` (
    `id`              bigint(20)    NOT NULL AUTO_INCREMENT           COMMENT '主键ID',
    `title`           varchar(255)  NOT NULL DEFAULT ''               COMMENT '公告标题',
    `link`            varchar(512)  DEFAULT ''                        COMMENT '外链URL',
    `icon_code`       varchar(64)   DEFAULT ''                        COMMENT '图标编码（前端图标组件名）',
    `status`          tinyint(3)    NOT NULL DEFAULT 0                COMMENT '状态：0-草稿 4-已发布',
    `channel`         tinyint(3)    NOT NULL DEFAULT 0                COMMENT '频道：1-公告栏 2-动态栏',
    `resource_type`   varchar(64)   DEFAULT ''                        COMMENT '资源类型（course/book/openproject/website/info_gap）',
    `resource_id`     bigint(20)    DEFAULT 0                         COMMENT '关联资源ID',
    `sort`            int(11)       NOT NULL DEFAULT 0                COMMENT '排序权重（越大越靠前）',
    `source`          varchar(64)   DEFAULT 'system'                  COMMENT '来源（system/manual）',
    `source_module`   varchar(64)   DEFAULT ''                        COMMENT '来源模块标识（openproject/website/book等）',
    `start_time`      datetime      DEFAULT NULL                      COMMENT '公告生效起始时间',
    `end_time`        datetime      DEFAULT NULL                      COMMENT '公告生效截止时间',
    `delete_flag`     tinyint(1)    NOT NULL DEFAULT 0                COMMENT '逻辑删除：0-正常 1-删除',
    `create_by`       varchar(64)   DEFAULT ''                        COMMENT '创建者',
    `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    `update_by`       varchar(64)   DEFAULT ''                        COMMENT '更新者',
    `update_time`     datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_source_module`       (`source_module`),
    KEY `idx_status_delete`       (`status`, `delete_flag`),
    KEY `idx_resource`            (`resource_type`, `resource_id`),
    KEY `idx_delete_flag`         (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告/动态表';
