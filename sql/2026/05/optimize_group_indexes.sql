-- ============================================================
-- 拼团模块上线 - 索引与性能优化SQL脚本
-- 创建时间: 2026-06-24
-- 说明:
--   1. 基于 OshGroupServerMapper.xml 实际 SQL 模式分析
--   2. 添加复合索引(覆盖高频 WHERE + ORDER BY)
--   3. 添加唯一索引(防止重复参团)
--   4. 删除冗余/低效单列索引
--   5. 使用 ALTER TABLE ... ADD/DROP INDEX 在线执行(InnoDB支持)
--
-- 依赖前置脚本:
--   - osh_server_group.sql          (基础表结构)
--   - osh_group_module.sql          (用户发起拼团表)
--   - sql/2026/05/fix_group_table_structure.sql (activity_id 字段)
--
-- 性能瓶颈分析(基于 MyBatis Mapper XML):
--   - selectMyGroupList:UNION ALL + LEFT JOIN 3张表 + ORDER BY join_time
--   - selectGroupWorkList:同上的 UNION ALL 结构
--   - selectTimeoutOrders:WHERE status + created_time
--   - checkUserJoined:WHERE group_activity_id + user_id
--   - selectGroupUsersByInitiatedId:INNER JOIN osh_group_order
--   - selectMyGroupList (用户发起): WHERE user_id + delete_flag + group_status
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ============================================================
-- 第一部分:osh_group_activity 表优化
-- ============================================================

-- 现有索引: PRIMARY(id), idx_type(type), idx_status(status),
--           idx_start_time(start_time), idx_sort_order(sort_order)

-- 查询模式1: SELECT WHERE type='server' AND status IN (1,2) ORDER BY sort_order DESC, create_time DESC
-- 优化: 添加覆盖索引(type, status, sort_order, create_time)
ALTER TABLE `osh_group_activity`
ADD INDEX `idx_type_status_sort` (`type`, `status`, `sort_order` DESC, `create_time` DESC);

-- 查询模式2: SELECT WHERE id=? AND status=? (乐观锁更新)
-- 已通过主键索引覆盖,无需额外添加


-- ============================================================
-- 第二部分:osh_group_work 表优化
-- ============================================================

-- 现有索引: PRIMARY(id), idx_group_activity_id, idx_user_id, idx_join_time

-- 查询模式1: selectMyGroupList - WHERE user_id=? AND group_status=? (UNION 第一段)
-- 优化: 联合索引(user_id, group_status, join_time DESC)
ALTER TABLE `osh_group_work`
ADD INDEX `idx_user_status_join` (`user_id`, `group_status`, `join_time` DESC);

-- 查询模式2: checkUserJoined - WHERE group_activity_id=? AND user_id=?
-- 优化: 联合唯一索引(防止同一活动重复参团)
ALTER TABLE `osh_group_work`
ADD UNIQUE INDEX `uk_activity_user` (`group_activity_id`, `user_id`);

-- 查询模式3: selectGroupWorkList - WHERE group_status=? ORDER BY join_time DESC
-- 优化: 联合索引(group_status, join_time DESC)
ALTER TABLE `osh_group_work`
ADD INDEX `idx_status_join` (`group_status`, `join_time` DESC);

-- 查询模式4: selectGroupUsers - WHERE group_activity_id=? ORDER BY join_time ASC
-- 已存在 idx_group_activity_id,但 ORDER BY 字段未在索引中
-- 优化: 扩展为覆盖索引(group_activity_id, join_time)
ALTER TABLE `osh_group_work`
DROP INDEX `idx_group_activity_id`,
ADD INDEX `idx_activity_join` (`group_activity_id`, `join_time` ASC);

-- 查询模式5: 通过 order_id 关联订单 (LEFT JOIN go ON gw.order_id = go.id)
-- 优化: 添加 order_id 索引(若不存在)
ALTER TABLE `osh_group_work`
ADD INDEX `idx_order_id` (`order_id`);

