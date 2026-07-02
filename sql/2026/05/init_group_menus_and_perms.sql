-- ============================================================
-- 拼团模块上线 - 菜单与权限配置SQL脚本
-- 创建时间: 2026-06-24
-- 说明:
--   1. 使用 RuoYi 标准 sys_menu + sys_role_menu 权限模型
--   2. 创建拼团管理顶级目录(菜单ID = 2030)
--   3. 创建二级菜单: 拼团活动、组团记录、订单管理 (C菜单)
--   4. 创建按钮权限: 查询/新增/修改/删除/导出/支付/退款 (F按钮)
--   5. 关联到超级管理员(role_id=1)和管理员(role_id=2)
--   6. 同时给所有已存在角色(role_id=1~7)分配只读权限
--
-- 依赖前置脚本:
--   - init_group_base_data.sql   (字典+配置+活动模板)
--   - ry_20250522.sql            (sys_menu/sys_role_menu表结构)
--
-- 执行顺序:
--   1. sys_menu 插入新菜单记录
--   2. sys_role_menu 关联角色
--   3. 验证数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ==================== 第一步:创建拼团管理顶级菜单(目录) ====================
-- menu_id = 2030, 与现有模块(系统管理=1, 系统监控=2, 系统工具=3)并列

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团管理', 0, 7, 'group', NULL,
    1, 0, 'M', '0', '0',
    '', 'peoples', 'admin', NOW(), '', NULL, '拼团模块管理目录'
);

-- 保存拼团目录菜单ID,供后续子菜单使用
SET @groupMenuId = LAST_INSERT_ID();


-- ==================== 第二步:创建二级菜单(C类型菜单) ====================

-- 2.1 拼团活动管理 (menu_id = 2031)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团活动', @groupMenuId, 1, 'activity', 'servicegroup/activity/index',
    1, 0, 'C', '0', '0',
    'group:activity:list', 'list', 'admin', NOW(), '', NULL, '拼团活动模板管理'
);
SET @activityMenuId = LAST_INSERT_ID();

-- 2.2 组团记录管理 (menu_id = 2032)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '组团记录', @groupMenuId, 2, 'work', 'servicegroup/work/index',
    1, 0, 'C', '0', '0',
    'group:work:list', 'documentation', 'admin', NOW(), '', NULL, '组团记录管理'
);
SET @workMenuId = LAST_INSERT_ID();

-- 2.3 拼团订单管理 (menu_id = 2033)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '拼团订单', @groupMenuId, 3, 'order', 'servicegroup/order/index',
    1, 0, 'C', '0', '0',
    'group:order:list', 'money', 'admin', NOW(), '', NULL, '拼团订单管理'
);
SET @orderMenuId = LAST_INSERT_ID();

-- 2.4 用户发起拼团管理 (menu_id = 2034)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '用户发起', @groupMenuId, 4, 'initiated', 'servicegroup/initiated/index',
    1, 0, 'C', '0', '0',
    'group:initiated:list', 'user', 'admin', NOW(), '', NULL, '用户发起的拼团管理'
);
SET @initiatedMenuId = LAST_INSERT_ID();


-- ==================== 第三步:创建按钮权限(F类型) ====================

-- 3.1 拼团活动按钮 (parent_id = @activityMenuId)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('活动查询',     @activityMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'group:activity:query',  '#', 'admin', NOW(), '', NULL, '拼团活动查询按钮'),
('活动新增',     @activityMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'group:activity:add',    '#', 'admin', NOW(), '', NULL, '拼团活动新增按钮'),
('活动修改',     @activityMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'group:activity:edit',   '#', 'admin', NOW(), '', NULL, '拼团活动修改按钮'),
('活动删除',     @activityMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'group:activity:remove', '#', 'admin', NOW(), '', NULL, '拼团活动删除按钮'),
('活动导出',     @activityMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'group:activity:export', '#', 'admin', NOW(), '', NULL, '拼团活动导出按钮'),
('上下架活动',   @activityMenuId, 6, '#', '', 1, 0, 'F', '0', '0', 'group:activity:status', '#', 'admin', NOW(), '', NULL, '拼团活动上下架按钮');

-- 3.2 组团记录按钮 (parent_id = @workMenuId)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('组团查询',     @workMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'group:work:query',  '#', 'admin', NOW(), '', NULL, '组团记录查询按钮'),
('发起拼团',     @workMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'group:work:create', '#', 'admin', NOW(), '', NULL, '管理员发起拼团按钮'),
('强制成团',     @workMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'group:work:force',  '#', 'admin', NOW(), '', NULL, '强制成团按钮'),
('组团导出',     @workMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'group:work:export', '#', 'admin', NOW(), '', NULL, '组团记录导出按钮'),
('手动添加用户', @workMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'group:user:add',    '#', 'admin', NOW(), '', NULL, '手动添加用户到拼团按钮');

-- 3.3 拼团订单按钮 (parent_id = @orderMenuId)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('订单查询',     @orderMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'group:order:query',    '#', 'admin', NOW(), '', NULL, '订单查询按钮'),
('订单导出',     @orderMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'group:order:export',   '#', 'admin', NOW(), '', NULL, '订单导出按钮'),
('订单退款',     @orderMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'group:order:refund',   '#', 'admin', NOW(), '', NULL, '订单退款按钮'),
('关闭订单',     @orderMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'group:order:close',    '#', 'admin', NOW(), '', NULL, '强制关闭订单按钮'),
('补发凭证',     @orderMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'group:order:resend',   '#', 'admin', NOW(), '', NULL, '补发服务器凭证按钮');

