SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `osh_redeem_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `channel_code` varchar(64) NOT NULL COMMENT '渠道编码',
  `channel_name` varchar(100) NOT NULL COMMENT '渠道名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NOT NULL DEFAULT 0 COMMENT '更新人',
  `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_status_sort` (`status`, `sort`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销码支付渠道';

CREATE TABLE IF NOT EXISTS `osh_redeem_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) NOT NULL COMMENT '核销码',
  `user_id` bigint DEFAULT NULL COMMENT '核销用户ID，待核销时为空',
  `resource_no` varchar(32) NOT NULL COMMENT '资源编号',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型',
  `product_type` tinyint NOT NULL COMMENT '统一订单商品类型',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(255) NOT NULL COMMENT '商品名称',
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
  `channel_id` bigint NOT NULL COMMENT '渠道ID',
  `channel_name` varchar(100) NOT NULL COMMENT '渠道名称快照',
  `order_no` varchar(64) DEFAULT NULL COMMENT '核销后订单号',
  `payment_no` varchar(64) DEFAULT NULL COMMENT '核销后支付流水号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待核销，1已核销，2已作废',
  `redeem_time` datetime DEFAULT NULL COMMENT '核销时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '作废时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NOT NULL DEFAULT 0 COMMENT '更新人',
  `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_resource_no` (`resource_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销码';

CREATE TABLE IF NOT EXISTS `osh_redeem_invite_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `redeem_code_id` bigint NOT NULL COMMENT '核销码ID',
  `redeem_code` varchar(32) NOT NULL COMMENT '核销码快照',
  `link_token` varchar(64) NOT NULL COMMENT '核销邀请链接Token',
  `invite_code` varchar(32) DEFAULT NULL COMMENT '邀请注册使用的邀请码',
  `inviter_id` bigint DEFAULT NULL COMMENT '邀请人用户ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `register_email` varchar(128) DEFAULT NULL COMMENT '通过链接提交注册的邮箱',
  `register_user_id` bigint DEFAULT NULL COMMENT '通过链接注册成功的用户ID',
  `register_time` datetime DEFAULT NULL COMMENT '注册成功时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` bigint NOT NULL DEFAULT 0 COMMENT '更新人',
  `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_redeem_code_id` (`redeem_code_id`),
  UNIQUE KEY `uk_link_token` (`link_token`),
  KEY `idx_inviter_id` (`inviter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_register_user_id` (`register_user_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销邀请链接';

-- Existing environments may have created user_id as NOT NULL. Make pending codes unbound.
SET @column_nullable := (
  SELECT IS_NULLABLE
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_redeem_code'
    AND column_name = 'user_id'
);
SET @ddl := IF(@column_nullable = 'NO',
  'ALTER TABLE `osh_redeem_code` MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT ''核销用户ID，待核销时为空''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `osh_redeem_channel`
  (`channel_code`, `channel_name`, `status`, `sort`, `remark`, `create_by`, `update_by`, `delete_flag`)
VALUES
  ('wechat_transfer', '微信转账', 1, 0, '默认线下核销渠道', 0, 0, 0)
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `status` = 1,
  `delete_flag` = 0,
  `update_time` = CURRENT_TIMESTAMP;

-- Resource search and resource number lookup indexes.
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_course'
    AND index_name = 'idx_redeem_course_no'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `osh_course` ADD INDEX `idx_redeem_course_no` (`no`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_book'
    AND index_name = 'idx_redeem_book_no'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `osh_book` ADD INDEX `idx_redeem_book_no` (`no`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_book'
    AND index_name = 'idx_redeem_book_title'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `osh_book` ADD INDEX `idx_redeem_book_title` (`title`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_tool'
    AND index_name = 'idx_redeem_tool_no'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `osh_tool` ADD INDEX `idx_redeem_tool_no` (`no`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_member_plan'
    AND index_name = 'idx_redeem_member_plan_no'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD INDEX `idx_redeem_member_plan_no` (`no`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'osh_member_plan'
    AND index_name = 'idx_redeem_member_plan_name'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `osh_member_plan` ADD INDEX `idx_redeem_member_plan_name` (`plan_name`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill one invite link for historical redeem codes that do not have a link yet.
INSERT INTO `osh_redeem_invite_link`
  (`redeem_code_id`, `redeem_code`, `link_token`, `invite_code`, `inviter_id`, `status`, `create_by`, `update_by`, `delete_flag`)
SELECT
  rc.id,
  rc.code,
  LOWER(REPLACE(UUID(), '-', '')),
  u.invite_code,
  NULLIF(rc.create_by, 0),
  1,
  rc.create_by,
  rc.create_by,
  0
FROM `osh_redeem_code` rc
LEFT JOIN `osh_redeem_invite_link` ril
  ON ril.redeem_code_id = rc.id
  AND ril.delete_flag = 0
LEFT JOIN `osh_user` u
  ON u.id = NULLIF(rc.create_by, 0)
  AND u.delete_flag = 0
WHERE rc.delete_flag = 0
  AND ril.id IS NULL;