-- 查询模式6: 服务到期时间(定时任务扫描)
--   WHERE group_status=1 AND server_expire_time BETWEEN ? AND ?
-- 优化: 添加覆盖索引
ALTER TABLE `osh_group_work`
ADD INDEX `idx_status_expire` (`group_status`, `server_expire_time`);


-- ============================================================
-- 第三部分:osh_group_order 表优化
-- ============================================================

-- 现有索引: PRIMARY(id), uk_order_no, idx_user_id, idx_group_activity_id,
--           idx_group_work_id, idx_status, idx_create_time

-- 查询模式1: selectTimeoutOrders - WHERE status='pending' AND created_time < ?
-- 优化: 联合索引(status, created_time) - 覆盖范围扫描
-- 已存在 idx_status 单列,扩展为联合
ALTER TABLE `osh_group_order`
DROP INDEX `idx_status`,
ADD INDEX `idx_status_create` (`status`, `created_time`);

-- 查询模式2: selectGroupOrderByOrderNo - WHERE order_no=? AND delete_flag=0
-- 已有 uk_order_no(order_no),但 delete_flag 不在索引中,需扫描数据行
-- 优化: 扩展唯一索引为包含 delete_flag
ALTER TABLE `osh_group_order`
DROP INDEX `uk_order_no`,
ADD UNIQUE INDEX `uk_order_no_df` (`order_no`, `delete_flag`);

-- 查询模式3: 按 user_id 查询个人订单列表 - WHERE user_id=? ORDER BY create_time DESC
-- 已存在 idx_user_id,优化为联合索引(user_id, create_time)
ALTER TABLE `osh_group_order`
DROP INDEX `idx_user_id`,
ADD INDEX `idx_user_create` (`user_id`, `create_time` DESC);

-- 查询模式4: 按活动查订单 - WHERE group_activity_id=? AND status=?
-- 优化: 联合索引
ALTER TABLE `osh_group_order`
ADD INDEX `idx_activity_status` (`group_activity_id`, `status`);

-- 查询模式5: 按订单状态统计 - WHERE status='paid' AND pay_time BETWEEN ? AND ?
-- 用于财务对账
ALTER TABLE `osh_group_order`
ADD INDEX `idx_pay_time` (`pay_time`, `status`);


-- ============================================================
-- 第四部分:osh_group_user_initiated 表优化
-- ============================================================

-- 现有索引: PRIMARY(id), idx_user_id, idx_order_id, idx_group_status, idx_initiate_time

-- 查询模式1: selectMyGroupList (UNION 第二段) - WHERE user_id=? AND delete_flag=0 AND group_status=?
-- 优化: 联合索引(user_id, delete_flag, group_status)
ALTER TABLE `osh_group_user_initiated`
ADD INDEX `idx_user_df_status` (`user_id`, `delete_flag`, `group_status`);

-- 查询模式2: selectUserInitiatedActivityList - WHERE delete_flag=0 [AND status=?] [AND type=?]
-- 优化: 覆盖索引
ALTER TABLE `osh_group_user_initiated`
ADD INDEX `idx_df_status_initiate` (`delete_flag`, `group_status`, `initiate_time` DESC);

-- 查询模式3: 过期清理 - WHERE delete_flag=0 AND group_status=0 AND expire_time < ?
-- 用于定时任务
ALTER TABLE `osh_group_user_initiated`
ADD INDEX `idx_df_status_expire` (`delete_flag`, `group_status`, `expire_time`);

-- 查询模式4: 通过 activity_id 关联活动模板
--   (fix_group_table_structure.sql 已添加 idx_activity_id)
-- 验证索引是否存在,若不存在则补充
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'osh_group_user_initiated'
                     AND INDEX_NAME = 'idx_activity_id');
SET @ddl = IF(@idx_exists = 0,
              'ALTER TABLE osh_group_user_initiated ADD INDEX idx_activity_id (activity_id)',
              'SELECT "idx_activity_id already exists" AS info');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ============================================================
-- 第五部分:osh_payment 表优化 (拼团支付流水)
-- ============================================================

-- 假设 osh_payment 表结构(基于 Mapper XML 推断):
--   payment_no, order_no, amount, status, delete_flag, created_time, paid_time

