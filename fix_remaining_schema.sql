-- fix_remaining_schema.sql - Fix all remaining DB issues found during testing
USE `ry-vue`;

-- ============================================================
-- 1. CREATE TABLE: osh_course_collection
-- ============================================================
CREATE TABLE IF NOT EXISTS `osh_course_collection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `course_id` BIGINT NOT NULL COMMENT 'Course ID',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`, `course_id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Course collection/favorites';

-- ============================================================
-- 2. CREATE TABLE: osh_open_project_tech_component
-- ============================================================
CREATE TABLE IF NOT EXISTS `osh_open_project_tech_component` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `no` VARCHAR(32) DEFAULT '' COMMENT 'Component number',
  `component_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'Component name',
  `component_code` VARCHAR(64) DEFAULT '' COMMENT 'Component code',
  `component_desc` VARCHAR(512) DEFAULT '' COMMENT 'Description',
  `official_url` VARCHAR(512) DEFAULT '' COMMENT 'Official website URL',
  `sort_order` INT DEFAULT 0 COMMENT 'Sort order',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_sort` (`sort_order`),
  INDEX `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Open project tech components';

-- ============================================================
-- 3. ALTER TABLE: osh_column - Add delete_flag
-- ============================================================
ALTER TABLE `osh_column`
  ADD COLUMN `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag' AFTER `type`;
ALTER TABLE `osh_column`
  ADD INDEX `idx_delete_flag` (`delete_flag`);

-- ============================================================
-- 4. ALTER TABLE: osh_course - Add missing columns
-- ============================================================
ALTER TABLE `osh_course`
  ADD COLUMN `no` VARCHAR(32) DEFAULT '' COMMENT 'Resource number' AFTER `id`,
  ADD COLUMN `total_duration` INT DEFAULT 0 COMMENT 'Total duration (minutes)' AFTER `remark`,
  ADD COLUMN `video_count` INT DEFAULT 0 COMMENT 'Video count' AFTER `total_duration`,
  ADD COLUMN `like_count` INT DEFAULT 0 COMMENT 'Like count' AFTER `view_count`,
  ADD COLUMN `comment_count` INT DEFAULT 0 COMMENT 'Comment count' AFTER `like_count`,
  ADD COLUMN `question_count` INT DEFAULT 0 COMMENT 'Question count' AFTER `comment_count`,
  ADD COLUMN `collection_count` INT DEFAULT 0 COMMENT 'Collection count' AFTER `question_count`,
  ADD COLUMN `rating_score` DECIMAL(3,2) DEFAULT 0.00 COMMENT 'Average rating score' AFTER `collection_count`,
  ADD COLUMN `free_type` TINYINT DEFAULT 0 COMMENT 'Free type: 0=free 1=partial 2=timed 3=paid' AFTER `rating_score`,
  ADD COLUMN `after_service_days` INT DEFAULT 0 COMMENT 'After-sales service days' AFTER `free_type`,
  ADD COLUMN `resource_type` VARCHAR(32) DEFAULT '' COMMENT 'Resource type' AFTER `after_service_days`,
  ADD COLUMN `level` TINYINT DEFAULT 0 COMMENT 'Level/target audience' AFTER `resource_type`,
  ADD COLUMN `difficulty` TINYINT DEFAULT 0 COMMENT 'Difficulty: 1=beginner 2=basic 3=advanced' AFTER `level`,
  ADD COLUMN `try_content` TEXT COMMENT 'Trial content' AFTER `difficulty`;
