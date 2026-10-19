USE `ry-vue`;
SELECT '=== osh_book_chapter columns ===' AS info;
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_book_chapter' ORDER BY ORDINAL_POSITION;

SELECT '=== osh_announcement columns ===' AS info;
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_announcement' ORDER BY ORDINAL_POSITION;

SELECT '=== osh_bbs_category columns ===' AS info;
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_bbs_category' ORDER BY ORDINAL_POSITION;

SELECT '=== osh_seckill_activity_item columns ===' AS info;
SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='osh_seckill_activity_item' ORDER BY ORDINAL_POSITION;

SELECT '=== assistant_feedback_tag exists? ===' AS info;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='assistant_feedback_tag';

SELECT '=== assistant_feedback_category exists? ===' AS info;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='assistant_feedback_category';

SELECT '=== assistant_feedback exists? ===' AS info;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_NAME='assistant_feedback';
