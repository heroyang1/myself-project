-- 汇付支付网关兼容迁移（可重复执行）。
-- 上线顺序：先执行本脚本并验证，再以 pay.provider=legacy 部署兼容 jar，最后才灰度 huifu。

SET @provider_column_ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_payment'
          AND COLUMN_NAME = 'provider'
    ),
    'SELECT 1',
    'ALTER TABLE osh_payment ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT ''legacy'' COMMENT ''支付提供方: legacy/huifu/internal'''
);
PREPARE provider_column_stmt FROM @provider_column_ddl;
EXECUTE provider_column_stmt;
DEALLOCATE PREPARE provider_column_stmt;

UPDATE osh_payment
SET provider = 'legacy'
WHERE provider IS NULL OR provider = '';

-- payment.order_id 统一指向 osh_order.id；历史拼团曾误存 osh_group_order.id。
UPDATE osh_payment payment
JOIN osh_order unified_order ON unified_order.order_no = payment.order_no
SET payment.order_id = unified_order.id
WHERE payment.order_id IS NULL OR payment.order_id <> unified_order.id;

-- 支付流水号/订单号必须由数据库保证唯一，不能只依赖“先查再插”。
-- 若历史库存在重复数据，本段会明确失败并阻止上线，先人工合并重复记录再重跑。
SET @payment_no_unique_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_payment'
          AND INDEX_NAME = 'uk_osh_payment_payment_no'
    ),
    'SELECT 1',
    'ALTER TABLE osh_payment ADD UNIQUE KEY uk_osh_payment_payment_no (payment_no)'
);
PREPARE payment_no_unique_stmt FROM @payment_no_unique_ddl;
EXECUTE payment_no_unique_stmt;
DEALLOCATE PREPARE payment_no_unique_stmt;

SET @order_no_unique_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_order'
          AND INDEX_NAME = 'uk_osh_order_order_no'
    ),
    'SELECT 1',
    'ALTER TABLE osh_order ADD UNIQUE KEY uk_osh_order_order_no (order_no)'
);
PREPARE order_no_unique_stmt FROM @order_no_unique_ddl;
EXECUTE order_no_unique_stmt;
DEALLOCATE PREPARE order_no_unique_stmt;

SET @group_order_no_unique_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_order'
          AND INDEX_NAME = 'uk_osh_group_order_order_no'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_order ADD UNIQUE KEY uk_osh_group_order_order_no (order_no)'
);
PREPARE group_order_no_unique_stmt FROM @group_order_no_unique_ddl;
EXECUTE group_order_no_unique_stmt;
DEALLOCATE PREPARE group_order_no_unique_stmt;

SET @group_work_order_unique_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_work'
          AND INDEX_NAME = 'uk_osh_group_work_order_id'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_work ADD UNIQUE KEY uk_osh_group_work_order_id (order_id)'
);
PREPARE group_work_order_unique_stmt FROM @group_work_order_unique_ddl;
EXECUTE group_work_order_unique_stmt;
DEALLOCATE PREPARE group_work_order_unique_stmt;

-- 现有超时扫描按 status/delete_flag/expire_time 查询，此索引与真实 SQL 顺序一致。
SET @payment_expire_index_ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_payment'
          AND INDEX_NAME = 'idx_osh_payment_status_expire'
    ),
    'SELECT 1',
    'CREATE INDEX idx_osh_payment_status_expire ON osh_payment (status, delete_flag, expire_time, id)'
);
PREPARE payment_expire_index_stmt FROM @payment_expire_index_ddl;
EXECUTE payment_expire_index_stmt;
DEALLOCATE PREPARE payment_expire_index_stmt;

-- 拼团来源必须显式保存，禁止再用两个表可能重复的自增 ID 猜来源。
SET @group_source_column_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_order'
          AND COLUMN_NAME = 'source_type'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_order ADD COLUMN source_type VARCHAR(20) NULL COMMENT ''拼团来源: system/initiated'''
);
PREPARE group_source_column_stmt FROM @group_source_column_ddl;
EXECUTE group_source_column_stmt;
DEALLOCATE PREPARE group_source_column_stmt;

SET @group_initiated_column_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_order'
          AND COLUMN_NAME = 'initiated_id'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_order ADD COLUMN initiated_id BIGINT NULL COMMENT ''用户发起拼团记录ID'''
);
PREPARE group_initiated_column_stmt FROM @group_initiated_column_ddl;
EXECUTE group_initiated_column_stmt;
DEALLOCATE PREPARE group_initiated_column_stmt;

-- 可确定的历史数据自动回填；无法证明来源的旧订单保留 NULL，支付前会被安全拒绝并要求重建。
UPDATE osh_group_order go
JOIN osh_group_user_initiated gui ON gui.order_id = go.id
SET go.source_type = 'initiated',
    go.initiated_id = gui.id
