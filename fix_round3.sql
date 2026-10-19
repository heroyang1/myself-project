-- fix_round3.sql - Fix remaining DB issues from test round 3
USE `ry-vue`;

-- 1. osh_announcement: add icon_code, channel, resource_type, resource_id, sort, status columns
ALTER TABLE `osh_announcement`
  ADD COLUMN `icon_code` VARCHAR(64) DEFAULT '' AFTER `link`,
  ADD COLUMN `channel` TINYINT DEFAULT 0 AFTER `icon_code`,
  ADD COLUMN `resource_type` VARCHAR(64) DEFAULT '' AFTER `channel`,
  ADD COLUMN `resource_id` BIGINT DEFAULT 0 AFTER `resource_type`,
  ADD COLUMN `sort` INT DEFAULT 0 AFTER `status`;

-- 2. osh_website_tag: add use_count column
ALTER TABLE `osh_website_tag`
  ADD COLUMN `use_count` INT DEFAULT 0 AFTER `tag_name`;
ALTER TABLE `osh_website_tag`
  ADD INDEX `idx_use_count` (`use_count`);

-- 3. CREATE TABLE: osh_examination
CREATE TABLE IF NOT EXISTS `osh_examination` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Exam title',
  `cover` VARCHAR(512) DEFAULT '' COMMENT 'Cover image URL',
  `description` TEXT COMMENT 'Description',
  `total_score` INT DEFAULT 0 COMMENT 'Total score',
  `pass_score` INT DEFAULT 0 COMMENT 'Pass score',
  `expire` INT DEFAULT 0 COMMENT 'Time limit (minutes)',
  `question_count` INT DEFAULT 0 COMMENT 'Question count',
  `collect_count` INT DEFAULT 0 COMMENT 'Collect count',
  `status` TINYINT DEFAULT 1 COMMENT 'Status: 0=draft 1=published',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Examination';

-- 4. CREATE TABLE: assistant_feedback_tag_rel
CREATE TABLE IF NOT EXISTS `assistant_feedback_tag_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `feedback_id` BIGINT NOT NULL COMMENT 'Feedback ID',
  `tag_id` BIGINT NOT NULL COMMENT 'Tag ID',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_feedback_id` (`feedback_id`),
  INDEX `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feedback tag relation';

-- 5. CREATE TABLE: osh_home_page_carousel
CREATE TABLE IF NOT EXISTS `osh_home_page_carousel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sort` INT DEFAULT 0 COMMENT 'Sort order',
  `emoji` VARCHAR(32) DEFAULT '' COMMENT 'Emoji icon',
  `icon_bg` VARCHAR(64) DEFAULT '' COMMENT 'Icon background color',
  `card_bg` VARCHAR(64) DEFAULT '' COMMENT 'Card background color',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'Title',
  `subtitle` VARCHAR(256) DEFAULT '' COMMENT 'Subtitle',
  `btn_text` VARCHAR(64) DEFAULT '' COMMENT 'Button text',
  `path` VARCHAR(256) DEFAULT '' COMMENT 'Link path',
  `feature1` VARCHAR(128) DEFAULT '' COMMENT 'Feature 1',
  `feature1_icon` VARCHAR(64) DEFAULT '' COMMENT 'Feature 1 icon',
  `feature2` VARCHAR(128) DEFAULT '' COMMENT 'Feature 2',
  `feature2_icon` VARCHAR(64) DEFAULT '' COMMENT 'Feature 2 icon',
  `feature3` VARCHAR(128) DEFAULT '' COMMENT 'Feature 3',
  `feature3_icon` VARCHAR(64) DEFAULT '' COMMENT 'Feature 3 icon',
  `is_visible` TINYINT DEFAULT 1 COMMENT 'Visible flag',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_visible_sort` (`is_visible`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Home page carousel';

-- 6. CREATE TABLE: osh_question_answer_question (for QA Hot)
CREATE TABLE IF NOT EXISTS `osh_question_answer_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `content` TEXT COMMENT 'Question content',
  `resource_type` VARCHAR(64) DEFAULT '' COMMENT 'Resource type',
  `status` TINYINT DEFAULT 0 COMMENT 'Status: 0=open 1=answered 2=closed',
  `view_count` INT DEFAULT 0 COMMENT 'View count',
  `follow_count` INT DEFAULT 0 COMMENT 'Follow count',
  `user_id` BIGINT DEFAULT 0 COMMENT 'User ID',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QA questions';

-- 7. CREATE TABLE: osh_question_answer_answer (for QA join)
CREATE TABLE IF NOT EXISTS `osh_question_answer_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT NOT NULL COMMENT 'Question ID',
  `content` TEXT COMMENT 'Answer content',
  `user_id` BIGINT DEFAULT 0 COMMENT 'User ID',
  `status` TINYINT DEFAULT 0 COMMENT 'Status: 0=pending 1=accepted',
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QA answers';
