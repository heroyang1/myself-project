USE `ry-vue`;
-- osh_group_activity: 补 goods_id (GroupMapper.getGroupActivityById 引用)
ALTER TABLE osh_group_activity
    ADD COLUMN goods_id BIGINT DEFAULT NULL COMMENT '商品ID' AFTER type;
SELECT 'OK' AS result;