WHERE go.source_type IS NULL
  AND go.order_no LIKE 'GRP%';

UPDATE osh_group_order go
JOIN osh_group_work gw ON gw.order_id = go.id
LEFT JOIN osh_group_user_initiated gui
       ON gui.id = gw.group_activity_id
      AND gui.activity_id = go.group_activity_id
SET go.source_type = 'system',
    go.initiated_id = NULL
WHERE go.source_type IS NULL
  AND gw.group_activity_id = go.group_activity_id
  AND gui.id IS NULL;

UPDATE osh_group_order go
JOIN osh_group_work gw ON gw.order_id = go.id
JOIN osh_group_user_initiated gui
     ON gui.id = gw.group_activity_id
    AND gui.activity_id = go.group_activity_id
LEFT JOIN osh_group_activity ga ON ga.id = gw.group_activity_id
SET go.source_type = 'initiated',
    go.initiated_id = gui.id
WHERE go.source_type IS NULL
  AND ga.id IS NULL;

-- 每个拼团订单只能对应一条 work；按 order_id 精确修正历史 group_work_id。
UPDATE osh_group_order go
JOIN (
    SELECT order_id, MIN(id) AS work_id
    FROM osh_group_work
    WHERE order_id IS NOT NULL
    GROUP BY order_id
    HAVING COUNT(*) = 1
) gw ON gw.order_id = go.id
SET go.group_work_id = gw.work_id
WHERE go.group_work_id IS NULL OR go.group_work_id <> gw.work_id;

-- 新建用户发起拼团从 0 个已支付用户开始；修复历史表默认值 1。
SET @initiated_current_num_default_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_user_initiated'
          AND COLUMN_NAME = 'current_num'
          AND COLUMN_DEFAULT = '0'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_user_initiated MODIFY COLUMN current_num INT NOT NULL DEFAULT 0'
);
PREPARE initiated_current_num_default_stmt FROM @initiated_current_num_default_ddl;
EXECUTE initiated_current_num_default_stmt;
DEALLOCATE PREPARE initiated_current_num_default_stmt;

SET @initiated_order_unique_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_user_initiated'
          AND INDEX_NAME = 'uk_osh_group_initiated_order_id'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_user_initiated ADD UNIQUE KEY uk_osh_group_initiated_order_id (order_id)'
);
PREPARE initiated_order_unique_stmt FROM @initiated_order_unique_ddl;
EXECUTE initiated_order_unique_stmt;
DEALLOCATE PREPARE initiated_order_unique_stmt;

-- 新代码的 current_num 只统计已支付用户。仅对来源已明确、且不存在疑似旧来源订单的
-- 用户发起拼团进行校正；存在歧义的活动保留原值并由上线检查拦截。
UPDATE osh_group_user_initiated gui
LEFT JOIN (
    SELECT initiated_id, COUNT(DISTINCT user_id) AS paid_users
    FROM osh_group_order
    WHERE source_type = 'initiated'
      AND status IN ('paid', 'success')
      AND delete_flag = 0
    GROUP BY initiated_id
) paid ON paid.initiated_id = gui.id
SET gui.current_num = COALESCE(paid.paid_users, 0),
    gui.update_time = NOW()
WHERE gui.delete_flag = 0
  AND NOT EXISTS (
      SELECT 1
      FROM osh_group_order ambiguous_order
      JOIN osh_group_work ambiguous_work ON ambiguous_work.order_id = ambiguous_order.id
      WHERE ambiguous_order.source_type IS NULL
        AND ambiguous_order.delete_flag = 0
        AND ambiguous_order.status IN ('pending', 'paid', 'success')
        AND ambiguous_work.group_activity_id = gui.id
  );

-- 同样按明确的 system 来源重算系统活动已支付人数；存在来源歧义的活动保持原值，
-- 并由脚本末尾门禁查询阻止启用新支付。
UPDATE osh_group_activity ga
LEFT JOIN (
    SELECT group_activity_id, COUNT(DISTINCT user_id) AS paid_users
    FROM osh_group_order
    WHERE source_type = 'system'
      AND status IN ('paid', 'success')
      AND delete_flag = 0
    GROUP BY group_activity_id
) paid ON paid.group_activity_id = ga.id
SET ga.current_num = COALESCE(paid.paid_users, 0),
    ga.update_time = NOW()
WHERE ga.delete_flag = 0
  AND NOT EXISTS (
      SELECT 1
      FROM osh_group_order ambiguous_order
      WHERE ambiguous_order.source_type IS NULL
        AND ambiguous_order.delete_flag = 0
        AND ambiguous_order.status IN ('pending', 'paid', 'success')
        AND ambiguous_order.group_activity_id = ga.id
  );

