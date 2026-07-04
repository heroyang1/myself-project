-- ============================================================
-- 拼团模块权限配置（兼容版）
-- 使用方式：逐段执行（先执行第一部分，再执行第二部分）
-- ============================================================

-- ============================================================
-- 第一部分：插入权限数据（单条多行 VALUES，一次性执行）
-- ============================================================

INSERT INTO backstage.osh_permission
(permission_name, permission_code, description, parent_id, `type`, url, `path`, component, sort_order, create_time, create_by, update_time, update_by, delete_flag)
VALUES -- 1. 拼团模块父菜单（parent_id=0 表示顶级菜单）
('拼团管理',           'group:module',                 '拼团模块',                         0,   1, NULL, '/group',                        NULL, 200, NOW(), 0, NOW(), 0, 0),
-- 2. 拼团活动列表（匿名可访问）
('拼团活动列表',       'group:activity:list',           '查询拼团活动列表',                 0,   2, '/pc/group/activity/list',           NULL, NULL, 1,   NOW(), 0, NOW(), 0, 0),
-- 3. 用户发起拼团列表
('发起拼团列表',       'group:activity:initiated:list', '查询用户发起的拼团列表',           0,   2, '/pc/group/activity/initiated/list', NULL, NULL, 2,   NOW(), 0, NOW(), 0, 0),
-- 4. 我的拼团列表
('我的拼团列表',       'group:activity:mylist',         '查询我的拼团记录',                 0,   2, '/pc/group/activity/mylist',         NULL, NULL, 3,   NOW(), 0, NOW(), 0, 0),
-- 5. 可发起的拼团活动列表（管理员）
('可发起拼团列表',     'group:activity:initiable',      '获取可发起的拼团活动列表',         0,   2, '/pc/group/activity/initiable',      NULL, NULL, 4,   NOW(), 0, NOW(), 0, 0),
-- 6. 拼团详情（匿名可访问）
('拼团详情',           'group:work:detail',             '查看拼团详情',                     0,   2, '/pc/group/work/detail',             NULL, NULL, 10,  NOW(), 0, NOW(), 0, 0),
-- 7. 参与拼团
('参与拼团',           'group:work:join',               '参与拼团（支付参团）',             0,   2, '/pc/group/work/join',               NULL, NULL, 11,  NOW(), 0, NOW(), 0, 0),
-- 8. 发起拼团（@PreAuthorize 已使用此标识）
('发起拼团',           'group:work:create',             '创建新的拼团组团',                 0,   2, '/pc/group/work/create',             NULL, NULL, 12,  NOW(), 0, NOW(), 0, 0),
-- 9. 全量组团记录查询（@PreAuthorize 已使用此标识，管理端）
('组团记录查询',       'group:work:query',              '查询全量组团记录列表（管理端）',   0,   2, '/pc/group/work/list',               NULL, NULL, 13,  NOW(), 0, NOW(), 0, 0),
-- 10. 拼团订单支付
('拼团订单支付',       'group:work:pay',                '支付拼团订单',                     0,   2, '/pc/group/work/pay',                NULL, NULL, 20,  NOW(), 0, NOW(), 0, 0),
-- 11. 拼团订单状态查询
('拼团订单状态',       'group:order:status',            '查询拼团订单状态',                 0,   2, '/pc/group/order/status',            NULL, NULL, 21,  NOW(), 0, NOW(), 0, 0),
-- 12. 搜索用户（@PreAuthorize 已使用此标识，管理端）
('搜索拼团用户',       'group:user:search',             '模糊查询用户名（手动添加参团用）', 0,   2, '/pc/group/user/search',             NULL, NULL, 30,  NOW(), 0, NOW(), 0, 0),
-- 13. 手动添加用户到拼团（@PreAuthorize 已使用此标识，管理端）
('手动添加参团用户',   'group:user:add',                '管理员手动添加用户到拼团',         0,   2, '/pc/group/user/add',                NULL, NULL, 31,  NOW(), 0, NOW(), 0, 0),
-- 14. 拼团课程详情
('拼团课程详情',       'group:course:read',             '查看拼团关联的课程详情',           0,   2, '/pc/group/course/read',             NULL, NULL, 40,  NOW(), 0, NOW(), 0, 0),
-- 15. 拼团专栏详情
('拼团专栏详情',       'group:column:read',             '查看拼团关联的专栏详情',           0,   2, '/pc/group/column/read',             NULL, NULL, 41,  NOW(), 0, NOW(), 0, 0),
-- 16. 拼团可组团列表（旧版）
('拼团可组团列表',     'group:list',                    '查看当前拼团的可组团列表',         0,   2, '/pc/group/list',                    NULL, NULL, 42,  NOW(), 0, NOW(), 0, 0),
-- 17. 服务器教程（匿名可访问）
('服务器教程',         'group:server:tutorial',         '获取拼团服务器使用教程',           0,   2, '/pc/group/server/tutorial',         NULL, NULL, 50,  NOW(), 0, NOW(), 0, 0),
-- 18. 服务器SSH信息
('服务器SSH信息',      'group:server:ssh-info',         '获取拼团服务器SSH连接信息',        0,   2, '/pc/group/server/ssh-info',         NULL, NULL, 51,  NOW(), 0, NOW(), 0, 0),
-- 19. 系统活动分享海报
('系统活动海报',       'group:share:poster',            '获取系统活动分享海报数据',         0,   2, '/pc/group/share/poster',            NULL, NULL, 60,  NOW(), 0, NOW(), 0, 0),
-- 20. 用户发起拼团分享海报
('发起拼团海报',       'group:share:poster:initiated',  '获取用户发起拼团分享海报数据',     0,   2, '/pc/group/share/poster/initiated',  NULL, NULL, 61,  NOW(), 0, NOW(), 0, 0);


