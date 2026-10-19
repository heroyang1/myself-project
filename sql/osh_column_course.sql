/*
 Navicat Premium Data Transfer

 Source Server         : backstage
 Source Server Type    : MySQL
 Source Server Version : 80408 (8.4.8)
 Source Host           : 43.242.200.25:3306
 Source Schema         : backstage

 Target Server Type    : MySQL
 Target Server Version : 80408 (8.4.8)
 File Encoding         : 65001

 Date: 19/03/2026 17:31:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for osh_column_course
-- ----------------------------
DROP TABLE IF EXISTS `osh_column_course`;
CREATE TABLE `osh_column_course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `column_id` bigint NOT NULL COMMENT '专栏ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `sort` int DEFAULT '0' COMMENT '排序(课程在专栏里的展示顺序)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_column` (`column_id`),
  KEY `idx_course` (`course_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='专栏课程关联表';

SET FOREIGN_KEY_CHECKS = 1;
