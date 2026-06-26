-- Ebook module table structure adjustment
-- Generated from the ebook refactor session.
-- Purpose: remove legacy del_flag fields and keep unified OSHBaseEntity delete_flag logic.

-- osh_book: use delete_flag instead of legacy del_flag
ALTER TABLE osh_book
  DROP COLUMN del_flag;

-- osh_book_chapter: use delete_flag instead of legacy del_flag
ALTER TABLE osh_book_chapter
  DROP COLUMN del_flag;

-- osh_book_tag: use delete_flag instead of legacy del_flag
ALTER TABLE osh_book_tag
  DROP COLUMN del_flag;

CREATE TABLE IF NOT EXISTS `osh_book_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '书签ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `book_id` bigint NOT NULL COMMENT '电子书ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `chapter_title` varchar(200) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '章节标题快照',
  `selected_text` text COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '用户选中的书签文本',
  `prefix_text` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '选中文本前文',
  `suffix_text` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '选中文本后文',
  `start_offset` int DEFAULT NULL COMMENT '章节纯文本起始偏移',
  `end_offset` int DEFAULT NULL COMMENT '章节纯文本结束偏移',
  `progress_percent` decimal(8,4) NOT NULL DEFAULT '0.0000' COMMENT '章节内阅读进度百分比(0-100)',
  `note` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '用户备注',
  `label` varchar(100) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '书签自定义标签名称',
  `color` varchar(32) COLLATE utf8mb4_0900_as_cs DEFAULT '#fef08a' COMMENT '书签颜色',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `delete_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记(0未删,1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_user_book` (`user_id`, `book_id`, `delete_flag`),
  KEY `idx_user_chapter` (`user_id`, `chapter_id`, `delete_flag`),
  KEY `idx_book_chapter` (`book_id`, `chapter_id`, `delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='电子书用户书签表';

-- Rollback / delete table statement:
-- DROP TABLE IF EXISTS `osh_book_bookmark`;

-- Existing table upgrade statement:
-- ALTER TABLE `osh_book_bookmark`
--   ADD COLUMN `progress_percent` decimal(8,4) NOT NULL DEFAULT '0.0000' COMMENT '章节内阅读进度百分比(0-100)' AFTER `end_offset`;
-- ALTER TABLE `osh_book_bookmark`
--   ADD COLUMN `label` varchar(100) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '书签自定义标签名称' AFTER `note`;


-- 为 osh_book_bookmark 表添加 label 字段，支持用户自定义书签标签名称
-- 执行日期：2026-06-25

ALTER TABLE `osh_book_bookmark`
  ADD COLUMN `label` varchar(100) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '书签自定义标签名称' AFTER `note`;

-- Rollback / 回滚语句:
-- ALTER TABLE `osh_book_bookmark` DROP COLUMN `label`;


-- ============================================================
-- 书签图片章节适配：记录图片位置索引
-- 执行日期：2026-06-25
--
-- 需求背景：
-- 图片章节（漫画模式）内容为纯图片 Markdown，无连续文本，startOffset/endOffset 无意义。
-- 书签应记录"在第几张图片位置"（从 0 开始），方便定位和跳转。
-- ============================================================

ALTER TABLE `osh_book_bookmark`
  ADD COLUMN `image_index` int DEFAULT NULL COMMENT '图片章节书签：图片索引位置（从0开始，仅图片章节使用）' AFTER `progress_percent`;

-- Rollback / 回滚语句:
-- ALTER TABLE `osh_book_bookmark` DROP COLUMN `image_index`;


-- ============================================================
-- 电子书图片构建章节 / 漫画模式表结构设计
-- 执行日期：2026-06-25
--
-- 需求目标：
-- 1. 支持一次性上传多张图片到 OSS。
-- 2. 图片先进入电子书图片资源池，用户可选择部分图片组成章节。
-- 3. 一个章节可追加、删除、拖拽排序图片。
-- 4. 系统根据章节图片顺序自动生成 Markdown 内容并写入 osh_book_chapter.content。
-- 5. 电子书封面、简介、权限、标签等基础信息保持现有结构不变。
-- 6. OSS 上传路径建议在 UploadPathEnum 增加：BOOK_CONTENT_IMAGE("common/image/book/content/")。
--
-- 实施计划：
-- 1. 后端新增/复用电子书内容图片批量上传接口，上传到 OSS 后写入 osh_book_image_asset。
-- 2. 前端电子书创建/编辑页增加“图片构建章节”模式，提供图片池、章节分组、拖拽排序。
-- 3. 用户将图片池中的图片分配到章节后，保存章节与图片关系到 osh_book_chapter_image。
-- 4. 后端或前端按 osh_book_chapter_image.sort_order 生成 Markdown 图片内容。
-- 5. 生成结果写回 osh_book_chapter.content，保留原有阅读渲染链路。
-- 6. 后续编辑章节时，从 osh_book_chapter_image 还原图片列表，允许追加/删除/排序后重新生成 Markdown。
-- ============================================================

-- osh_book_chapter: 增加章节内容来源类型，用于区分手动 Markdown 与图片生成 Markdown
ALTER TABLE `osh_book_chapter`
  ADD COLUMN `content_source` tinyint NOT NULL DEFAULT 0 COMMENT '章节内容来源：0手动Markdown，1图片自动生成Markdown' AFTER `content`,
  ADD COLUMN `content_generate_time` datetime DEFAULT NULL COMMENT '图片章节Markdown最近生成时间' AFTER `content_source`;

-- 电子书图片资源池：记录一次/多次上传到 OSS 的图片资源，未分配章节的图片也可保留在资源池中
CREATE TABLE IF NOT EXISTS `osh_book_image_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '电子书图片资源ID',
  `book_id` bigint NOT NULL COMMENT '电子书ID',
  `oss_key` varchar(512) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'OSS对象Key/相对路径',
  `original_name` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '原始文件名',
  `file_ext` varchar(32) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '文件扩展名',
  `mime_type` varchar(100) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'MIME类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(bytes)',
  `width` int DEFAULT NULL COMMENT '图片宽度(px)',
  `height` int DEFAULT NULL COMMENT '图片高度(px)',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '图片池默认排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0未删,1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_book_sort` (`book_id`, `sort_order`, `delete_flag`),
  KEY `idx_book_create` (`book_id`, `create_time`, `delete_flag`),
  KEY `idx_oss_key` (`oss_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='电子书图片资源池表';

-- 章节图片关系表：记录哪些图片组成章节，以及章节内图片顺序
CREATE TABLE IF NOT EXISTS `osh_book_chapter_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节图片关系ID',
  `book_id` bigint NOT NULL COMMENT '电子书ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `image_asset_id` bigint NOT NULL COMMENT '电子书图片资源ID',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '章节内图片排序',
  `alt_text` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT '生成Markdown时使用的图片替代文本',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0未删,1已删)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chapter_asset` (`chapter_id`, `image_asset_id`, `delete_flag`),
  KEY `idx_book_chapter_sort` (`book_id`, `chapter_id`, `sort_order`, `delete_flag`),
  KEY `idx_image_asset` (`image_asset_id`, `delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='电子书章节图片编排表';

-- Markdown 生成规则建议：
-- SELECT a.oss_key, ci.alt_text
-- FROM osh_book_chapter_image ci
-- JOIN osh_book_image_asset a ON a.id = ci.image_asset_id AND a.delete_flag = 0
-- WHERE ci.chapter_id = ? AND ci.delete_flag = 0
-- ORDER BY ci.sort_order ASC, ci.id ASC;
-- 逐行生成：![alt_text](OSS访问URL)

-- Rollback / 回滚语句：
-- ALTER TABLE `osh_book_chapter`
--   DROP COLUMN `content_generate_time`,
--   DROP COLUMN `content_source`;
-- DROP TABLE IF EXISTS `osh_book_chapter_image`;
-- DROP TABLE IF EXISTS `osh_book_image_asset`;

-- ============================================================
-- Book bookmark enhancement note
-- No additional schema changes are required for this frontend update.
-- Existing osh_book_bookmark columns already cover label, color, progress_percent,
-- image_index, start_offset, and end_offset.
-- ============================================================

-- Align ebook audit user columns with OSHBaseEntity(Long createBy/updateBy).
UPDATE osh_book
SET create_by = NULL
WHERE create_by IS NOT NULL
  AND create_by <> ''
  AND create_by NOT REGEXP '^[0-9]+$';

UPDATE osh_book
SET update_by = NULL
WHERE update_by IS NOT NULL
  AND update_by <> ''
  AND update_by NOT REGEXP '^[0-9]+$';

UPDATE osh_book_chapter
SET create_by = NULL
WHERE create_by IS NOT NULL
  AND create_by <> ''
  AND create_by NOT REGEXP '^[0-9]+$';

UPDATE osh_book_chapter
SET update_by = NULL
WHERE update_by IS NOT NULL
  AND update_by <> ''
  AND update_by NOT REGEXP '^[0-9]+$';

UPDATE osh_book_tag
SET create_by = NULL
WHERE create_by IS NOT NULL
  AND create_by <> ''
  AND create_by NOT REGEXP '^[0-9]+$';

UPDATE osh_book_tag
SET update_by = NULL
WHERE update_by IS NOT NULL
  AND update_by <> ''
  AND update_by NOT REGEXP '^[0-9]+$';

ALTER TABLE osh_book
    MODIFY create_by BIGINT NULL DEFAULT NULL,
    MODIFY update_by BIGINT NULL DEFAULT NULL;

ALTER TABLE osh_book_chapter
    MODIFY create_by BIGINT NULL DEFAULT NULL,
    MODIFY update_by BIGINT NULL DEFAULT NULL;

ALTER TABLE osh_book_tag
    MODIFY create_by BIGINT NULL DEFAULT NULL,
    MODIFY update_by BIGINT NULL DEFAULT NULL;

-- ============================================================
-- Book-level media type and comic chapter pages
-- media_type: 1 ebook, 2 comic. The existing BOOK resource number prefix is reused.
-- ============================================================

ALTER TABLE `osh_book`
    ADD COLUMN `media_type` tinyint NOT NULL DEFAULT 1 COMMENT 'Book media type: 1 ebook, 2 comic' AFTER `level`;

CREATE TABLE IF NOT EXISTS `osh_book_chapter_page` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Comic page id',
    `book_id` bigint NOT NULL COMMENT 'Book id',
    `chapter_id` bigint NOT NULL COMMENT 'Chapter id',
    `page_no` int NOT NULL COMMENT 'Page number, starts from 1',
    `image_url` varchar(512) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'OSS key or relative path',
    `original_name` varchar(255) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'Original file name',
    `mime_type` varchar(100) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'MIME type',
    `file_size` bigint DEFAULT NULL COMMENT 'File size in bytes',
    `width` int DEFAULT NULL COMMENT 'Image width in pixels',
    `height` int DEFAULT NULL COMMENT 'Image height in pixels',
    `checksum` varchar(128) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'File checksum',
    `remark` varchar(500) COLLATE utf8mb4_0900_as_cs DEFAULT NULL COMMENT 'Remark',
    `create_by` bigint DEFAULT NULL COMMENT 'Creator',
    `create_time` datetime DEFAULT NULL COMMENT 'Create time',
    `update_by` bigint DEFAULT NULL COMMENT 'Updater',
    `update_time` datetime DEFAULT NULL COMMENT 'Update time',
    `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Logic delete flag: 0 active, 1 deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chapter_page` (`chapter_id`, `page_no`, `delete_flag`),
    KEY `idx_book_chapter_page` (`book_id`, `chapter_id`, `page_no`, `delete_flag`),
    KEY `idx_checksum` (`checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs COMMENT='Comic chapter page table';

-- Rollback:
-- DROP TABLE IF EXISTS `osh_book_chapter_page`;
-- ALTER TABLE `osh_book` DROP COLUMN `media_type`;

-- Fix comic page replacement uniqueness:
-- Comic page rows are chapter layout snapshots and are physically replaced when saving a chapter.
-- Keep the active-page uniqueness scoped by book + chapter + page number.
ALTER TABLE `osh_book_chapter_page`
    DROP INDEX `uk_chapter_page`,
    ADD UNIQUE KEY `uk_book_chapter_page` (`book_id`, `chapter_id`, `page_no`, `delete_flag`);
