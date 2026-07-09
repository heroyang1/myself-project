-- =====================================================
-- 工具主表 osh_tool 重建脚本
-- 说明：
-- 1. 本脚本只处理 osh_tool 主表，不处理标签、收藏、套餐、评价等关联表
-- 2. 执行会删除 osh_tool 现有数据，请先备份生产数据
-- 3. 当前结构已包含工具展开背景图字段 background_image
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `osh_tool`;

CREATE TABLE `osh_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `no` char(8) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '工具编号',
  `tool_name` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '工具名称',
  `description` varchar(1000) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '工具描述',
  `access_type` tinyint NOT NULL DEFAULT '1' COMMENT '访问类型：1-站内工具，2-iframe第三方工具',
  `route_path` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '站内工具前端路由',
  `iframe_url` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '第三方iframe地址',
  `github_url` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'GitHub地址',
  `background_image` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '工具展开背景图OSS相对路径',
  `quota_cost` int NOT NULL DEFAULT '0' COMMENT '单次消耗工具点数',
  `status` tinyint NOT NULL DEFAULT '2' COMMENT '状态：2-待审核，4-上架，6-下架',
  `good_count` int NOT NULL DEFAULT '0' COMMENT '好评数',
  `neutral_count` int NOT NULL DEFAULT '0' COMMENT '中评数',
  `bad_count` int NOT NULL DEFAULT '0' COMMENT '差评数',
  `view_count` bigint NOT NULL DEFAULT '0' COMMENT '浏览数',
  `collection_count` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `total_usage` bigint NOT NULL DEFAULT '0' COMMENT '累计使用次数',
  `remark` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '备注',
  `resource_type` varchar(20) COLLATE utf8mb4_0900_as_cs DEFAULT 'FREE' COMMENT '资源类型：FREE,CASH_POINT',
  `level` tinyint DEFAULT '1' COMMENT '资源等级',
  `create_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_no` (`no`),
  KEY `idx_tool_status` (`status`,`delete_flag`),
  KEY `idx_tool_resource_type` (`resource_type`,`delete_flag`),
  KEY `idx_tool_total_usage` (`total_usage`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='工具表';

SET FOREIGN_KEY_CHECKS = 1;