-- 3.4 用户发起拼团按钮 (parent_id = @initiatedMenuId)
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`,
    `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('发起记录查询', @initiatedMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'group:initiated:query',  '#', 'admin', NOW(), '', NULL, '发起记录查询按钮'),
('发起记录导出', @initiatedMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'group:initiated:export', '#', 'admin', NOW(), '', NULL, '发起记录导出按钮'),
('强制关闭发起', @initiatedMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'group:initiated:close',  '#', 'admin', NOW(), '', NULL, '强制关闭发起拼团按钮');


-- ==================== 第四步:关联角色权限 sys_role_menu ====================
-- 关联规则:
--   role_id=1 (超级管理员) → 全部菜单和按钮
--   role_id=2 (管理员) → 除退款外的所有菜单和按钮
--   role_id=3,4,5 (其他角色) → 仅只读权限(查询类)

-- 4.1 为超级管理员(role_id=1)授权所有菜单和按钮
-- 这里使用 IN 子查询 + path LIKE 模式批量授权
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT 1, menu_id
FROM sys_menu
WHERE path = 'group'
   OR path IN ('activity', 'work', 'order', 'initiated') AND parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '拼团管理' AND menu_type = 'M' LIMIT 1)
   OR (menu_type = 'F' AND perms LIKE 'group:%');

-- 4.2 为管理员(role_id=2)授权(排除退款权限group:order:refund)
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT 2, m.menu_id
FROM sys_menu m
WHERE (m.path = 'group'
    OR (m.path IN ('activity', 'work', 'order', 'initiated')
        AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '拼团管理' AND menu_type = 'M' LIMIT 1))
    OR (m.menu_type = 'F' AND m.perms LIKE 'group:%'))
  AND m.perms NOT IN ('group:order:refund', 'group:work:force', 'group:initiated:close');

-- 4.3 为其他角色(role_id=3,4,5,6,7)授权只读权限(仅查询按钮)
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM (SELECT 3 AS role_id UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) r
CROSS JOIN sys_menu m
WHERE (m.path = 'group'
    OR (m.path IN ('activity', 'work', 'order', 'initiated')
        AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '拼团管理' AND menu_type = 'M' LIMIT 1))
    OR m.perms IN ('group:activity:query', 'group:work:query', 'group:order:query', 'group:initiated:query'));

-- 4.4 为 C 端用户赋予登录后可访问的接口(便于 H5 调用)
-- 注: C端用户访问 /pc/group/* 走 Token 校验,无需 sys_menu 控制
-- 这里只为匿名接口做特殊配置:允许未登录访问
-- (注: 该配置在 Shiro/JWT 配置中实现,不在此 SQL 范围内)


-- ==================== 第五步:验证数据 ====================

-- 5.1 检查菜单树
SELECT menu_id, parent_id, menu_name, menu_type, perms, order_num
FROM sys_menu
WHERE menu_name = '拼团管理'
   OR (parent_id IN (SELECT menu_id FROM sys_menu WHERE menu_name = '拼团管理' AND menu_type = 'M'))
   OR perms LIKE 'group:%'
ORDER BY parent_id, order_num;

-- 5.2 检查角色权限关联
SELECT rm.role_id, COUNT(*) AS menu_count
FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms LIKE 'group:%' OR m.menu_name = '拼团管理'
GROUP BY rm.role_id
ORDER BY rm.role_id;

-- 5.3 检查按钮权限数量
SELECT
    (SELECT COUNT(*) FROM sys_menu WHERE menu_name = '拼团管理' AND menu_type = 'M') AS catalog_count,
    (SELECT COUNT(*) FROM sys_menu WHERE parent_id IN (SELECT menu_id FROM sys_menu WHERE menu_name = '拼团管理' AND menu_type = 'M') AND menu_type = 'C') AS menu_count,
    (SELECT COUNT(*) FROM sys_menu WHERE menu_type = 'F' AND perms LIKE 'group:%') AS button_count;

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 执行完成
-- 后续步骤:
--   1. 执行 init_group_quartz_jobs.sql 配置定时任务
--   2. 执行 optimize_group_indexes.sql 添加索引
--   3. 在管理后台登录 admin 账号,验证菜单是否正常显示
--   4. 在管理后台创建自定义按钮/页面时引用 perms 字段(如 group:activity:list)
--   5. 前端代码中通过 v-hasPermi="['group:activity:edit']" 控制按钮显示
--
-- 注意事项:
--   - menu_id 起始 2030,避免与现有菜单冲突
--   - 按钮 perms 与 Controller 的 @PreAuthorize 注解一致
--   - 已废弃的 @PreAuthorize("hasAuthority('group:work:create')") 和
--     @PreAuthorize("hasAuthority('group:user:add')") 在接口 6 和 9 已使用,必须确保 sys_menu 中存在对应 perms
--   - 若使用 vue-element-admin 等前端框架,需要在 src/permission.js 和 src/store/modules/permission.js 中增加对应路由
-- ============================================================