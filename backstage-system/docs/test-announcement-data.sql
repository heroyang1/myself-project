-- 首页公告栏测试数据（当前版本）
-- 用途：验证 GET /notice、GET /dynamic 以及 WebSocket 刷新事件联动
-- 当前首页逻辑：
-- 1. /notice 只查 channel=1
-- 2. /dynamic 只查 channel=2
-- 3. 每个 resource_type 分组后各取最近 5 条
-- 4. WebSocket 不推完整数据，只推刷新事件

DELETE FROM osh_announcement
WHERE source_module = 'homepage'
  AND create_by = 'doc_test';

-- 系统通知（channel=1）
INSERT INTO osh_announcement
    (title, link, icon_code, channel, resource_type, resource_id, sort, status,
     start_time, end_time, source, source_module, delete_flag,
     create_by, create_time, update_by, update_time)
VALUES
    ('系统通知：课程专区已完成本周更新', '/course/detail/101', 'course_notice', 1, 'course', 101, 100, 4,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('系统通知：电子书专区新增推荐书单', '/book/detail/201', 'book_notice', 1, 'book', 201, 96, 4,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('系统通知：问答专区规则已更新', '/qa/detail/301', 'qa_notice', 1, 'qa', 301, 92, 2,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('系统通知：网站推荐榜单已刷新', '/website/detail/401', 'website_notice', 1, 'website', 401, 88, 0,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('系统通知：订单中心公告测试数据', '/order/detail/501', 'order_notice', 1, 'order', 501, 82, 4,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW());

-- 业务动态（channel=2）
INSERT INTO osh_announcement
    (title, link, icon_code, channel, resource_type, resource_id, sort, status,
     start_time, end_time, source, source_module, delete_flag,
     create_by, create_time, update_by, update_time)
VALUES
    ('业务动态：秒杀专区本周新增活动', '/seckill/detail/601', 'seckill_hot', 2, 'seckill', 601, 100, 4,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('业务动态：拼团专区热度持续上升', '/group/detail/701', 'group_hot', 2, 'group', 701, 95, 4,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('业务动态：信息差栏目新增热门推荐', '/info-gap/detail/801', 'info_gap_hot', 2, 'info_gap', 801, 90, 4,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('业务动态：工具栏新增图片处理能力', '/tool/detail/901', 'tool_new', 2, 'tool', 901, 85, 2,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('业务动态：反馈中心处理效率提升', '/feedback/detail/1001', 'feedback_hot', 2, 'feedback', 1001, 80, 0,
     NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW());

-- 过期数据，不应出现在接口结果中
INSERT INTO osh_announcement
    (title, link, icon_code, channel, resource_type, resource_id, sort, status,
     start_time, end_time, source, source_module, delete_flag,
     create_by, create_time, update_by, update_time)
VALUES
    ('过期测试数据：不应被 notice 返回', '/course/detail/9991', 'expired_notice', 1, 'course', 9991, 999, 4,
     DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW()),
    ('未来生效测试数据：不应被 dynamic 返回', '/group/detail/9992', 'future_notice', 2, 'group', 9992, 999, 4,
     DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 'manual', 'homepage', 0,
     'doc_test', NOW(), 'doc_test', NOW());

-- 验证 SQL
SELECT id, title, channel, resource_type, sort, status, start_time, end_time
FROM osh_announcement
WHERE source_module = 'homepage'
  AND create_by = 'doc_test'
ORDER BY channel, resource_type, sort DESC, IFNULL(update_time, create_time) DESC, id DESC;

-- 当前接口核心过滤条件
-- WHERE delete_flag = 0
--   AND status IN (0, 2, 4)
--   AND channel = #{channel}
--   AND resource_type = #{resourceType}
--   AND (start_time IS NULL OR start_time <= NOW())
--   AND (end_time IS NULL OR end_time >= NOW())
-- ORDER BY sort DESC, IFNULL(update_time, create_time) DESC, create_time DESC, id DESC
-- LIMIT 5;
