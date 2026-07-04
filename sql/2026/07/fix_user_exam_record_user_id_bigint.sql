-- Fix: osh_user_exam_record.user_id / exam_id must be BIGINT for snowflake user IDs.
-- Symptom on QA/prod: Data truncation: Out of range value for column 'user_id' at row 1
-- when calling GET /pc/testpaper/read (insert into osh_user_exam_record).

ALTER TABLE `osh_user_exam_record`
    MODIFY COLUMN `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN `exam_id` bigint(20) NOT NULL COMMENT '考试ID';