-- ============================================================
-- 第二部分：修正子权限的 parent_id（在上面的 INSERT 执行成功后执行）
-- 将所有 group:* 子权限的 parent_id 设为 group:module 的 id
-- ============================================================

UPDATE backstage.osh_permission
SET parent_id = (
    SELECT t.id FROM (SELECT id FROM backstage.osh_permission WHERE permission_code = 'group:module' AND delete_flag = 0 LIMIT 1) t
)
WHERE permission_code LIKE 'group:%'
  AND permission_code != 'group:module'
  AND delete_flag = 0
  AND parent_id = 0;


-- ============================================================
-- 第三部分：角色权限关联（在权限数据就绪后执行）
-- 敏感操作 group:work:create(发起拼团) 和 group:user:add(手动添加用户)
-- 仅分配给核心开发者(role_id=6)和创始人(role_id=7)
-- ============================================================

INSERT INTO backstage.osh_role_permission (role_id, permission_id, create_time, create_by, update_time, update_by, delete_flag)
SELECT r.role_id, p.id, NOW(), 0, NOW(), 0, 0
FROM (
    SELECT 1 AS role_id UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7
) r
CROSS JOIN backstage.osh_permission p
WHERE p.permission_code LIKE 'group:%'
  AND p.delete_flag = 0
  -- 敏感操作仅分配给核心开发者(6)和创始人(7)
  AND (
      p.permission_code NOT IN ('group:work:create', 'group:user:add')
      OR r.role_id IN (6, 7)
  )
  AND NOT EXISTS (
    SELECT 1 FROM backstage.osh_role_permission rp
    WHERE rp.role_id = r.role_id AND rp.permission_id = p.id AND rp.delete_flag = 0
);


-- ============================================================
-- 验证查询
-- ============================================================

-- 查看拼团模块所有权限
SELECT id, permission_code, permission_name, parent_id, `type`, url
FROM backstage.osh_permission
WHERE permission_code LIKE 'group:%' AND delete_flag = 0
ORDER BY sort_order;

-- 查看各角色的拼团权限数量
SELECT rp.role_id, COUNT(*) AS perm_count
FROM backstage.osh_role_permission rp
JOIN backstage.osh_permission p ON rp.permission_id = p.id
WHERE p.permission_code LIKE 'group:%' AND rp.delete_flag = 0
GROUP BY rp.role_id;
