USE `ry-vue`;

-- osh_course_section: 补 delete_flag + title
ALTER TABLE osh_course_section
    ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志（0-未删 1-已删）',
    ADD COLUMN title VARCHAR(255) DEFAULT '' COMMENT '章节标题';

SELECT 'OK' AS result;
