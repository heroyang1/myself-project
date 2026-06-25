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
