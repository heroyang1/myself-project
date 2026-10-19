USE `ry-vue`;

-- osh_book: 补 delete_flag + level
ALTER TABLE osh_book
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）',
    ADD COLUMN level TINYINT DEFAULT NULL COMMENT '图书等级' AFTER sub_count;

-- osh_bbs_post: 补 is_delete
ALTER TABLE osh_bbs_post
    ADD COLUMN is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）';

SELECT 'All applied OK' AS result;
