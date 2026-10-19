-- ============================================================
-- 拼团模块上线 - 菜单与权限配置
-- 说明:
--   1. 使用 osh_permission + osh_role_permission 权限模型
--   2. 创建拼团管理父菜单(type=1, parent_id=0)
--   3. 创建管理端子菜单: 拼团活动、组团记录、拼团订单、用户发起(type=1)
--   4. 创建按钮/API权限: 查询/新增/修改/删除/导出等(type=2)
--   5. 通过 osh_role_permission 关联角色(1~7)
--
-- 依赖前置脚本:
--   - osh_permission.sql         (osh_permission/osh_role/osh_role_permission 表结构)
--   - init_group_base_data.sql   (字典+配置+活动模板)
--   - group_permission.sql       (C端接口API权限, 需先执行)
--
-- 执行顺序:
--   1. osh_permission 插入管理端菜单和按钮记录
--   2. osh_role_permission 关联角色
--
-- 注意: 本脚本仅补充管理端菜单树(type=1子菜单+type=2按钮),
--       C端接口权限(如 group:activity:list, group:work:join 等)
--       已由 group_permission.sql 覆盖, 此处不重复插入。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ==================== 第一步: 创建管理端子菜单(type=1) ====================
-- 拼团管理父菜单已由 group_permission.sql 创建(permission_code='group:module')
-- 此处补充管理端的4个二级子菜单, parent_id 指向 group:module

-- 获取 group:module 的 id 作为父菜单ID
SET @groupModuleId = (
    SELECT id FROM `osh_permission`
    WHERE permission_code = 'group:module' AND delete_flag = 0
    LIMIT 1
);

-- 如果 group:module 不存在, 则创建
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT '拼团管理', 'group:module', '拼团模块管理目录', 0, 1,
       NULL, '/group', NULL, 200,
       NOW(), 0, NOW(), 0, 0
FROM DUAL
WHERE @groupModuleId IS NULL;

-- 重新获取(确保后续引用正确)
SET @groupModuleId = (
    SELECT id FROM `osh_permission`
    WHERE permission_code = 'group:module' AND delete_flag = 0
    LIMIT 1
);


-- 1.1 拼团活动管理子菜单
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT '拼团活动管理', 'group:admin:activity', '拼团活动模板管理页面', @groupModuleId, 1,
       NULL, '/group/activity', 'servicegroup/activity/index', 1,
       NOW(), 0, NOW(), 0, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` WHERE permission_code = 'group:admin:activity' AND delete_flag = 0
);

-- 1.2 组团记录管理子菜单
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT '组团记录管理', 'group:admin:work', '组团记录管理页面', @groupModuleId, 1,
       NULL, '/group/work', 'servicegroup/work/index', 2,
       NOW(), 0, NOW(), 0, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` WHERE permission_code = 'group:admin:work' AND delete_flag = 0
);

-- 1.3 拼团订单管理子菜单
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT '拼团订单管理', 'group:admin:order', '拼团订单管理页面', @groupModuleId, 1,
       NULL, '/group/order', 'servicegroup/order/index', 3,
       NOW(), 0, NOW(), 0, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` WHERE permission_code = 'group:admin:order' AND delete_flag = 0
);

-- 1.4 用户发起拼团管理子菜单
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT '用户发起管理', 'group:admin:initiated', '用户发起的拼团管理页面', @groupModuleId, 1,
       NULL, '/group/initiated', 'servicegroup/initiated/index', 4,
       NOW(), 0, NOW(), 0, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` WHERE permission_code = 'group:admin:initiated' AND delete_flag = 0
);


-- ==================== 第二步: 创建按钮权限(type=2, API级别) ====================
-- 注意: C端接口权限(如 group:activity:list, group:work:join)已由 group_permission.sql 创建
--       此处仅补充管理端专用操作权限

-- 2.1 拼团活动按钮
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
) VALUES
('活动查询',   'group:activity:query',  '拼团活动查询',       @groupModuleId, 2, NULL, NULL, NULL, 101, NOW(), 0, NOW(), 0, 0),
('活动新增',   'group:activity:add',    '拼团活动新增',       @groupModuleId, 2, NULL, NULL, NULL, 102, NOW(), 0, NOW(), 0, 0),
('活动修改',   'group:activity:edit',   '拼团活动修改',       @groupModuleId, 2, NULL, NULL, NULL, 103, NOW(), 0, NOW(), 0, 0),
('活动删除',   'group:activity:remove', '拼团活动删除',       @groupModuleId, 2, NULL, NULL, NULL, 104, NOW(), 0, NOW(), 0, 0),
('活动导出',   'group:activity:export', '拼团活动导出',       @groupModuleId, 2, NULL, NULL, NULL, 105, NOW(), 0, NOW(), 0, 0),
('上下架活动', 'group:activity:status', '拼团活动上下架',     @groupModuleId, 2, NULL, NULL, NULL, 106, NOW(), 0, NOW(), 0, 0);

-- 2.2 组团记录按钮
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT v.permission_name, v.permission_code, v.description, @groupModuleId, 2,
       NULL, NULL, NULL, v.sort_order,
       NOW(), 0, NOW(), 0, 0
