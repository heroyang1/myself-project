-- ============================================================
-- 拼团模块数据一致性校验 SQL
-- 用于运维监控：检测双表数据不一致、孤儿记录、状态异常等
-- 执行频率：建议每天凌晨低峰期执行一次
-- ============================================================

-- ============================================================
-- 1. 活动人数一致性校验
-- 对比 osh_group_activity.current_num 与 osh_group_work 实际参团人数
-- ============================================================
CREATE OR REPLACE VIEW v_group_activity_consistency AS
SELECT
    a.id AS activity_id,
    a.title,
    a.status AS activity_status,
    a.current_num AS recorded_num,
    IFNULL(w.actual_num, 0) AS actual_num,
    (a.current_num - IFNULL(w.actual_num, 0)) AS num_diff,
    CASE
        WHEN a.current_num != IFNULL(w.actual_num, 0) THEN 'MISMATCH'
        ELSE 'OK'
    END AS consistency_status
FROM osh_group_activity a
LEFT JOIN (
    SELECT group_activity_id, COUNT(*) AS actual_num
    FROM osh_group_work
    WHERE group_status IN (0, 1)
    GROUP BY group_activity_id
) w ON a.id = w.group_activity_id
WHERE a.status IN (1, 2)
ORDER BY num_diff DESC;

-- ============================================================
-- 2. 订单-参团记录一致性校验
-- 检测已支付订单但无对应参团记录，或参团记录无对应订单的情况
-- ============================================================
CREATE OR REPLACE VIEW v_group_order_consistency AS
SELECT
    o.id AS order_id,
    o.order_no,
    o.user_id,
    o.status AS order_status,
    o.price AS order_price,
    o.group_activity_id,
    o.group_work_id,
    w.id AS work_id,
    w.group_status AS work_status,
    CASE
        WHEN o.status = 'paid' AND w.id IS NULL THEN 'ORPHAN_ORDER'
        WHEN o.status = 'paid' AND w.group_status = 0 THEN 'PAID_BUT_NOT_CONFIRMED'
        ELSE 'OK'
    END AS consistency_status
FROM osh_group_order o
LEFT JOIN osh_group_work w ON o.group_work_id = w.id
WHERE o.status IN ('paid', 'success')
ORDER BY o.create_time DESC;

-- ============================================================
-- 3. 双表订单一致性校验
-- 对比 osh_group_order 与 osh_order 的状态同步情况
-- ============================================================
CREATE OR REPLACE VIEW v_group_dual_order_consistency AS
SELECT
    go.order_no,
    go.user_id,
    go.status AS group_order_status,
    oo.status AS osh_order_status,
    go.price AS group_price,
    oo.payable_amount AS osh_price,
    CASE
        WHEN oo.id IS NULL THEN 'MISSING_IN_OSH_ORDER'
        WHEN go.status = 'paid' AND oo.status != 'paid' THEN 'STATUS_MISMATCH'
        WHEN go.status = 'cancelled' AND oo.status NOT IN ('cancelled', 'refunded') THEN 'CANCEL_MISMATCH'
        ELSE 'OK'
    END AS consistency_status
FROM osh_group_order go
LEFT JOIN osh_order oo ON go.order_no = oo.order_no AND oo.delete_flag = 0
WHERE go.status IN ('paid', 'success', 'cancelled')
ORDER BY go.create_time DESC;

-- ============================================================
-- 4. 用户发起拼团一致性校验
-- 对比 osh_group_user_initiated.current_num 与 osh_group_work 实际人数
-- ============================================================
CREATE OR REPLACE VIEW v_group_initiated_consistency AS
SELECT
    ui.id AS initiated_id,
    ui.user_id AS leader_user_id,
    ui.group_status,
    ui.current_num AS recorded_num,
    ui.min_num,
    ui.max_num,
    IFNULL(w.actual_num, 0) AS actual_num,
    (ui.current_num - IFNULL(w.actual_num, 0)) AS num_diff,
    CASE
        WHEN ui.current_num != IFNULL(w.actual_num, 0) THEN 'NUM_MISMATCH'
        WHEN ui.group_status = 1 AND ui.current_num < ui.min_num THEN 'STATUS_NUM_CONFLICT'
        WHEN ui.group_status = 0 AND ui.current_num >= ui.min_num THEN 'SHOULD_BE_SUCCESS'
        ELSE 'OK'
    END AS consistency_status
FROM osh_group_user_initiated ui
LEFT JOIN (
    SELECT group_activity_id, COUNT(*) AS actual_num
    FROM osh_group_work
    WHERE group_status IN (0, 1)
    GROUP BY group_activity_id
) w ON ui.id = w.group_activity_id
WHERE ui.group_status IN (0, 1)
ORDER BY num_diff DESC;

-- ============================================================
-- 5. 孤儿参团记录检测
-- 检测 osh_group_work 中关联了不存在的活动/订单的记录
-- ============================================================
CREATE OR REPLACE VIEW v_group_orphan_works AS
SELECT
    w.id AS work_id,
    w.group_activity_id,
    w.user_id,
    w.group_status,
    w.order_id,
    CASE
        WHEN a.id IS NULL AND ui.id IS NULL THEN 'ORPHAN_NO_ACTIVITY'
        WHEN w.order_id IS NOT NULL AND o.id IS NULL THEN 'ORPHAN_NO_ORDER'
        ELSE 'OK'
    END AS consistency_status
FROM osh_group_work w
LEFT JOIN osh_group_activity a ON w.group_activity_id = a.id
LEFT JOIN osh_group_user_initiated ui ON w.group_activity_id = ui.id
LEFT JOIN osh_group_order o ON w.order_id = o.id
WHERE (a.id IS NULL AND ui.id IS NULL)
   OR (w.order_id IS NOT NULL AND o.id IS NULL);

-- ============================================================
-- 6. 快速诊断查询（运维常用）
-- ============================================================

-- 6.1 检测人数不一致的活动
-- SELECT * FROM v_group_activity_consistency WHERE consistency_status != 'OK';

-- 6.2 检测孤儿订单（已支付但无参团记录）
-- SELECT * FROM v_group_order_consistency WHERE consistency_status = 'ORPHAN_ORDER';

-- 6.3 检测双表状态不一致
-- SELECT * FROM v_group_dual_order_consistency WHERE consistency_status != 'OK';

-- 6.4 检测用户发起拼团状态异常
-- SELECT * FROM v_group_initiated_consistency WHERE consistency_status != 'OK';

-- 6.5 统计各状态汇总
-- SELECT '活动' AS type, status, COUNT(*) AS cnt FROM osh_group_activity GROUP BY status
-- UNION ALL
-- SELECT '订单' AS type, status, COUNT(*) AS cnt FROM osh_group_order GROUP BY status
-- UNION ALL
-- SELECT '参团' AS type, CAST(group_status AS CHAR), COUNT(*) AS cnt FROM osh_group_work GROUP BY group_status
-- UNION ALL
-- SELECT '发起' AS type, CAST(group_status AS CHAR), COUNT(*) AS cnt FROM osh_group_user_initiated GROUP BY group_status;
