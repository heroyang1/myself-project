USE `ry-vue`;
SELECT '=== Check tables ===' AS info;
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME IN ('osh_course_tag_rel','osh_course_tag','osh_group_work','osh_user_action_event','osh_open_project_tag','osh_announcement');

SELECT '=== osh_group_work columns ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_group_work';

SELECT '=== osh_user_action_event columns ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_user_action_event';

SELECT '=== osh_open_project_tag columns ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_open_project_tag';
