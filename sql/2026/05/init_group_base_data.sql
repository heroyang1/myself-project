-- ============================================================
-- 拼团模块上线 - 基础数据配置脚本
-- 创建时间: 2026-06-24
-- 说明:
--   1. 拼团活动模板初始数据(系统活动模板,供C端列表展示)
--   2. 服务器配置基础信息(3套典型规格)
--   3. 默认拼团规则参数(sys_config全局配置)
--   4. 系统管理员联系方式
--   5. 数据字典(状态、类型等枚举)
--
-- 依赖前置脚本:
--   - osh_server_group.sql         (创建核心表)
--   - osh_group_module.sql         (创建兼容表+通知表)
--   - sql/2026/05/fix_group_table_structure.sql  (添加activity_id字段)
--
-- 执行顺序:
--   1. 字典数据 (sys_dict_type / sys_dict_data)
--   2. 系统参数 (sys_config)
--   3. 拼团活动模板 (osh_group_activity)
--   4. 验证数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ==================== 第一步:数据字典配置 ====================
-- 说明: 用于前端下拉、状态显示、图标配色等

-- 1.1 拼团活动状态字典
INSERT INTO `sys_dict_type`(`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES('拼团活动状态', 'osh_group_activity_status', '0', 'admin', NOW(), '拼团活动状态:1-进行中 2-拼团成功 3-已结束');

INSERT INTO `sys_dict_data`(`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '进行中',   '1', 'osh_group_activity_status', '', 'primary', 'Y', '0', 'admin', NOW(), '拼团活动进行中'),
(2, '拼团成功', '2', 'osh_group_activity_status', '', 'success', 'N', '0', 'admin', NOW(), '达到最低人数'),
(3, '已结束',   '3', 'osh_group_activity_status', '', 'danger',  'N', '0', 'admin', NOW(), '人数达上限或到期');

-- 1.2 组团状态字典
INSERT INTO `sys_dict_type`(`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES('组团状态', 'osh_group_work_status', '0', 'admin', NOW(), '组团状态:0-进行中 1-已成团 2-已取消/过期');

INSERT INTO `sys_dict_data`(`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '进行中',     '0', 'osh_group_work_status', '', 'primary', 'Y', '0', 'admin', NOW(), '组团进行中'),
(2, '已成团',     '1', 'osh_group_work_status', '', 'success', 'N', '0', 'admin', NOW(), '达到最低人数'),
(3, '已取消/过期','2', 'osh_group_work_status', '', 'info',    'N', '0', 'admin', NOW(), '组团已取消或过期');

-- 1.3 拼团订单状态字典
INSERT INTO `sys_dict_type`(`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES('拼团订单状态', 'osh_group_order_status', '0', 'admin', NOW(), '拼团订单状态:pending/paid/success/refunded/cancelled');

INSERT INTO `sys_dict_data`(`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '待支付',   'pending',   'osh_group_order_status', '', 'warning', 'Y', '0', 'admin', NOW(), '订单待支付'),
(2, '已支付',   'paid',      'osh_group_order_status', '', 'primary', 'N', '0', 'admin', NOW(), '已支付待成团'),
(3, '拼团成功', 'success',   'osh_group_order_status', '', 'success', 'N', '0', 'admin', NOW(), '成团成功'),
(4, '已退款',   'refunded',  'osh_group_order_status', '', 'info',    'N', '0', 'admin', NOW(), '已退款'),
(5, '已取消',   'cancelled', 'osh_group_order_status', '', 'danger',  'N', '0', 'admin', NOW(), '已取消');

-- 1.4 拼团类型字典
INSERT INTO `sys_dict_type`(`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES('拼团商品类型', 'osh_group_type', '0', 'admin', NOW(), '拼团商品类型:目前仅服务器');

INSERT INTO `sys_dict_data`(`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '服务器资源', 'server', 'osh_group_type', '', 'primary', 'Y', '0', 'admin', NOW(), '服务器拼团');

-- 1.5 支付方式字典
INSERT INTO `sys_dict_type`(`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES('拼团支付方式', 'osh_group_pay_method', '0', 'admin', NOW(), '拼团订单支付方式');

INSERT INTO `sys_dict_data`(`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, '微信支付', 'wechat', 'osh_group_pay_method', '', 'success', 'Y', '0', 'admin', NOW(), '微信支付'),
(2, '支付宝',   'alipay', 'osh_group_pay_method', '', 'primary', 'N', '0', 'admin', NOW(), '支付宝');


-- ==================== 第二步:系统参数配置 (sys_config) ====================
-- 说明: 通过 sys_config 实现动态配置,避免硬编码

