CREATE TABLE IF NOT EXISTS `osh_object_slim_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bucket` varchar(128) NOT NULL COMMENT '原始桶名',
  `object_key` varchar(1024) NOT NULL COMMENT '原始对象key',
  `backup_bucket` varchar(128) DEFAULT NULL COMMENT '备份桶名',
  `backup_key` varchar(1024) DEFAULT NULL COMMENT '备份对象key',
  `file_name` varchar(512) DEFAULT NULL COMMENT '文件名',
  `file_suffix` varchar(32) DEFAULT NULL COMMENT '文件后缀',
  `file_type` varchar(32) DEFAULT NULL COMMENT '文件类型 video/image/audio/doc/other',
  `source_type` varchar(64) DEFAULT NULL COMMENT '来源 course_section/oss_log/r2_scan',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID',
  `source_title` varchar(255) DEFAULT NULL COMMENT '来源标题',
  `original_size` bigint DEFAULT 0 COMMENT '原始大小，字节',
  `slim_size` bigint DEFAULT NULL COMMENT '瘦身后大小，字节',
  `saved_size` bigint DEFAULT NULL COMMENT '节省大小，字节',
  `saved_ratio` decimal(10,4) DEFAULT NULL COMMENT '节省比例，0-1',
  `slim_mode` varchar(32) DEFAULT NULL COMMENT '瘦身模式 quality/balanced/storage_saver/low_cost',
  `status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed/skipped/no_need/rolled_back/backup_deleted',
  `codec_before` varchar(64) DEFAULT NULL COMMENT '转码前编码',
  `codec_after` varchar(64) DEFAULT NULL COMMENT '转码后编码',
  `resolution_before` varchar(32) DEFAULT NULL COMMENT '转码前分辨率',
  `resolution_after` varchar(32) DEFAULT NULL COMMENT '转码后分辨率',
  `duration_before` decimal(14,3) DEFAULT NULL COMMENT '转码前时长，秒',
  `duration_after` decimal(14,3) DEFAULT NULL COMMENT '转码后时长，秒',
  `bitrate_before` bigint DEFAULT NULL COMMENT '转码前码率',
  `bitrate_after` bigint DEFAULT NULL COMMENT '转码后码率',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT '尝试次数',
  `last_error` varchar(1024) DEFAULT NULL COMMENT '最近错误',
  `last_scan_time` datetime DEFAULT NULL COMMENT '最近扫描时间',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `backup_deleted_time` datetime DEFAULT NULL COMMENT '备份删除时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bucket_object` (`bucket`, `object_key`(512)),
  KEY `idx_status` (`status`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象存储文件瘦身任务表';

DELIMITER $$

CREATE PROCEDURE add_object_slim_mode_column()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'osh_object_slim_task' AND COLUMN_NAME = 'slim_mode') THEN
        ALTER TABLE `osh_object_slim_task`
            ADD COLUMN `slim_mode` varchar(32) DEFAULT NULL COMMENT '瘦身模式 quality/balanced/storage_saver/low_cost' AFTER `saved_ratio`;
    END IF;
END$$

DELIMITER ;

CALL add_object_slim_mode_column();

DROP PROCEDURE IF EXISTS add_object_slim_mode_column;

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '对象存储视频瘦身开关', 'object.slim.enabled', 'false', 'N', 'system', NOW(), 'false=关闭，true=开启；关闭时 XXL-JOB handler 直接退出'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'object.slim.enabled');
