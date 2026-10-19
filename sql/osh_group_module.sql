-- ============================================================
-- 拼团功能模块 SQL（增强版）
-- 包含 5 张表：
--   1. osh_group_activity     拼团活动表（增强）
--   2. osh_group_work         组团表（增强）
--   3. osh_group_user         参团用户表（增强）
--   4. osh_group_order        拼团订单表（增强）
--   5. osh_group_notification 拼团通知记录表（新增）
--
-- 目标数据库：backstage
-- MySQL 版本：8.4.8
-- 字符集：utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ----------------------------
-- 1. 拼团活动表（核心主表）
-- 业务说明：定义一次拼团活动的完整配置，包括关联商品、价格、成团规则等
-- 关联关系：
--   goods_id + type → 多态关联到 osh_course / osh_column 等商品表
--   一个活动下可创建多个组团（osh_group_work）
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_activity`;
CREATE TABLE `osh_group_activity` (
    `id`                bigint          NOT NULL AUTO_INCREMENT              COMMENT '拼团活动ID（主键）',
    `school_id`         bigint          NOT NULL                             COMMENT '所属网校ID',
    `title`             varchar(200)    NOT NULL                             COMMENT '拼团活动标题',
    `type`              varchar(30)     NOT NULL                             COMMENT '关联商品类型：course-课程 column-专栏 book-电子书 live-直播 server_ai-AI服务器资源',
    `goods_id`          bigint          NOT NULL                             COMMENT '关联商品ID（根据type指向不同商品表的主键）',
    `goods_snapshot`    json            DEFAULT NULL                         COMMENT '商品快照（JSON格式，防止商品下架后信息丢失）',
    `original_price`    decimal(10,2)   NOT NULL DEFAULT 0.00                COMMENT '商品原价',
    `price`             decimal(10,2)   NOT NULL DEFAULT 0.00                COMMENT '拼团价格',
    `p_num`             int             NOT NULL DEFAULT 2                   COMMENT '成团人数（最少2人）',
    `max_groups`        int             NOT NULL DEFAULT 0                   COMMENT '最大组团数（0=不限制）',
    `per_user_limit`    int             NOT NULL DEFAULT 1                   COMMENT '每人限购次数（默认1次）',
    `duration_hours`    int             NOT NULL DEFAULT 24                  COMMENT '单次组团有效期（小时，默认24小时）',
    `start_time`        datetime        NOT NULL                             COMMENT '活动开始时间',
    `end_time`          datetime        NOT NULL                             COMMENT '活动结束时间',
    `status`            tinyint         NOT NULL DEFAULT 0                   COMMENT '活动状态：0-草稿 1-上架 2-下架 3-已结束',
    `auto_refund`       tinyint         NOT NULL DEFAULT 1                   COMMENT '未成团是否自动退款：0-否 1-是',
    `success_msg`       text            DEFAULT NULL                         COMMENT '成团后推送消息模板',
    `wechat_group_qr`   varchar(500)    DEFAULT NULL                         COMMENT '成团后的微信群二维码URL',
    `admin_contact`     varchar(200)    DEFAULT NULL                         COMMENT '管理员联系方式',
    `sort_order`        int             NOT NULL DEFAULT 0                   COMMENT '排序权重（值越大越靠前）',
    `cover`             varchar(512)    DEFAULT NULL                         COMMENT '封面图片URL',
    `created_by`        bigint          DEFAULT NULL                         COMMENT '创建人用户ID',
    `created_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `updated_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`       tinyint(1)      NOT NULL DEFAULT 0                   COMMENT '逻辑删除：0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_school_id`     (`school_id`),
    KEY `idx_type`          (`type`),
    KEY `idx_goods`         (`type`, `goods_id`),
    KEY `idx_status`        (`status`),
    KEY `idx_start_time`    (`start_time`),
    KEY `idx_end_time`      (`end_time`),
    KEY `idx_sort_order`    (`sort_order`),
    KEY `idx_delete_flag`   (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='拼团活动表';


-- ----------------------------
-- 2. 组团表（从表，挂在活动下）
-- 业务说明：一个拼团活动下可以有多个组团，每个组团由一个团长发起
-- 关联关系：
--   group_activity_id → osh_group_activity.id（多对一）
--   leader_user_id    → osh_user.id（多对一）
--   一个组团下有多个参团用户（osh_group_user）
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_work`;
CREATE TABLE `osh_group_work` (
    `id`                  bigint      NOT NULL AUTO_INCREMENT                COMMENT '组团ID（主键）',
    `group_activity_id`   bigint      NOT NULL                               COMMENT '关联拼团活动ID → osh_group_activity.id',
    `leader_user_id`      bigint      NOT NULL                               COMMENT '团长用户ID → osh_user.id',
    `num`                 int         NOT NULL DEFAULT 0                     COMMENT '已参团人数',
    `total`               int         NOT NULL DEFAULT 2                     COMMENT '成团所需总人数',
    `status`              tinyint     NOT NULL DEFAULT 0                     COMMENT '组团状态：0-进行中 1-已成团 2-已取消/过期',
    `expire`              datetime    NOT NULL                               COMMENT '组团过期时间',
    `success_time`        datetime    DEFAULT NULL                           COMMENT '成团时间（成团后记录）',
    `wechat_group_id`     varchar(100) DEFAULT NULL                          COMMENT '关联微信群ID（成团后创建）',
    `created_time`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_activity_id` (`group_activity_id`),
    KEY `idx_leader_user_id`    (`leader_user_id`),
    KEY `idx_status`            (`status`),
    KEY `idx_expire`            (`expire`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='组团表';


-- ----------------------------
-- 3. 参团用户表（中间关联表）
-- 业务说明：记录每个用户加入某个组团的关系，同时关联到支付订单
-- 关联关系：
--   group_work_id → osh_group_work.id（多对一）
--   user_id       → osh_user.id（多对一）
--   order_id      → osh_group_order.id（一对一）
-- 约束：同一组团中同一用户只能参与一次（联合唯一索引）
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_user`;
CREATE TABLE `osh_group_user` (
    `id`              bigint      NOT NULL AUTO_INCREMENT                    COMMENT '参团记录ID（主键）',
    `group_work_id`   bigint      NOT NULL                                   COMMENT '关联组团ID → osh_group_work.id',
    `user_id`         bigint      NOT NULL                                   COMMENT '参团用户ID → osh_user.id',
    `order_id`        bigint      DEFAULT NULL                               COMMENT '关联订单ID → osh_group_order.id',
    `is_leader`       tinyint     NOT NULL DEFAULT 0                         COMMENT '是否团长：0-否 1-是',
    `join_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP         COMMENT '加入时间',
    `status`          tinyint     NOT NULL DEFAULT 0                         COMMENT '参团状态：0-已加入 1-已退出/退款',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_user`   (`group_work_id`, `user_id`),
    KEY `idx_user_id`           (`user_id`),
    KEY `idx_order_id`          (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='参团用户表';


-- ----------------------------
-- 4. 拼团订单表（增强版，替换原 osh_group_order）
-- 业务说明：记录用户因参与拼团产生的支付订单
-- 关联关系：
--   user_id           → osh_user.id（多对一）
--   group_activity_id → osh_group_activity.id（多对一）
--   group_work_id     → osh_group_work.id（多对一）
-- 修复：
--   1. 新增 group_activity_id、group_work_id 关联字段（原表缺失）
--   2. 修正 status 注释中的拼写错误 pendding → pending
--   3. 新增 pay_method、pay_time、refund_time、goods_snapshot 字段
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_order`;
CREATE TABLE `osh_group_order` (
    `id`                  bigint          NOT NULL AUTO_INCREMENT             COMMENT '订单ID（主键）',
    `school_id`           bigint          NOT NULL                            COMMENT '所属网校ID',
    `user_id`             bigint          NOT NULL                            COMMENT '下单用户ID → osh_user.id',
    `group_activity_id`   bigint          DEFAULT NULL                        COMMENT '关联拼团活动ID → osh_group_activity.id',
    `group_work_id`       bigint          DEFAULT NULL                        COMMENT '关联组团ID → osh_group_work.id',
    `no`                  varchar(64)     NOT NULL                            COMMENT '订单编号（唯一）',
    `status`              varchar(20)     NOT NULL DEFAULT 'pending'          COMMENT '订单状态：pending-待支付 paid-已支付 success-拼团成功 refunding-退款中 refunded-已退款 cancel-已取消',
    `price`               decimal(10,2)   NOT NULL                            COMMENT '实际支付价格（拼团价）',
    `total_price`         decimal(10,2)   NOT NULL                            COMMENT '商品原价',
    `type`                varchar(20)     NOT NULL DEFAULT 'group'            COMMENT '订单类型：group-拼团',
    `pay_method`          varchar(20)     DEFAULT NULL                        COMMENT '支付方式：wechat-微信支付 alipay-支付宝',
    `pay_time`            datetime        DEFAULT NULL                        COMMENT '支付成功时间',
    `refund_time`         datetime        DEFAULT NULL                        COMMENT '退款完成时间',
    `goods_snapshot`      json            DEFAULT NULL                        COMMENT '下单时商品快照（JSON格式）',
    `created_time`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `updated_time`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_no`              (`no`),
    KEY `idx_school_id`             (`school_id`),
    KEY `idx_user_id`               (`user_id`),
    KEY `idx_group_activity_id`     (`group_activity_id`),
    KEY `idx_group_work_id`         (`group_work_id`),
    KEY `idx_status`                (`status`),
    KEY `idx_created_time`          (`created_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='拼团订单表';


-- ----------------------------
-- 5. 拼团通知记录表（新增）
-- 业务说明：记录拼团过程中向用户推送的各类通知
-- 关联关系：
--   group_work_id → osh_group_work.id（多对一）
--   user_id       → osh_user.id（多对一）
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_notification`;
CREATE TABLE `osh_group_notification` (
    `id`              bigint          NOT NULL AUTO_INCREMENT                 COMMENT '通知ID（主键）',
    `group_work_id`   bigint          NOT NULL                                COMMENT '关联组团ID → osh_group_work.id',
    `user_id`         bigint          NOT NULL                                COMMENT '接收用户ID → osh_user.id',
    `type`            varchar(30)     NOT NULL                                COMMENT '通知类型：join-有人参团 success-拼团成功 expire-即将过期 refund-退款通知',
    `content`         text            DEFAULT NULL                            COMMENT '通知内容',
    `is_read`         tinyint         NOT NULL DEFAULT 0                      COMMENT '是否已读：0-未读 1-已读',
    `created_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_work_id` (`group_work_id`),
    KEY `idx_user_id`       (`user_id`),
    KEY `idx_type`          (`type`),
    KEY `idx_is_read`       (`is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=1
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='拼团通知记录表';


-- ============================================================
-- 数据迁移：将旧 osh_group_order 中的历史数据状态值修正
-- 说明：原代码中 status 拼写为 'pendding'，需统一修正为 'pending'
-- 注意：仅在确认旧表数据已备份后执行
-- ============================================================
-- UPDATE `osh_group_order` SET `status` = 'pending' WHERE `status` = 'pendding';


SET FOREIGN_KEY_CHECKS = 1;
CREATE TABLE `osh_group_user_initiated` (
                                            `id`                    bigint          NOT NULL AUTO_INCREMENT              COMMENT '发起记录ID（主键）',
                                            `user_id`               bigint          NOT NULL                             COMMENT '发起人用户ID → osh_user.id',
                                            `order_id`              bigint          DEFAULT NULL                         COMMENT '关联订单ID → osh_group_order.id',

    -- 用户自定义规则
                                            `min_num`               int             NOT NULL DEFAULT 2                   COMMENT '最低成团人数',
                                            `max_num`               int             NOT NULL DEFAULT 5                   COMMENT '最多成团人数',
                                            `duration`              int             NOT NULL DEFAULT 12                  COMMENT '服务器使用时长（月）',
                                            `custom_price`          decimal(10,2)  NOT NULL DEFAULT 0.00                COMMENT '自定义拼团价格（总价）',

    -- 状态与进度
                                            `group_status`          tinyint         NOT NULL DEFAULT 0                   COMMENT '组团状态：0-招募中 1-已成团 2-已取消/过期',
                                            `current_num`           int             NOT NULL DEFAULT 1                   COMMENT '当前参团人数（含发起人自己）',

    -- 服务器信息（成团后分配）
                                            `server_ip`             varchar(50)     DEFAULT NULL                         COMMENT '服务器IP地址',
                                            `server_account`        varchar(100)    DEFAULT NULL                         COMMENT '服务器登录账号',
                                            `server_password`       varchar(255)    DEFAULT NULL                         COMMENT '服务器登录密码（AES加密存储）',
                                            `server_start_time`     datetime        DEFAULT NULL                         COMMENT '服务器开始使用时间',
                                            `server_expire_time`    datetime        DEFAULT NULL                         COMMENT '服务器到期时间',

    -- 时间
                                            `initiate_time`         datetime        DEFAULT CURRENT_TIMESTAMP            COMMENT '发起时间',
                                            `expire_time`           datetime        DEFAULT NULL                         COMMENT '招募截止时间',
                                            `create_time`           datetime        DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
                                            `update_time`           datetime        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `delete_flag`           tinyint(1)     DEFAULT 0                            COMMENT '逻辑删除：0-正常 1-已删除',

                                            PRIMARY KEY (`id`),
                                            KEY `idx_user_id`           (`user_id`),
                                            KEY `idx_order_id`          (`order_id`),
                                            KEY `idx_group_status`      (`group_status`),
                                            KEY `idx_initiate_time`     (`initiate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户发起拼团记录表';