FROM (
    SELECT '组团查询' AS permission_name, 'group:work:query' AS permission_code, '组团记录查询' AS description, 111 AS sort_order
    UNION ALL SELECT '强制成团', 'group:work:force', '强制成团操作', 112
    UNION ALL SELECT '组团导出', 'group:work:export', '组团记录导出', 113
) v
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` p
    WHERE p.permission_code = v.permission_code AND p.delete_flag = 0
);

-- 2.3 拼团订单按钮
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT v.permission_name, v.permission_code, v.description, @groupModuleId, 2,
       NULL, NULL, NULL, v.sort_order,
       NOW(), 0, NOW(), 0, 0
FROM (
    SELECT '订单查询' AS permission_name, 'group:order:query' AS permission_code, '订单查询' AS description, 121 AS sort_order
    UNION ALL SELECT '订单导出', 'group:order:export', '订单导出', 122
    UNION ALL SELECT '订单退款', 'group:order:refund', '订单退款操作', 123
    UNION ALL SELECT '关闭订单', 'group:order:close',  '强制关闭订单', 124
    UNION ALL SELECT '补发凭证', 'group:order:resend', '补发服务器凭证', 125
) v
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` p
    WHERE p.permission_code = v.permission_code AND p.delete_flag = 0
);

-- 2.4 用户发起拼团按钮
INSERT INTO `osh_permission`(
    permission_name, permission_code, description, parent_id, `type`,
    url, `path`, component, sort_order,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT v.permission_name, v.permission_code, v.description, @groupModuleId, 2,
       NULL, NULL, NULL, v.sort_order,
       NOW(), 0, NOW(), 0, 0
FROM (
    SELECT '发起记录查询' AS permission_name, 'group:initiated:query' AS permission_code, '发起记录查询' AS description, 131 AS sort_order
    UNION ALL SELECT '发起记录导出', 'group:initiated:export', '发起记录导出', 132
    UNION ALL SELECT '强制关闭发起', 'group:initiated:close',  '强制关闭发起拼团', 133
) v
WHERE NOT EXISTS (
    SELECT 1 FROM `osh_permission` p
    WHERE p.permission_code = v.permission_code AND p.delete_flag = 0
);


-- ==================== 第三步: 关联角色权限 osh_role_permission ====================
-- 关联规则:
--   role_id=1~7: 关联大部分 group:* 权限
--   role_id=6(核心开发者), 7(创始人): 额外拥有敏感操作权限
--     - group:work:create (发起拼团)
--     - group:user:add    (手动添加用户参与)
--   其他角色(1~5)不分配上述两个敏感操作
--   使用 NOT EXISTS 防重复插入

INSERT INTO `osh_role_permission`(
    role_id, permission_id,
    create_time, create_by, update_time, update_by, delete_flag
)
SELECT r.role_id, p.id,
       NOW(), 0, NOW(), 0, 0
FROM (
    SELECT 1 AS role_id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) r
CROSS JOIN `osh_permission` p
WHERE p.permission_code LIKE 'group:%'
  AND p.delete_flag = 0
  -- 敏感操作仅分配给核心开发者(6)和创始人(7)
  AND (
      p.permission_code NOT IN ('group:work:create', 'group:user:add')
      OR r.role_id IN (6, 7)
  )
  AND NOT EXISTS (
    SELECT 1 FROM `osh_role_permission` rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.id
      AND rp.delete_flag = 0
  );


-- ==================== 第四步: 验证数据 ====================

-- 4.1 查看拼团模块权限树
SELECT id, parent_id, permission_name, permission_code, `type`, `path`, component, sort_order
FROM `osh_permission`
WHERE permission_code LIKE 'group:%' AND delete_flag = 0
ORDER BY sort_order;

-- 4.2 查看各角色的拼团权限数量
SELECT rp.role_id, r.role_name, COUNT(*) AS perm_count
FROM `osh_role_permission` rp
JOIN `osh_permission` p ON rp.permission_id = p.id
LEFT JOIN `osh_role` r ON rp.role_id = r.id
WHERE p.permission_code LIKE 'group:%' AND rp.delete_flag = 0
GROUP BY rp.role_id, r.role_name
ORDER BY rp.role_id;

-- 4.3 统计权限类型分布
SELECT
    CASE `type` WHEN 1 THEN '菜单' WHEN 2 THEN '按钮/API' END AS type_name,
    COUNT(*) AS count
FROM `osh_permission`
WHERE permission_code LIKE 'group:%' AND delete_flag = 0
GROUP BY `type`;

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 执行完成
-- 后续步骤:
--   1. 确保 group_permission.sql 已执行(C端接口权限)
--   2. 执行 init_group_quartz_jobs.sql 配置定时任务
--   3. 执行 optimize_group_indexes.sql 添加索引
--   4. 前端代码中通过 permission_code 控制按钮显示
--
-- 注意事项:
--   - 管理端子菜单使用 group:admin:* 命名空间, 避免与 C端接口权限冲突
--   - 按钮权限 permission_code 与 Controller 的 @PreAuthorize 注解保持一致
--   - 所有 INSERT 使用 NOT EXISTS 防重复, 可安全重复执行
--   - osh_role 表需已存在 role_id=1~7 的角色数据
-- ============================================================