-- 2.1 拼团业务基础配置
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES
('拼团-订单支付超时(分钟)',     'osh.group.order.timeout_minutes',   '30',    'Y', 'admin', NOW(), '拼团订单超过该时间未支付自动取消'),
('拼团-发起招募有效期(天)',     'osh.group.initiate.expire_days',   '7',     'Y', 'admin', NOW(), '用户发起的拼团,超过该天数招募截止'),
('拼团-服务器到期前提醒(天)',   'osh.group.expire.notify_days',     '7,3,1', 'Y', 'admin', NOW(), '服务器到期前N天提醒用户,英文逗号分隔多个值'),
('拼团-成员价格折扣',           'osh.group.member.discount',        '0.8',   'Y', 'admin', NOW(), '会员价 = 当前价 × 该折扣(0.8 即8折)'),
('拼团-最低成团人数下限',       'osh.group.min_num.floor',          '2',     'Y', 'admin', NOW(), '允许配置的最低成团人数最小值'),
('拼团-最大拼团人数上限',       'osh.group.max_num.ceil',           '50',    'Y', 'admin', NOW(), '允许配置的最大拼团人数最大值'),
('拼团-单团招募最长期限(天)',   'osh.group.max_duration_days',      '180',   'Y', 'admin', NOW(), '单个拼团活动最大招募天数,超过后强制关闭'),
('拼团-价格向上取整天数',       'osh.group.price_round_days',       '30',    'Y', 'admin', NOW(), '剩余月数计算时,每多少天算一个月'),
('拼团-自定义价格下限系数',     'osh.group.price.min_ratio',        '0.5',   'Y', 'admin', NOW(), '自定义价格不得低于基础价格的此系数(0.5=50%)'),
('拼团-自定义价格上限系数',     'osh.group.price.max_ratio',        '2.0',   'Y', 'admin', NOW(), '自定义价格不得高于基础价格的此系数(2.0=200%)'),
('拼团-管理员默认联系方式',     'osh.group.admin.contact',          'group_admin', 'Y', 'admin', NOW(), '拼团活动页展示的管理员微信号(脱敏后展示)'),
('拼团-活动封面默认图',         'osh.group.cover.default',          '/default/group-cover.png', 'Y', 'admin', NOW(), '未配置封面时的默认封面URL');


-- ==================== 第三步:拼团活动模板初始数据 ====================
-- 说明: 三套典型服务器规格,覆盖入门/标准/高性能三种场景
-- 注意: 务必修改 admin_contact 为真实管理员微信; cover URL 替换为实际 OSS 路径

-- 3.1 高性能AI服务器 (12个月)
INSERT INTO `osh_group_activity`(
    `title`, `type`, `cpu`, `memory`, `storage`,
    `base_price`, `total_duration`, `group_min_num`, `group_max_num`,
    `current_num`, `status`, `start_time`,
    `server_tutorial_url`, `admin_contact`, `sort_order`,
    `cover`, `create_time`, `update_time`
) VALUES (
    '高性能AI服务器12月套餐', 'server',
    '8核', '32GB', '500GB NVMe SSD',
    1999.00, 12, 3, 8,
    0, 1, NOW(),
    '/tutorial/ai-server-setup.html', 'group_admin', 100,
    '/cover/group-ai-server.jpg', NOW(), NOW()
);

-- 3.2 标准服务器 (6个月)
INSERT INTO `osh_group_activity`(
    `title`, `type`, `cpu`, `memory`, `storage`,
    `base_price`, `total_duration`, `group_min_num`, `group_max_num`,
    `current_num`, `status`, `start_time`,
    `server_tutorial_url`, `admin_contact`, `sort_order`,
    `cover`, `create_time`, `update_time`
) VALUES (
    '标准型服务器6月套餐', 'server',
    '4核', '16GB', '200GB SSD',
    999.00, 6, 2, 5,
    0, 1, NOW(),
    '/tutorial/standard-server-setup.html', 'group_admin', 90,
    '/cover/group-standard-server.jpg', NOW(), NOW()
);

-- 3.3 入门服务器 (3个月)
INSERT INTO `osh_group_activity`(
    `title`, `type`, `cpu`, `memory`, `storage`,
    `base_price`, `total_duration`, `group_min_num`, `group_max_num`,
    `current_num`, `status`, `start_time`,
    `server_tutorial_url`, `admin_contact`, `sort_order`,
    `cover`, `create_time`, `update_time`
) VALUES (
    '入门型服务器3月体验套餐', 'server',
    '2核', '4GB', '80GB SSD',
    299.00, 3, 2, 3,
    0, 1, NOW(),
    '/tutorial/basic-server-setup.html', 'group_admin', 80,
    '/cover/group-basic-server.jpg', NOW(), NOW()
);


-- ==================== 第四步:补充缺失的关联配置 ====================

-- 4.1 为 osh_group_user_initiated 表补齐 activity_id 默认值(对历史数据)
UPDATE `osh_group_user_initiated` gui
INNER JOIN `osh_group_order` go ON gui.order_id = go.id
SET gui.activity_id = go.group_activity_id
WHERE gui.activity_id = 0 OR gui.activity_id IS NULL;


-- ==================== 第五步:验证数据 ====================

-- 5.1 检查活动模板数据
SELECT id, title, cpu, memory, storage, base_price, total_duration,
       group_min_num, group_max_num, status, admin_contact
FROM `osh_group_activity`
ORDER BY sort_order DESC;

-- 5.2 检查字典数据
SELECT dict_type, COUNT(*) AS dict_count
FROM sys_dict_data
WHERE dict_type LIKE 'osh_group_%'
GROUP BY dict_type
ORDER BY dict_type;

-- 5.3 检查系统参数
SELECT config_key, config_value, remark
FROM sys_config
WHERE config_key LIKE 'osh.group.%'
ORDER BY config_key;

-- 5.4 数据回填验证
SELECT
    '历史数据回填验证' AS check_name,
    SUM(CASE WHEN activity_id > 0 THEN 1 ELSE 0 END) AS with_activity,
    SUM(CASE WHEN activity_id = 0 OR activity_id IS NULL THEN 1 ELSE 0 END) AS without_activity,
    COUNT(*) AS total
FROM `osh_group_user_initiated`
WHERE delete_flag = 0;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 执行完成
-- 后续步骤:
--   1. 执行 init_group_menus_and_perms.sql 配置菜单与权限
--   2. 执行 init_group_quartz_jobs.sql 配置定时任务
--   3. 执行 optimize_group_indexes.sql 添加索引
--   4. 在管理后台修改 admin_contact 为真实微信号
--   5. 上传活动封面到 OSS,更新 cover 字段
-- ============================================================