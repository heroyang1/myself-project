-- ============================================================
-- fix_test_schema.sql - Fix missing tables and columns found during testing
-- Database: ry-vue
-- Generated: 2026-09-28
-- ============================================================

USE `ry-vue`;

-- ============================================================
-- 1. CREATE TABLE: osh_course_tag_rel (course-tag relationship)
--    Referenced by: OshCourseMapper.xml (pageQuerySearchCourse, pageQueryAllCoursesForEsSync)
--    Entity: OshCourseTagRel (id, courseId, tagId, deleteFlag, createBy, createTime, updateBy, updateTime)
-- ============================================================
CREATE TABLE IF NOT EXISTS `osh_course_tag_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `course_id` BIGINT NOT NULL COMMENT 'Course ID',
  `tag_id` BIGINT NOT NULL COMMENT 'Tag ID',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag: 0=active, 1=deleted',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT 'Updater',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_tag_id` (`tag_id`),
  INDEX `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Course-Tag relationship table';

-- ============================================================
-- 2. CREATE TABLE: osh_announcement (announcements)
--    Referenced by: OshOpenProjectAnnouncementMapper (selectLatestAnnouncements)
--    SQL from error: SELECT id, title, link, create_time FROM osh_announcement WHERE delete_flag=0 AND status=4 AND source_module='openproject'
-- ============================================================
CREATE TABLE IF NOT EXISTS `osh_announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Announcement title',
  `link` VARCHAR(512) DEFAULT '' COMMENT 'External link',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0=draft, 4=published',
  `source_module` VARCHAR(64) DEFAULT '' COMMENT 'Source module (e.g. openproject)',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag: 0=active, 1=deleted',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT 'Updater',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  INDEX `idx_source_module` (`source_module`),
  INDEX `idx_status_delete` (`status`, `delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Announcement table';

-- ============================================================
-- 3. ALTER TABLE: osh_group_work - Add delete_flag
--    Error: Unknown column 'osh_group_work.delete_flag' in 'where clause'
--    Referenced by: GroupMapper.xml (getGroupWorkListByActivityId)
--    Note: GlobalLogicDeleteInterceptor auto-appends WHERE delete_flag=0
-- ============================================================
ALTER TABLE `osh_group_work`
  ADD COLUMN `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag: 0=active, 1=deleted' AFTER `update_time`;
ALTER TABLE `osh_group_work`
  ADD INDEX `idx_delete_flag` (`delete_flag`);

-- ============================================================
-- 4. ALTER TABLE: osh_open_project_tag - Add no column
--    Error: Unknown column 'no' in 'field list'
--    Entity field: OshOpenProjectTag.no (String)
--    SQL: SELECT id,no,tag_name,tag_code,sort_order,... FROM osh_open_project_tag
-- ============================================================
ALTER TABLE `osh_open_project_tag`
  ADD COLUMN `no` VARCHAR(32) DEFAULT '' COMMENT 'Tag number/code' AFTER `id`;

-- ============================================================
-- 5. ALTER TABLE: osh_user_action_event - Add missing columns
--    Error: Unknown column 'request_uri' in 'field list'
--    Entity fields from OshUserEvent.java @TableField annotations:
--      request_uri, request_method, ip, user_agent, duration_ms,
--      result_code, contribution, resource_no, resource_name,
--      role_level, trace_id
-- ============================================================
ALTER TABLE `osh_user_action_event`
  ADD COLUMN `request_uri` VARCHAR(512) DEFAULT '' COMMENT 'Request URI' AFTER `status`,
  ADD COLUMN `request_method` VARCHAR(16) DEFAULT '' COMMENT 'HTTP method' AFTER `request_uri`,
  ADD COLUMN `ip` VARCHAR(64) DEFAULT '' COMMENT 'Client IP' AFTER `request_method`,
  ADD COLUMN `user_agent` VARCHAR(512) DEFAULT '' COMMENT 'User-Agent header' AFTER `ip`,
  ADD COLUMN `duration_ms` BIGINT DEFAULT 0 COMMENT 'Request duration in ms' AFTER `user_agent`,
  ADD COLUMN `result_code` VARCHAR(32) DEFAULT '' COMMENT 'Response code' AFTER `duration_ms`,
  ADD COLUMN `contribution` INT DEFAULT 0 COMMENT 'Contribution points' AFTER `result_code`,
  ADD COLUMN `resource_no` VARCHAR(64) DEFAULT '' COMMENT 'Resource number' AFTER `resource_id`,
  ADD COLUMN `resource_name` VARCHAR(255) DEFAULT '' COMMENT 'Resource name' AFTER `resource_no`,
  ADD COLUMN `role_level` INT DEFAULT 0 COMMENT 'User role level' AFTER `role`,
  ADD COLUMN `trace_id` VARCHAR(64) DEFAULT '' COMMENT 'Trace ID for distributed tracing' AFTER `contribution`;
