-- ============================================================
-- 为拼团相关 4 张表统一添加 delete_flag 字段
-- 原因：GlobalLogicDeleteInterceptor 会自动追加 delete_flag = 0 条件，
--       但建表 DDL 中遗漏了此字段，导致 Unknown column 错误。
-- 注：osh_group_notification 表未实际创建，已移除。
-- ============================================================

-- 1. osh_group_activity
ALTER TABLE `osh_group_activity`
    ADD COLUMN `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除' AFTER `updated_time`;
ALTER TABLE `osh_group_activity` ADD INDEX `idx_delete_flag` (`delete_flag`);

-- 2. osh_group_order
ALTER TABLE `osh_group_order`
    ADD COLUMN `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除' AFTER `updated_time`;
ALTER TABLE `osh_group_order` ADD INDEX `idx_delete_flag` (`delete_flag`);

-- 3. osh_group_work
ALTER TABLE `osh_group_work`
    ADD COLUMN `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除' AFTER `created_time`;
ALTER TABLE `osh_group_work` ADD INDEX `idx_delete_flag` (`delete_flag`);

-- 4. osh_group_user
ALTER TABLE `osh_group_user`
    ADD COLUMN `delete_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除' AFTER `status`;
ALTER TABLE `osh_group_user` ADD INDEX `idx_delete_flag` (`delete_flag`);

-- ============================================================
-- 将 4 张表的现有记录 delete_flag 统一设为 0（正常状态）
-- 注：ADD COLUMN DEFAULT 0 已自动处理，以下为显式确认
-- ============================================================
UPDATE `osh_group_activity` SET `delete_flag` = 0 WHERE `delete_flag` IS NULL;
UPDATE `osh_group_order`    SET `delete_flag` = 0 WHERE `delete_flag` IS NULL;
UPDATE `osh_group_work`     SET `delete_flag` = 0 WHERE `delete_flag` IS NULL;
UPDATE `osh_group_user`     SET `delete_flag` = 0 WHERE `delete_flag` IS NULL;
