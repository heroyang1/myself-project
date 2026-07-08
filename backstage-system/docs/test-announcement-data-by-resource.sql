-- 首页公告栏按 resource_type 分组测试数据
-- 适用于验证“每个资源类型在 /notice 和 /dynamic 下各取 5 条”的当前逻辑

DELETE FROM osh_announcement
WHERE source_module = 'homepage'
  AND create_by = 'doc_resource_test';

INSERT INTO osh_announcement
    (title, link, icon_code, channel, resource_type, resource_id, sort, status,
     start_time, end_time, source, source_module, delete_flag,
     create_by, create_time, update_by, update_time)
VALUES
    ('课程系统通知 1', '/course/detail/1001', 'course_notice', 1, 'course', 1001, 100, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('课程业务动态 1', '/course/detail/1002', 'course_hot', 2, 'course', 1002, 99, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('电子书系统通知 1', '/book/detail/2001', 'book_notice', 1, 'book', 2001, 98, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('电子书业务动态 1', '/book/detail/2002', 'book_hot', 2, 'book', 2002, 97, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('考试系统通知 1', '/exam/detail/3001', 'exam_notice', 1, 'exam', 3001, 96, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('考试业务动态 1', '/exam/detail/3002', 'exam_hot', 2, 'exam', 3002, 95, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('问答系统通知 1', '/qa/detail/4001', 'qa_notice', 1, 'qa', 4001, 94, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('问答业务动态 1', '/qa/detail/4002', 'qa_hot', 2, 'qa', 4002, 93, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('秒杀系统通知 1', '/seckill/detail/5001', 'seckill_notice', 1, 'seckill', 5001, 92, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('秒杀业务动态 1', '/seckill/detail/5002', 'seckill_hot', 2, 'seckill', 5002, 91, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('拼团系统通知 1', '/group/detail/6001', 'group_notice', 1, 'group', 6001, 90, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('拼团业务动态 1', '/group/detail/6002', 'group_hot', 2, 'group', 6002, 89, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('信息差系统通知 1', '/info-gap/detail/7001', 'info_gap_notice', 1, 'info_gap', 7001, 88, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('信息差业务动态 1', '/info-gap/detail/7002', 'info_gap_hot', 2, 'info_gap', 7002, 87, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('开源项目系统通知 1', '/open-project/detail/8001', 'open_project_notice', 1, 'open_project', 8001, 86, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('开源项目业务动态 1', '/open-project/detail/8002', 'open_project_hot', 2, 'open_project', 8002, 85, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('网站系统通知 1', '/website/detail/9001', 'website_notice', 1, 'website', 9001, 84, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('网站业务动态 1', '/website/detail/9002', 'website_hot', 2, 'website', 9002, 83, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('反馈系统通知 1', '/feedback/detail/10001', 'feedback_notice', 1, 'feedback', 10001, 82, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('反馈业务动态 1', '/feedback/detail/10002', 'feedback_hot', 2, 'feedback', 10002, 81, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('工具系统通知 1', '/tool/detail/11001', 'tool_notice', 1, 'tool', 11001, 80, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('工具业务动态 1', '/tool/detail/11002', 'tool_hot', 2, 'tool', 11002, 79, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('审核系统通知 1', '/audit/detail/12001', 'audit_notice', 1, 'audit', 12001, 78, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('审核业务动态 1', '/audit/detail/12002', 'audit_hot', 2, 'audit', 12002, 77, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('订单系统通知 1', '/order/detail/13001', 'order_notice', 1, 'order', 13001, 76, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW()),
    ('订单业务动态 1', '/order/detail/13002', 'order_hot', 2, 'order', 13002, 75, 4, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'manual', 'homepage', 0, 'doc_resource_test', NOW(), 'doc_resource_test', NOW());

-- 验证每个 resource_type 都有 channel=1 / channel=2 数据
SELECT
    resource_type,
    channel,
    COUNT(*) AS row_count
FROM osh_announcement
WHERE source_module = 'homepage'
  AND create_by = 'doc_resource_test'
GROUP BY resource_type, channel
ORDER BY resource_type, channel;
