USE `ry-vue`;
ALTER TABLE osh_bbs_post
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（拦截器用）' AFTER is_delete;
SELECT 'OK' AS result;