-- 数据库兜底：同一来源拼团内，同一用户最多只能保留一笔有效订单。
SET @group_source_id_column_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_order'
          AND COLUMN_NAME = 'source_id'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_order ADD COLUMN source_id BIGINT GENERATED ALWAYS AS (CASE WHEN source_type = ''initiated'' THEN initiated_id WHEN source_type = ''system'' THEN group_activity_id ELSE NULL END) STORED'
);
PREPARE group_source_id_column_stmt FROM @group_source_id_column_ddl;
EXECUTE group_source_id_column_stmt;
DEALLOCATE PREPARE group_source_id_column_stmt;

SET @group_active_user_column_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_order'
          AND COLUMN_NAME = 'active_user_id'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_order ADD COLUMN active_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN delete_flag = 0 AND status IN (''pending'', ''paid'', ''success'') THEN user_id ELSE NULL END) STORED'
);
PREPARE group_active_user_column_stmt FROM @group_active_user_column_ddl;
EXECUTE group_active_user_column_stmt;
DEALLOCATE PREPARE group_active_user_column_stmt;

SET @group_active_member_unique_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_group_order'
          AND INDEX_NAME = 'uk_osh_group_active_member'
    ),
    'SELECT 1',
    'ALTER TABLE osh_group_order ADD UNIQUE KEY uk_osh_group_active_member (source_type, source_id, active_user_id)'
);
PREPARE group_active_member_unique_stmt FROM @group_active_member_unique_ddl;
EXECUTE group_active_member_unique_stmt;
DEALLOCATE PREPARE group_active_member_unique_stmt;

-- 支付状态与权益发放任务同事务落库。处理失败时保留任务，由定时任务幂等补发。
CREATE TABLE IF NOT EXISTS osh_order_entitlement_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    product_type INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2成功',
    attempts INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at DATETIME NULL,
    completed_time DATETIME NULL,
    last_error VARCHAR(500) NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_osh_entitlement_order_no (order_no),
    KEY idx_osh_entitlement_due (status, next_retry_time, locked_at),
    KEY idx_osh_entitlement_delete_flag (delete_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付成功权益可靠发放任务';

SET @entitlement_delete_flag_column_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_order_entitlement_task'
          AND COLUMN_NAME = 'delete_flag'
    ),
    'SELECT 1',
    'ALTER TABLE osh_order_entitlement_task ADD COLUMN delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0未删除 1已删除'' AFTER updated_time'
);
PREPARE entitlement_delete_flag_column_stmt FROM @entitlement_delete_flag_column_ddl;
EXECUTE entitlement_delete_flag_column_stmt;
DEALLOCATE PREPARE entitlement_delete_flag_column_stmt;

SET @entitlement_delete_flag_index_ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'osh_order_entitlement_task'
          AND INDEX_NAME = 'idx_osh_entitlement_delete_flag'
    ),
    'SELECT 1',
    'ALTER TABLE osh_order_entitlement_task ADD INDEX idx_osh_entitlement_delete_flag (delete_flag)'
);
PREPARE entitlement_delete_flag_index_stmt FROM @entitlement_delete_flag_index_ddl;
EXECUTE entitlement_delete_flag_index_stmt;
DEALLOCATE PREPARE entitlement_delete_flag_index_stmt;

-- 执行后验证（应返回 0）：
-- SELECT COUNT(*) FROM osh_payment WHERE provider IS NULL OR provider NOT IN ('legacy','huifu','internal');
-- 启用 task/huifu 前必须人工处理结果中的旧待支付/已支付订单：
-- SELECT id, order_no, status FROM osh_group_order WHERE source_type IS NULL AND status IN ('pending','paid','success');
-- 下面两项也必须为 0：
-- SELECT COUNT(*) FROM (SELECT payment_no FROM osh_payment GROUP BY payment_no HAVING COUNT(*) > 1) duplicate_payment;
-- SELECT COUNT(*) FROM (SELECT order_no FROM osh_order GROUP BY order_no HAVING COUNT(*) > 1) duplicate_order;
-- 历史拼团关联硬门禁（必须返回 0）：
-- SELECT COUNT(*) FROM (
--   SELECT go.id, go.group_work_id, go.source_type, go.initiated_id,
--          COUNT(gw.id) work_count, MIN(gw.id) actual_work_id
--   FROM osh_group_order go
--   LEFT JOIN osh_group_work gw ON gw.order_id = go.id
--   WHERE go.delete_flag = 0 AND go.status IN ('pending','paid','success')
--   GROUP BY go.id, go.group_work_id, go.source_type, go.initiated_id
--   HAVING work_count <> 1 OR group_work_id IS NULL OR group_work_id <> actual_work_id
--      OR source_type IS NULL OR (source_type = 'initiated' AND initiated_id IS NULL)
-- ) broken_group_order;