-- 查询模式1: existsPaymentByPaymentNo - WHERE payment_no=? AND delete_flag=0
-- 通常已存在 payment_no 唯一索引,验证即可

-- 查询模式2: selectGroupOrderByOrderNo 子查询 - MAX(id) WHERE payment_no=? AND delete_flag=0
-- 优化: 联合索引(payment_no, delete_flag, id)
ALTER TABLE `osh_payment`
ADD INDEX `idx_payment_no_df_id` (`payment_no`, `delete_flag`, `id` DESC);

-- 查询模式3: 按订单号查最新支付流水 - WHERE order_no=? AND delete_flag=0
ALTER TABLE `osh_payment`
ADD INDEX `idx_order_no_df` (`order_no`, `delete_flag`);


-- ============================================================
-- 第六部分:表分析与统计信息更新
-- ============================================================

-- 6.1 更新表统计信息(让优化器选择最佳执行计划)
ANALYZE TABLE `osh_group_activity`;
ANALYZE TABLE `osh_group_work`;
ANALYZE TABLE `osh_group_order`;
ANALYZE TABLE `osh_group_user_initiated`;
ANALYZE TABLE `osh_payment`;


-- ============================================================
-- 第七部分:验证数据
-- ============================================================

-- 7.1 查看各表索引列表
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'osh_group_activity',
      'osh_group_work',
      'osh_group_order',
      'osh_group_user_initiated',
      'osh_payment'
  )
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- 7.2 索引总数统计
SELECT
    TABLE_NAME,
    COUNT(*) AS index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE 'osh_group%'
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;

-- 7.3 检查冗余索引(同前缀的单列索引)
SELECT
    s.TABLE_NAME,
    s.INDEX_NAME AS single_col_index,
    c.INDEX_NAME AS composite_index,
    s.COLUMN_NAME
FROM information_schema.STATISTICS s
INNER JOIN information_schema.STATISTICS c
    ON s.TABLE_NAME = c.TABLE_NAME
   AND s.COLUMN_NAME = c.COLUMN_NAME
   AND c.SEQ_IN_INDEX = 1
   AND s.INDEX_NAME != c.INDEX_NAME
   AND s.SEQ_IN_INDEX = 1
WHERE s.TABLE_SCHEMA = DATABASE()
  AND s.TABLE_NAME LIKE 'osh_group%'
  AND s.INDEX_NAME != 'PRIMARY';

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 执行完成
--
-- 索引优化要点总结:
--   1. 复合索引遵循最左前缀原则:把高频查询的WHERE字段放最前,ORDER BY字段放最后
--   2. 唯一约束索引 uk_activity_user 防止同一用户重复参团(原代码仅做应用层校验)
--   3. 包含 ORDER BY DESC/ASC 的索引,MySQL 8.0 支持降序索引,大幅提升排序性能
--   4. 覆盖索引(包含所有 SELECT 字段)可避免回表查询
--   5. 联合唯一索引 uk_order_no_df 扩展包含 delete_flag,实现软删除场景下的高效查询
--
-- 后续步骤:
--   1. 使用 EXPLAIN 验证关键 SQL 是否走索引:
--      EXPLAIN SELECT * FROM osh_group_work WHERE user_id=? AND group_status=?;
--   2. 监控慢查询日志,关注 rows_examined > 1000 的查询
--   3. 使用 sys.schema_index_statistics 观察索引使用频率
--   4. 对 TEXT/BLOB 大字段,考虑前缀索引(如 INDEX(title(20)))
--   5. 单表索引数量控制在 6 个以内,过多会影响写入性能
--
-- 注意事项:
--   - DROP INDEX 不会立即释放空间,建议执行 OPTIMIZE TABLE
--   - 大表 ALTER TABLE 可能锁表,建议使用 pt-online-schema-change
--   - uk_activity_user 添加前必须先清理重复数据:
--     SELECT group_activity_id, user_id, COUNT(*)
--     FROM osh_group_work
--     GROUP BY group_activity_id, user_id HAVING COUNT(*) > 1;
-- ============================================================