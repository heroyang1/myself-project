-- ============================================================
-- 拼团模块上线 - 定时任务配置SQL脚本
-- 创建时间: 2026-06-24
-- 说明:
--   1. 基于 RuoYi Quartz 框架(sys_job + QRTZ_* 表)
--   2. 使用 invoke_target 字符串调用 Spring Bean 方法
--   3. 拼团模块核心业务方法需要先在 Service 中实现(详见注释)
--   4. 执行本 SQL 后,任务初始为暂停状态(status=1),需在管理后台手动启用
--
-- 依赖前置脚本:
--   - init_group_base_data.sql
--   - quartz.sql (Quartz框架表)
--   - ry_20250522.sql (sys_job表)
--
-- 拼团模块需要新增的 Service 方法(后续开发):
--   - IOshGroupServerService.cancelExpiredOrders()        取消超时订单
--   - IOshGroupServerService.expireInitiatedGroups()       关闭过期用户发起
--   - IOshGroupServerService.autoUpdateActivityStatus()    自动更新活动状态
--   - IOshGroupServerService.notifyExpiringServers()       到期前提醒
--   - IOshGroupServerService.refreshActivityCurrentNum()   同步参与人数
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ==================== 第一步:拼团模块业务定时任务 ====================
-- job_id 从 100 开始,避免与系统任务(1,2,3)冲突

-- 1.1 订单超时自动取消任务
-- cron: 每5分钟执行一次,扫描 pending 状态订单,超过 timeout_minutes 分钟自动取消
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团订单超时自动取消',
    'DEFAULT',
    'groupTask.cancelExpiredOrders',
    '0 */5 * * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每5分钟扫描一次,取消超过osh.group.order.timeout_minutes分钟未支付的pending订单'
);

-- 1.2 用户发起拼团过期清理任务
-- cron: 每天凌晨2点执行一次
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '用户发起拼团过期清理',
    'DEFAULT',
    'groupTask.expireInitiatedGroups',
    '0 0 2 * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每天凌晨2点执行,关闭超过osh.group.initiate.expire_days天的用户发起拼团(status=2)'
);

-- 1.3 拼团活动状态自动更新任务
-- cron: 每天凌晨2点30分执行一次
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团活动状态自动更新',
    'DEFAULT',
    'groupTask.autoUpdateActivityStatus',
    '0 30 2 * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每天凌晨2:30执行,根据参团人数和时间自动更新活动状态(1进行中/2拼团成功/3已结束)'
);

-- 1.4 同步活动当前参与人数任务
-- cron: 每10分钟执行一次,同步 osh_group_work → osh_group_activity.current_num
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '同步拼团活动参与人数',
    'DEFAULT',
    'groupTask.refreshActivityCurrentNum',
    '0 */10 * * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每10分钟执行一次,基于osh_group_work表刷新osh_group_activity的current_num字段'
);

-- 1.5 服务器到期前提醒任务
-- cron: 每天上午9点执行一次,提醒即将到期的用户
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '服务器到期前提醒',
    'DEFAULT',
    'groupTask.notifyExpiringServers',
    '0 0 9 * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每天9点执行,根据osh.group.expire.notify_days配置,在到期前N天通过站内信/短信提醒用户'
);

-- 1.6 拼团订单超时关团清理任务
-- cron: 每天凌晨3点执行,清理已成团但部分订单过期的组团
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团订单超时关团清理',
    'DEFAULT',
    'groupTask.cleanupExpiredGroups',
    '0 0 3 * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每天3点执行,清理长期未成团且过期的组团数据,释放服务器资源'
);

-- 1.7 拼团统计数据快照(可选,用于报表)
-- cron: 每天凌晨4点执行一次,生成统计数据
INSERT INTO `sys_job`(
    `job_name`, `job_group`, `invoke_target`, `cron_expression`,
    `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团每日数据快照',
    'DEFAULT',
    'groupTask.dailySnapshot',
    '0 0 4 * * ?',
    '3', '1', '1', 'admin', NOW(), '', NULL,
    '每天4点执行,生成前一天拼团数据快照(订单数/成交额/成团率等),供BI报表使用'
);

-- 注:产品确认当前阶段不实现系统自动退款,由用户线下联系客服处理,
--     故未配置支付超时退款监控任务。如后续需要,可参考历史版本补充 groupTask.refundTimeoutPaidOrders。


-- ==================== 第二步:创建对应的 Spring Bean ====================
-- 后续开发:在 backstage-system 模块下创建 GroupTask 定时任务 Bean
--
-- @Component("groupTask")
-- public class GroupTask {
--     @Resource
--     private IOshGroupServerService groupServerService;
--
--     public void cancelExpiredOrders() {
--         int count = groupServerService.cancelExpiredOrders();
--         log.info("[拼团定时任务] 取消超时订单: {} 条", count);
--     }
--
--     public void expireInitiatedGroups() {
--         int count = groupServerService.expireInitiatedGroups();
--         log.info("[拼团定时任务] 过期用户发起: {} 条", count);
--     }
--
--     public void autoUpdateActivityStatus() {
--         int count = groupServerService.autoUpdateActivityStatus();
--         log.info("[拼团定时任务] 自动更新活动状态: {} 条", count);
--     }
--
--     public void refreshActivityCurrentNum() {
--         int count = groupServerService.refreshActivityCurrentNum();
--         log.info("[拼团定时任务] 刷新活动人数: {} 条", count);
--     }
--
--     public void notifyExpiringServers() {
--         int count = groupServerService.notifyExpiringServers();
--         log.info("[拼团定时任务] 到期提醒: {} 条", count);
--     }
--
--     public void cleanupExpiredGroups() {
--         int count = groupServerService.cleanupExpiredGroups();
--         log.info("[拼团定时任务] 清理过期组团: {} 条", count);
--     }
--
--     public void dailySnapshot() {
--         groupServerService.dailySnapshot();
--         log.info("[拼团定时任务] 每日数据快照完成");
--     }
-- }


-- ==================== 第三步:验证数据 ====================

-- 3.1 查看所有拼团定时任务
SELECT job_id, job_name, job_group, invoke_target, cron_expression, status, remark
FROM sys_job
WHERE invoke_target LIKE 'groupTask.%'
ORDER BY job_id;

-- 3.2 统计任务分布
SELECT
    CASE status WHEN '0' THEN '正常' WHEN '1' THEN '暂停' END AS status_name,
    COUNT(*) AS count
FROM sys_job
WHERE invoke_target LIKE 'groupTask.%'
GROUP BY status;

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 执行完成
-- 后续步骤:
--   1. 在 backstage-system 模块创建 GroupTask 任务 Bean(参考上方代码模板)
--   2. 在 IOshGroupServerService 接口中新增对应的业务方法
--   3. 登录管理后台 → 系统监控 → 定时任务 → 启用本脚本配置的任务
--   4. 通过 sys_job_log 表观察任务执行情况
--   5. 建议开启 Quartz 任务执行失败告警通知(邮件/钉钉)
--
-- 注意事项:
--   - 本脚本默认将任务设置为暂停(status=1),避免上线时未开发完的Bean导致报错
--   - 请按业务优先级分批启用:先启用 cancelExpiredOrders(订单超时取消)
--   - cron 表达式均为标准 7 位表达式,与 Quartz 一致
--   - misfire_policy=3 表示"放弃执行",适合大多数定时任务
--   - concurrent=1 表示禁止并发执行(默认)
--   - 时区问题:Quartz 默认使用系统时区,生产环境务必确认服务器时区为 Asia/Shanghai
-- ============================================================