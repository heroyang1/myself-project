-- ============================================================
-- GET /server/tutorial 接口相关表维护脚本
-- 
-- 接口链路：
--   Controller: OshGroupServerController.getServerTutorial()
--   Service:    OshGroupServerServiceImpl.getServerTutorial()
--   Mapper:     OshGroupServerMapper.selectGroupActivityById()
--               OshGroupUserInitiatedMapper.selectById()
--
-- 涉及数据表：
--   1. osh_group_activity       拼团活动表（主表，含服务器配置和教程URL）
--   2. osh_group_user_initiated 用户发起拼团记录表（从表，关联活动模板）
--   3. osh_group_work           组团记录表（含服务器分配信息）
--   4. osh_group_order          拼团订单表
--
-- 目标数据库：backstage
-- MySQL 版本：8.4.8
-- 字符集：utf8mb4
-- 创建日期：2026-07-01
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ============================================================
-- 第一部分：osh_group_activity 表基础信息维护
-- ============================================================

-- ----------------------------
-- 1.1 创建 osh_group_activity 表（服务器拼团活动表 - 实际使用版本）
-- 
-- 说明：该表是 /server/tutorial 接口的核心数据源。
--       server_tutorial_url 字段存储教程文件的 OSS 相对路径，
--       Service 层通过 ossUtil.getFullFilePath() 拼接为完整 URL 返回给前端。
--
-- 字段来源：根据 OshGroupActivity.java 实体类 + OshGroupServerMapper.xml 的 SQL 映射推导
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_activity`;
CREATE TABLE `osh_group_activity` (
    `id`                  BIGINT          NOT NULL AUTO_INCREMENT              COMMENT '拼团活动ID（主键）',
    `title`               VARCHAR(200)    NOT NULL                             COMMENT '拼团活动标题',
    `type`                VARCHAR(30)     NOT NULL DEFAULT 'server'            COMMENT '拼团类型：server-服务器（固定值）',
    `cpu`                 VARCHAR(50)     DEFAULT NULL                         COMMENT '服务器CPU配置（如：2核）',
    `memory`              VARCHAR(50)     DEFAULT NULL                         COMMENT '服务器内存配置（如：4GB）',
    `storage`             VARCHAR(50)     DEFAULT NULL                         COMMENT '服务器存储配置（如：50GB SSD）',
    `base_price`          DECIMAL(10,2)   NOT NULL DEFAULT 0.00                COMMENT '基础拼团价格（按月计算，完整周期价格）',
    `total_duration`      INT             DEFAULT NULL                         COMMENT '服务器总使用时长（月）',
    `group_min_num`       INT             NOT NULL DEFAULT 2                   COMMENT '拼团所需最低人数',
    `group_max_num`       INT             NOT NULL DEFAULT 5                   COMMENT '拼团人数上限',
    `current_num`         INT             NOT NULL DEFAULT 0                   COMMENT '当前已参团人数',
    `status`              TINYINT         NOT NULL DEFAULT 1                   COMMENT '活动状态：1-进行中 2-拼团成功 3-已结束',
    `start_time`          DATETIME        DEFAULT NULL                         COMMENT '拼团开始时间',
    `server_start_time`   DATETIME        DEFAULT NULL                         COMMENT '服务器开始使用时间（成团后有值）',
    `server_end_time`     DATETIME        DEFAULT NULL                         COMMENT '服务器使用结束时间（成团后有值）',
    `server_tutorial_url` VARCHAR(500)    DEFAULT NULL                         COMMENT '服务器配置教程URL（OSS相对路径）',
    `admin_contact`       VARCHAR(200)    DEFAULT NULL                         COMMENT '管理员联系方式',
    `sort_order`          INT             NOT NULL DEFAULT 0                   COMMENT '排序权重（值越大越靠前）',
    `cover`               VARCHAR(500)    DEFAULT NULL                         COMMENT '封面图片路径（OSS相对路径）',
    `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`         TINYINT(1)      NOT NULL DEFAULT 0                   COMMENT '逻辑删除：0-正常 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_type`          (`type`),
    KEY `idx_status`        (`status`),
    KEY `idx_sort_order`    (`sort_order`),
    KEY `idx_create_time`   (`create_time`),
    KEY `idx_delete_flag`   (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='服务器拼团活动表';


-- ----------------------------
-- 1.2 创建 osh_group_user_initiated 表（用户发起拼团记录表）
--
-- 说明：该表记录用户自主发起的拼团，通过 activity_id 关联到
--       osh_group_activity 获取教程URL和服务器配置模板信息。
--       /server/tutorial 接口优先从此表查询。
-- ----------------------------
DROP TABLE IF EXISTS `osh_group_user_initiated`;
CREATE TABLE `osh_group_user_initiated` (
    `id`                  BIGINT          NOT NULL AUTO_INCREMENT              COMMENT '发起记录ID（主键）',
    `user_id`             BIGINT          NOT NULL                             COMMENT '发起人用户ID → osh_user.id',
    `activity_id`         BIGINT          DEFAULT NULL                         COMMENT '关联活动模板ID → osh_group_activity.id',
    `order_id`            BIGINT          DEFAULT NULL                         COMMENT '关联订单ID → osh_group_order.id',
    `type`                VARCHAR(30)     NOT NULL DEFAULT 'server'            COMMENT '拼团类型：server-服务器',
    `min_num`             INT             NOT NULL DEFAULT 2                   COMMENT '最低成团人数',
    `max_num`             INT             NOT NULL DEFAULT 5                   COMMENT '最多成团人数',
    `duration`            INT             NOT NULL DEFAULT 12                  COMMENT '服务器使用时长（月）',
    `custom_price`        DECIMAL(10,2)   NOT NULL DEFAULT 0.00                COMMENT '自定义拼团价格（总价）',
    `group_status`        TINYINT         NOT NULL DEFAULT 0                   COMMENT '组团状态：0-招募中 1-已成团 2-已取消/过期 3-拼团结束',
    `current_num`         INT             NOT NULL DEFAULT 1                   COMMENT '当前参团人数（含发起人自己）',
    `server_ip`           VARCHAR(50)     DEFAULT NULL                         COMMENT '服务器IP地址',
    `server_account`      VARCHAR(100)    DEFAULT NULL                         COMMENT '服务器登录账号',
    `server_password`     VARCHAR(255)    DEFAULT NULL                         COMMENT '服务器登录密码（AES加密存储）',
    `server_start_time`   DATETIME        DEFAULT NULL                         COMMENT '服务器开始使用时间',
    `server_expire_time`  DATETIME        DEFAULT NULL                         COMMENT '服务器到期时间',
    `initiate_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP            COMMENT '发起时间',
    `expire_time`         DATETIME        DEFAULT NULL                         COMMENT '招募截止时间',
    `create_time`         DATETIME        DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_time`         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `delete_flag`         TINYINT(1)      DEFAULT 0                            COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id`           (`user_id`),
    KEY `idx_activity_id`       (`activity_id`),
    KEY `idx_order_id`          (`order_id`),
    KEY `idx_group_status`      (`group_status`),
    KEY `idx_type`              (`type`),
    KEY `idx_initiate_time`     (`initiate_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100
  CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户发起拼团记录表';


-- ============================================================
-- 第二部分：服务器拼团服务内容 - 数据管理脚本
-- ============================================================

-- ----------------------------
-- 2.1 初始化服务器拼团活动模板数据
-- ----------------------------

-- 基础型服务器拼团活动（2核4G）
INSERT INTO `osh_group_activity` (
    `title`, `type`, `cpu`, `memory`, `storage`,
    `base_price`, `total_duration`, `group_min_num`, `group_max_num`,
    `current_num`, `status`, `start_time`,
    `server_tutorial_url`, `admin_contact`, `sort_order`, `cover`
) VALUES (
    '轻量开发服务器 - 2核4G',
    'server',
    '2核',
    '4GB',
    '50GB SSD',
    299.00,
    12,
    3,
    5,
    0,
    1,
    NOW(),
    'tutorials/server-basic-guide.html',
    '微信：dev_support | QQ群：123456789',
    100,
    'covers/server_basic.png'
);

-- 进阶型服务器拼团活动（4核8G）
INSERT INTO `osh_group_activity` (
    `title`, `type`, `cpu`, `memory`, `storage`,
    `base_price`, `total_duration`, `group_min_num`, `group_max_num`,
    `current_num`, `status`, `start_time`,
    `server_tutorial_url`, `admin_contact`, `sort_order`, `cover`
) VALUES (
    '全能型服务器 - 4核8G',
    'server',
    '4核',
    '8GB',
    '100GB SSD',
    599.00,
    12,
    5,
    10,
    0,
    1,
    NOW(),
    'tutorials/server-advanced-guide.html',
    '微信：dev_support | QQ群：123456789',
    90,
    'covers/server_advanced.png'
);

-- 高配型服务器拼团活动（8核16G）
INSERT INTO `osh_group_activity` (
    `title`, `type`, `cpu`, `memory`, `storage`,
    `base_price`, `total_duration`, `group_min_num`, `group_max_num`,
    `current_num`, `status`, `start_time`,
    `server_tutorial_url`, `admin_contact`, `sort_order`, `cover`
) VALUES (
    '企业级服务器 - 8核16G',
    'server',
    '8核',
    '16GB',
    '200GB SSD',
    999.00,
    12,
    8,
    15,
    0,
    1,
    NOW(),
    'tutorials/server-pro-guide.html',
    '微信：enterprise_support | QQ群：987654321',
    80,
    'covers/server_pro.png'
);


-- ----------------------------
-- 2.2 更新已有活动的教程URL
-- ----------------------------

-- 更新指定活动的教程URL
-- UPDATE `osh_group_activity`
-- SET `server_tutorial_url` = 'tutorials/server-basic-guide-v2.html',
--     `update_time` = NOW()
-- WHERE `id` = 100;

-- 批量更新所有进行中的活动教程URL（慎用）
-- UPDATE `osh_group_activity`
-- SET `server_tutorial_url` = CONCAT('tutorials/server-guide-', `cpu`, '-', `memory`, '.html')
-- WHERE `status` = 1 AND `type` = 'server';


-- ----------------------------
-- 2.3 更新管理员联系方式
-- ----------------------------

-- 更新指定活动的管理员联系方式
-- UPDATE `osh_group_activity`
-- SET `admin_contact` = '微信：new_support | 邮箱：support@example.com',
--     `update_time` = NOW()
-- WHERE `id` = 100;


-- ----------------------------
-- 2.4 查询活动数据
-- ----------------------------

-- 查询所有进行中的服务器拼团活动
SELECT 
    id,
    title,
    cpu,
    memory,
    storage,
    base_price,
    total_duration,
    group_min_num,
    group_max_num,
    current_num,
    status,
    server_tutorial_url,
    admin_contact,
    cover
FROM osh_group_activity
WHERE type = 'server' AND status IN (1, 2)
ORDER BY sort_order DESC, create_time DESC;

-- 查询指定活动详情（模拟 /server/tutorial 接口查询）
SELECT *
FROM osh_group_activity
WHERE id = 100;


-- ----------------------------
-- 2.5 逻辑删除活动（软删除）
-- ----------------------------

-- 结束指定活动（状态改为已结束）
-- UPDATE `osh_group_activity`
-- SET `status` = 3,
--     `update_time` = NOW()
-- WHERE `id` = 100;


-- ============================================================
-- 第三部分：服务器配置指南相关 - 数据管理脚本
-- ============================================================

-- ----------------------------
-- 3.1 用户发起拼团记录管理
-- ----------------------------

-- 插入用户发起的拼团记录（模拟用户发起拼团）
INSERT INTO `osh_group_user_initiated` (
    `user_id`, `activity_id`, `type`,
    `min_num`, `max_num`, `duration`, `custom_price`,
    `group_status`, `current_num`,
    `initiate_time`, `expire_time`, `create_time`
) VALUES (
    1,              -- 发起人用户ID
    100,            -- 关联活动模板ID（对应上面插入的2核4G活动）
    'server',
    3,              -- 最低3人成团
    5,              -- 最多5人
    12,             -- 12个月
    299.00,         -- 自定义价格
    0,              -- 招募中
    1,              -- 当前1人（发起人）
    NOW(),          -- 发起时间
    DATE_ADD(NOW(), INTERVAL 7 DAY),  -- 7天后招募截止
    NOW()
);


-- ----------------------------
-- 3.2 模拟 /server/tutorial 接口的查询逻辑
-- ----------------------------

-- 步骤1：尝试从 osh_group_user_initiated 查询（activityId 作为发起记录ID）
-- 对应代码：OshGroupUserInitiatedMapper.selectById(activityId)
SELECT *
FROM osh_group_user_initiated
WHERE id = 100 AND delete_flag = 0;

-- 步骤2：如果找到，通过 activity_id 关联查询活动模板
-- 对应代码：OshGroupServerMapper.selectGroupActivityById(initiated.getActivityId())
SELECT *
FROM osh_group_activity
WHERE id = (
    SELECT activity_id 
    FROM osh_group_user_initiated 
    WHERE id = 100 AND delete_flag = 0
);

-- 步骤3：如果未找到发起记录，直接查询活动模板
-- 对应代码：OshGroupServerMapper.selectGroupActivityById(activityId)
SELECT *
FROM osh_group_activity
WHERE id = 100;


-- ----------------------------
-- 3.3 更新拼团状态（成团时设置服务器时间）
-- ----------------------------

-- 模拟成团：更新发起记录状态为已成团，设置服务器时间
-- UPDATE `osh_group_user_initiated`
-- SET `group_status` = 1,
--     `server_start_time` = NOW(),
--     `server_expire_time` = DATE_ADD(NOW(), INTERVAL `duration` MONTH),
--     `update_time` = NOW()
-- WHERE `id` = 100 AND `delete_flag` = 0;

-- 同步更新关联活动模板的状态
-- UPDATE `osh_group_activity`
-- SET `status` = 2,
--     `server_start_time` = NOW(),
--     `server_end_time` = DATE_ADD(NOW(), INTERVAL 12 MONTH),
--     `update_time` = NOW()
-- WHERE `id` = (
--     SELECT activity_id FROM osh_group_user_initiated WHERE id = 100
-- );


-- ----------------------------
-- 3.4 分配服务器信息（成团后执行）
-- ----------------------------

-- 为发起记录分配服务器凭证
-- UPDATE `osh_group_user_initiated`
-- SET `server_ip` = '43.242.200.100',
--     `server_account` = 'user_100',
--     `server_password` = AES_ENCRYPT('SecureP@ss2026', 'encryption_key'),
--     `update_time` = NOW()
-- WHERE `id` = 100 AND `delete_flag` = 0;


-- ----------------------------
-- 3.5 查询用户发起的拼团列表（含教程URL）
-- ----------------------------

SELECT 
    gui.id,
    gui.activity_id,
    COALESCE(ga.title, '未知活动') AS title,
    COALESCE(ga.cpu, '待定') AS cpu,
    COALESCE(ga.memory, '待定') AS memory,
    COALESCE(ga.storage, '待定') AS storage,
    gui.custom_price AS base_price,
    gui.duration AS total_duration,
    gui.current_num,
    gui.min_num AS group_min_num,
    gui.max_num AS group_max_num,
    gui.group_status,
    gui.server_ip,
    gui.server_start_time,
    gui.server_expire_time,
    ga.server_tutorial_url,
    ga.admin_contact,
    ga.cover
FROM osh_group_user_initiated gui
LEFT JOIN osh_group_activity ga ON gui.activity_id = ga.id
WHERE gui.delete_flag = 0
ORDER BY gui.initiate_time DESC;


-- ----------------------------
-- 3.6 增加参团人数（有人加入拼团时）
-- ----------------------------

-- 递增当前参团人数
-- UPDATE `osh_group_user_initiated`
-- SET `current_num` = `current_num` + 1,
--     `update_time` = NOW()
-- WHERE `id` = 100 AND `delete_flag` = 0
--   AND `current_num` < `max_num`;

-- 同步更新活动模板的参团人数
-- UPDATE `osh_group_activity`
-- SET `current_num` = `current_num` + 1,
--     `update_time` = NOW()
-- WHERE `id` = (
--     SELECT activity_id FROM osh_group_user_initiated WHERE id = 100
-- );


-- ----------------------------
-- 3.7 取消/过期拼团记录
-- ----------------------------

-- 逻辑删除发起记录
-- UPDATE `osh_group_user_initiated`
-- SET `group_status` = 2,
--     `delete_flag` = 1,
--     `update_time` = NOW()
-- WHERE `id` = 100;

-- 批量清理过期的招募中记录
-- UPDATE `osh_group_user_initiated`
-- SET `group_status` = 3,
--     `update_time` = NOW()
-- WHERE `group_status` = 0
--   AND `expire_time` < NOW()
--   AND `delete_flag` = 0;


-- ----------------------------
-- 3.8 数据验证查询
-- ----------------------------

-- 查看各状态的活动数量统计
SELECT 
    status,
    CASE status
        WHEN 1 THEN '进行中'
        WHEN 2 THEN '拼团成功'
        WHEN 3 THEN '已结束'
        ELSE '未知'
    END AS status_name,
    COUNT(*) AS count
FROM osh_group_activity
WHERE type = 'server'
GROUP BY status
ORDER BY status;

-- 查看各状态的发起记录数量统计
SELECT 
    group_status,
    CASE group_status
        WHEN 0 THEN '招募中'
        WHEN 1 THEN '已成团'
        WHEN 2 THEN '已取消/过期'
        WHEN 3 THEN '拼团结束'
        ELSE '未知'
    END AS status_name,
    COUNT(*) AS count
FROM osh_group_user_initiated
WHERE delete_flag = 0
GROUP BY group_status
ORDER BY group_status;

-- 查看活动与发起记录的关联关系
SELECT 
    ga.id AS activity_id,
    ga.title,
    ga.cpu,
    ga.memory,
    COUNT(gui.id) AS initiated_count,
    SUM(gui.current_num) AS total_participants
FROM osh_group_activity ga
LEFT JOIN osh_group_user_initiated gui ON ga.id = gui.activity_id AND gui.delete_flag = 0
WHERE ga.type = 'server'
GROUP BY ga.id, ga.title, ga.cpu, ga.memory
ORDER BY ga.sort_order DESC;


SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 第四部分：业务逻辑说明
-- ============================================================
--
-- 1. 接口说明
--    GET /server/tutorial?activityId={id}
--    - 匿名可访问（@Anonymous注解）
--    - 根据 activityId 获取服务器使用教程
--
-- 2. 查询逻辑（双数据源策略）
--    a) 先尝试将 activityId 作为 osh_group_user_initiated.id 查询
--       - 如果找到 → 通过 activity_id 关联查询 osh_group_activity 获取教程
--    b) 如果未找到 → 将 activityId 作为 osh_group_activity.id 直接查询
--    c) 如果两者都未找到 → 抛出 "拼团活动不存在" 异常
--
-- 3. 返回数据结构（ServerTutorialVO）
--    - tutorial:      教程内容URL（从 server_tutorial_url 拼接 OSS 完整路径）
--    - videoUrl:      教程视频链接（当前未使用，预留扩展）
--    - steps:         教程步骤列表（硬编码4步：连接服务器→初始化配置→环境检查→开始使用）
--    - serverConfig:  服务器配置信息（硬编码：SSH端口22，默认用户root）
--    - faq:           常见问题列表（硬编码4个FAQ）
--
-- 4. 使用场景
--    a) 用户在拼团详情页查看服务器使用教程（成团前/后均可访问）
--    b) 前端根据 tutorial URL 加载教程内容展示给用户
--    c) steps 和 faq 为前端辅助展示内容，不依赖数据库存储
--
-- 5. 关联表关系
--    osh_group_activity (活动模板)
--        ├── 1:N → osh_group_user_initiated (用户发起记录，通过 activity_id 关联)
--        ├── 1:N → osh_group_work (组团记录，通过 group_activity_id 关联)
--        └── 1:N → osh_group_order (订单记录，通过 group_activity_id 关联)
--
--    osh_group_user_initiated (发起记录)
--        ├── N:1 → osh_group_activity (活动模板)
--        ├── 1:1 → osh_group_order (订单)
--        └── 1:N → osh_group_work (组团记录)
-- ============================================================
