-- ============================================================
-- 服务器拼团模块 SQL
-- 包含 3 张核心表：
--   1. osh_group_activity     拼团活动表（服务器专用）
--   2. osh_group_work         组团参与表
--   3. osh_group_order        拼团订单表
--
-- 业务特点：
--   - 仅支持服务器资源拼团
--   - 用户不可主动开团，由系统/管理员自动创建
--   - 无截止时间，通过状态控制流程
--   - 动态定价：按剩余月数比例计算
--   - 成团后分配服务器资源，开始计时
--
-- 目标数据库：backstage
-- MySQL 版本：8.4.8
-- 字符集：utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ----------------------------
-- 1. 拼团活动表（核心主表 - 服务器专用）
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_activity`;
CREATE TABLE `osh_group_activity` (
    `id`                    bigint          NOT NULL AUTO_INCREMENT              COMMENT '拼团活动ID（主键）',
    `title`                 varchar(200)    NOT NULL                             COMMENT '拼团活动标题，如"高性能服务器5人拼团"',
    `type`                  varchar(20)     NOT NULL DEFAULT 'server'            COMMENT '拼团类型：server-服务器（固定值）',
    `cpu`                   varchar(50)     DEFAULT NULL                         COMMENT '服务器CPU配置，如"8核"',
    `memory`                varchar(50)     DEFAULT NULL                         COMMENT '服务器内存配置，如"16GB"',
    `storage`               varchar(100)    DEFAULT NULL                         COMMENT '服务器存储配置，如"200GB SSD"',
    `base_price`            decimal(10,2)  NOT NULL                             COMMENT '基础拼团价格（按月计算，完整周期价格）',
    `total_duration`        int             NOT NULL DEFAULT 12                  COMMENT '服务器总使用时长（月）',
    `group_min_num`         int             NOT NULL DEFAULT 2                   COMMENT '拼团所需最低人数',
    `group_max_num`         int             NOT NULL DEFAULT 5                   COMMENT '拼团人数上限',
    `current_num`           int             NOT NULL DEFAULT 0                   COMMENT '当前已参团人数',
    `status`                tinyint         NOT NULL DEFAULT 1                   COMMENT '活动状态：1-进行中 2-拼团成功 3-已结束',
    `start_time`            datetime        DEFAULT NULL                         COMMENT '拼团开始时间',
    `server_start_time`     datetime        DEFAULT NULL                         COMMENT '服务器开始使用时间（成团后有值）',
    `server_end_time`       datetime        DEFAULT NULL                         COMMENT '服务器使用结束时间（成团后有值）',
    `server_tutorial_url`   varchar(500)    DEFAULT NULL                         COMMENT '服务器配置教程URL',
    `admin_contact`         varchar(100)    DEFAULT NULL                         COMMENT '管理员联系方式（微信号）',
    `sort_order`            int             NOT NULL DEFAULT 0                   COMMENT '排序权重，数值越大越靠前',
    `cover`                 varchar(512)    DEFAULT NULL                         COMMENT '封面图片URL',
    `create_time`           datetime        DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_time`           datetime        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`           tinyint(1)      NOT NULL DEFAULT 0                   COMMENT '逻辑删除：0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_type`              (`type`),
    KEY `idx_status`            (`status`),
    KEY `idx_start_time`        (`start_time`),
    KEY `idx_sort_order`        (`sort_order`),
    KEY `idx_delete_flag`       (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器拼团活动表';


-- ----------------------------
-- 2. 组团参与表（记录每个用户的参团信息）
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_work`;
CREATE TABLE `osh_group_work` (
    `id`                    bigint          NOT NULL AUTO_INCREMENT              COMMENT '参团记录ID（主键）',
    `group_activity_id`     bigint          NOT NULL                           COMMENT '关联拼团活动ID → osh_group_activity.id',
    `user_id`               bigint          NOT NULL                            COMMENT '参团用户ID → osh_user.id',
    `order_id`              bigint          DEFAULT NULL                         COMMENT '关联订单ID → osh_group_order.id',
    `actual_price`          decimal(10,2)  NOT NULL                             COMMENT '用户实际支付价格（根据剩余月数动态计算）',
    `remaining_months`      decimal(10,4)  DEFAULT NULL                         COMMENT '参团时剩余可使用月数',
    `group_status`          tinyint         NOT NULL DEFAULT 0                   COMMENT '组团状态：0-进行中（未成团） 1-已成团 2-已取消/过期',
    `join_time`             datetime        DEFAULT NULL                         COMMENT '参团时间',
    `server_start_time`     datetime        DEFAULT NULL                       COMMENT '服务器开始使用时间（成团后有值）',
    `server_expire_time`    datetime        DEFAULT NULL                         COMMENT '服务器到期时间（成团后有值）',
    `create_time`           datetime        DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_time`           datetime        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_activity_id`   (`group_activity_id`),
    KEY `idx_user_id`            (`user_id`),
    KEY `idx_join_time`          (`join_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器拼团参团记录表';


-- ----------------------------
-- 3. 拼团订单表
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_order`;
CREATE TABLE `osh_group_order` (
    `id`                    bigint          NOT NULL AUTO_INCREMENT              COMMENT '订单ID（主键）',
    `user_id`               bigint          NOT NULL                            COMMENT '下单用户ID → osh_user.id',
    `group_activity_id`     bigint          NOT NULL                           COMMENT '关联拼团活动ID → osh_group_activity.id',
    `group_work_id`         bigint          DEFAULT NULL                         COMMENT '关联参团记录ID → osh_group_work.id',
    `order_no`              varchar(50)     NOT NULL                             COMMENT '订单编号（唯一）',
    `price`                 decimal(10,2)  NOT NULL                             COMMENT '实际支付价格（动态计算）',
    `base_price`            decimal(10,2)  DEFAULT NULL                         COMMENT '基础拼团价格（完整周期）',
    `remaining_months`       decimal(10,4)  DEFAULT NULL                         COMMENT '参团时剩余月数',
    `status`                varchar(20)     NOT NULL DEFAULT 'pending'           COMMENT '订单状态：pending-待支付 paid-已支付 success-拼团成功 refunded-已退款 cancelled-已取消',
    `pay_method`            varchar(20)     DEFAULT NULL                         COMMENT '支付方式：wechat-微信 alipay-支付宝',
    `pay_time`              datetime        DEFAULT NULL                         COMMENT '支付时间',
    `remark`                varchar(500)    DEFAULT NULL                         COMMENT '备注',
    `create_time`           datetime        DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_time`           datetime        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no`          (`order_no`),
    KEY `idx_user_id`               (`user_id`),
    KEY `idx_group_activity_id`     (`group_activity_id`),
    KEY `idx_group_work_id`         (`group_work_id`),
    KEY `idx_status`                (`status`),
    KEY `idx_create_time`           (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器拼团订单表';


-- ----------------------------
-- 测试数据
-- ----------------------------

-- 示例：创建一个服务器拼团活动
INSERT INTO osh_group_activity (
    title, type, cpu, memory, storage, 
    base_price, total_duration, group_min_num, group_max_num,
    current_num, status, start_time, server_tutorial_url, admin_contact, sort_order
) VALUES (
    '高性能AI服务器5人拼团', 
    'server', 
    '8核', 
    '16GB', 
    '200GB SSD',
    199.00, 
    12, 
    2, 
    5,
    1, 
    1, 
    NOW(), 
    '/tutorial/server-config', 
    'admin_wechat', 
    100
);

-- 示例：创建更多拼团活动
INSERT INTO osh_group_activity (
    title, type, cpu, memory, storage, 
    base_price, total_duration, group_min_num, group_max_num,
    current_num, status, start_time, server_tutorial_url, admin_contact, sort_order
) VALUES 
('标准型服务器3人拼团', 'server', '4核', '8GB', '100GB SSD', 99.00, 6, 2, 3, 2, 2, NOW(), '/tutorial/server-config', 'admin_wechat', 90),
('入门型服务器2人拼团', 'server', '2核', '4GB', '50GB SSD', 59.00, 3, 2, 2, 0, 1, NOW(), '/tutorial/server-config', 'admin_wechat', 80);

SET FOREIGN_KEY_CHECKS = 1;
