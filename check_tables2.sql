USE `ry-vue`;
SELECT '=== Check all osh tables ===' AS info;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME LIKE 'osh_%' ORDER BY TABLE_NAME;

SELECT '=== osh_course columns ===' AS info;
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_course' ORDER BY ORDINAL_POSITION;

SELECT '=== osh_column columns ===' AS info;
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_column' ORDER BY ORDINAL_POSITION;

SELECT '=== osh_course_buy exists? ===' AS info;
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_course_buy';

SELECT '=== osh_course_collection exists? ===' AS info;
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_course_collection';

SELECT '=== osh_open_project_tech_component exists? ===' AS info;
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_open_project_tech_component';
