# 拼团模块表结构完整修复方案

## 一、设计问题的根本原因

### 1.1 核心矛盾:业务模式演进导致表设计不匹配

**原始设计**(传统拼团):
```
管理员创建活动 → 用户加入活动
osh_group_activity (活动实例) → osh_group_work (参团记录)
```

**新增需求**(用户发起拼团):
```
管理员创建模板 → 用户发起实例 → 其他用户加入
osh_group_activity (模板) → osh_group_user_initiated (用户实例) → osh_group_work (参团记录)
```

**问题**: `osh_group_activity` 表被迫承担双重角色(模板+实例),导致概念混乱。

### 1.2 表设计三大问题

| 问题 | 严重性 | 影响 | 根本原因 |
|------|--------|------|----------|
| 缺少 activity_id | 🔴 严重 | 查询性能差、数据关联脆弱 | 表设计时未考虑模板-实例关系 |
| 职责混淆 | 🟡 中等 | 代码逻辑复杂、易出错 | 业务模式演进未重构表结构 |
| 字段冗余 | 🟢 轻微 | 存储浪费、维护成本高 | 用户自定义配置需要独立存储 |

## 二、完整的表结构修复方案

### 方案A: 最小改动方案(推荐短期使用) ⭐

**核心思路**: 在现有表结构上添加 `activity_id` 字段,最小化改动。

#### 2.1.1 修改 osh_group_user_initiated 表

```sql
-- 1. 添加 activity_id 字段
ALTER TABLE `osh_group_user_initiated` 
ADD COLUMN `activity_id` bigint NOT NULL DEFAULT 0 COMMENT '关联活动模板ID → osh_group_activity.id' 
AFTER `user_id`;

-- 2. 添加索引
ALTER TABLE `osh_group_user_initiated` 
ADD INDEX `idx_activity_id` (`activity_id`) 
COMMENT '优化活动模板关联查询';

-- 3. 添加 order 表索引
ALTER TABLE `osh_group_order` 
ADD INDEX `idx_group_activity_id` (`group_activity_id`) 
COMMENT '优化用户发起拼团列表查询性能';

-- 4. 回填历史数据
UPDATE osh_group_user_initiated gui
INNER JOIN osh_group_order go ON gui.order_id = go.id
SET gui.activity_id = go.group_activity_id
WHERE gui.activity_id = 0;

-- 5. 验证数据完整性
SELECT 
    gui.id,
    gui.activity_id,
    go.group_activity_id,
    CASE 
        WHEN gui.activity_id = go.group_activity_id THEN '✅ 一致'
        ELSE '❌ 不一致'
    END AS status
FROM osh_group_user_initiated gui
LEFT JOIN osh_group_order go ON gui.order_id = go.id
WHERE gui.delete_flag = 0
LIMIT 100;
```

#### 2.1.2 修改 Java 代码

**文件**: `OshGroupServerServiceImpl.java` 的 `createGroupWork` 方法

```java
// Step 3: 创建用户发起拼团记录(使用新表)
OshGroupUserInitiated initiated = new OshGroupUserInitiated();
initiated.setUserId(userId);
initiated.setActivityId(dto.getActivityId()); // ✅ 新增:保存活动模板ID
initiated.setOrderId(order.getId());
initiated.setMinNum(dto.getMinNum());
initiated.setMaxNum(dto.getMaxNum());
initiated.setDuration(dto.getDuration());
initiated.setCustomPrice(dto.getPrice());
initiated.setGroupStatus(0); // 0-招募中(未成团)
initiated.setCurrentNum(1); // 发起人自己算1人
initiated.setInitiateTime(LocalDateTime.now());
initiated.setExpireTime(LocalDateTime.now().plusDays(7));
initiated.setCreateTime(LocalDateTime.now());
initiated.setUpdateTime(LocalDateTime.now());
```

**文件**: `OshGroupUserInitiated.java` 实体类

```java
/** 关联活动模板ID */
private Long activityId;

public Long getActivityId() {
    return activityId;
}

public void setActivityId(Long activityId) {
    this.activityId = activityId;
}
```

**文件**: `OshGroupUserInitiatedMapper.xml` 的 `insertUserInitiated` 方法

```xml
<insert id="insertUserInitiated" parameterType="com.backstage.system.domain.servergroup.OshGroupUserInitiated" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO osh_group_user_initiated (
        user_id,
        activity_id,  <!-- ✅ 新增 -->
        order_id,
        min_num,
        max_num,
        duration,
        custom_price,
        group_status,
        current_num,
        initiate_time,
        expire_time,
        create_time,
        update_time,
        delete_flag
    ) VALUES (
        #{userId},
        #{activityId},  <!-- ✅ 新增 -->
        #{orderId},
        #{minNum},
        #{maxNum},
        #{duration},
        #{customPrice},
        #{groupStatus},
        #{currentNum},
        #{initiateTime},
        #{expireTime},
        NOW(),
        NOW(),
        0
    )
</insert>
```

#### 2.1.3 优化查询SQL

**文件**: `OshGroupUserInitiatedMapper.xml` 的 `selectUserInitiatedActivityList`

```xml
<!-- 优化后:直接关联 activity_id,减少一次JOIN -->
<select id="selectUserInitiatedActivityList" parameterType="java.lang.Integer" resultMap="UserInitiatedActivityListVOResult">
    SELECT 
        gui.id,
        COALESCE(ga.title, '未知活动') AS title,
        COALESCE(ga.cpu, '待定') AS cpu,
        COALESCE(ga.memory, '待定') AS memory,
        COALESCE(ga.storage, '待定') AS storage,
        gui.custom_price AS base_price,
        gui.duration AS total_duration,
        gui.current_num,
        gui.min_num AS group_min_num,
        gui.max_num AS group_max_num,
        CASE 
            WHEN gui.group_status = 0 THEN 1
            WHEN gui.group_status = 1 THEN 2
            WHEN gui.group_status = 2 THEN 3
            ELSE 1
        END AS status,
        gui.initiate_time AS start_time,
        gui.server_start_time,
        gui.server_expire_time AS server_end_time,
        ga.server_tutorial_url,
        ga.cover
    FROM osh_group_user_initiated gui
    LEFT JOIN osh_group_activity ga ON gui.activity_id = ga.id  <!-- ✅ 直接关联 -->
    <where>
        gui.delete_flag = 0
        <if test="status != null">
            AND (
                CASE 
                    WHEN gui.group_status = 0 THEN 1
                    WHEN gui.group_status = 1 THEN 2
                    WHEN gui.group_status = 2 THEN 3
                    ELSE 1
                END
            ) = #{status}
        </if>
    </where>
    ORDER BY gui.initiate_time DESC
</select>
```

### 方案B: 完整重构方案(推荐长期使用) 🚀

**核心思路**: 明确分离"模板"和"实例"概念,重新设计表结构。

#### 2.2.1 表结构重构

```sql
-- 1. 创建纯模板表(从 osh_group_activity 拆分)
CREATE TABLE `osh_group_activity_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `title` varchar(200) NOT NULL COMMENT '模板标题',
    `type` varchar(30) NOT NULL DEFAULT 'server' COMMENT '类型:server-服务器',
    `cpu` varchar(50) NOT NULL COMMENT 'CPU配置',
    `memory` varchar(50) NOT NULL COMMENT '内存配置',
    `storage` varchar(100) NOT NULL COMMENT '存储配置',
    `base_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '基础价格',
    `total_duration` int NOT NULL COMMENT '总使用时长(月)',
    `group_min_num` int NOT NULL DEFAULT '2' COMMENT '最低成团人数',
    `group_max_num` int NOT NULL COMMENT '人数上限',
    `server_tutorial_url` varchar(500) DEFAULT NULL COMMENT '教程链接',
    `admin_contact` varchar(200) DEFAULT NULL COMMENT '管理员联系方式',
    `cover` varchar(500) DEFAULT NULL COMMENT '封面图片',
    `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '模板状态:1-启用 0-禁用',
    `created_by` bigint DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `delete_flag` tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动模板表';

-- 2. 创建用户发起实例表(增强版)
CREATE TABLE `osh_group_user_initiated_v2` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发起记录ID',
    `template_id` bigint NOT NULL COMMENT '关联模板ID → osh_group_activity_template.id',
    `user_id` bigint NOT NULL COMMENT '发起人用户ID',
    `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
    `min_num` int NOT NULL DEFAULT '2' COMMENT '最低成团人数(可自定义)',
    `max_num` int NOT NULL DEFAULT '5' COMMENT '最多成团人数(可自定义)',
    `duration` int NOT NULL DEFAULT '12' COMMENT '使用时长(月,可自定义)',
    `custom_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '自定义价格',
    `group_status` tinyint NOT NULL DEFAULT '0' COMMENT '组团状态:0-招募中 1-已成团 2-已取消',
    `current_num` int NOT NULL DEFAULT '1' COMMENT '当前参团人数',
    `server_ip` varchar(50) DEFAULT NULL COMMENT '服务器IP',
    `server_account` varchar(100) DEFAULT NULL COMMENT '服务器账号',
    `server_password` varchar(255) DEFAULT NULL COMMENT '服务器密码(加密)',
    `server_start_time` datetime DEFAULT NULL COMMENT '服务器开始时间',
    `server_expire_time` datetime DEFAULT NULL COMMENT '服务器到期时间',
    `initiate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    `expire_time` datetime DEFAULT NULL COMMENT '招募截止时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `delete_flag` tinyint(1) DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_group_status` (`group_status`),
    KEY `idx_initiate_time` (`initiate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户发起拼团实例表';

-- 3. 迁移数据
INSERT INTO osh_group_activity_template (
    id, title, type, cpu, memory, storage, base_price, total_duration,
    group_min_num, group_max_num, server_tutorial_url, admin_contact,
    cover, sort_order, status, create_time, update_time, delete_flag
)
SELECT 
    id, title, type, cpu, memory, storage, base_price, total_duration,
    group_min_num, group_max_num, server_tutorial_url, admin_contact,
    cover, sort_order, 
    CASE WHEN status IN (1,2) THEN 1 ELSE 0 END,  -- 转换状态
    create_time, update_time, delete_flag
FROM osh_group_activity
WHERE delete_flag = 0;
```

#### 2.2.2 方案B的优势

| 对比维度 | 方案A(最小改动) | 方案B(完整重构) |
|----------|-----------------|-----------------|
| 改动范围 | 小(加字段+改代码) | 大(新建表+迁移数据) |
| 实施风险 | 低 | 中 |
| 查询性能 | 中(仍需JOIN) | 高(直接关联) |
| 概念清晰度 | 中 | 高 |
| 可维护性 | 中 | 高 |
| 扩展性 | 低 | 高 |
| 实施周期 | 1-2天 | 1-2周 |

## 三、推荐实施方案

### 3.1 分阶段实施策略

**第一阶段(本周)**: 执行方案A
- ✅ 添加 `activity_id` 字段
- ✅ 修改创建拼团逻辑
- ✅ 优化查询SQL
- ✅ 添加索引

**第二阶段(下个月)**: 评估是否需要方案B
- 如果数据量 < 10万,继续使用方案A
- 如果数据量 > 10万 或 需要支持更多拼团类型,执行方案B

### 3.2 实施步骤(方案A)

#### Step 1: 数据库变更

```bash
# 1. 备份数据库
mysqldump -u root -p your_database osh_group_user_initiated osh_group_order > backup_$(date +%Y%m%d).sql

# 2. 执行SQL脚本
mysql -u root -p your_database < sql/2026/05/fix_group_table_structure.sql

# 3. 验证数据
mysql -u root -p your_database -e "SELECT COUNT(*) FROM osh_group_user_initiated WHERE activity_id = 0;"
```

#### Step 2: 代码修改

修改以下文件:
1. ✅ `OshGroupUserInitiated.java` - 添加 activityId 字段
2. ✅ `OshGroupUserInitiatedMapper.xml` - 修改 INSERT 和 SELECT
3. ✅ `OshGroupServerServiceImpl.java` - createGroupWork 方法添加 activityId

#### Step 3: 测试验证

```java
// 单元测试示例
@Test
public void testCreateGroupWithActivityId() {
    GroupCreateDTO dto = new GroupCreateDTO();
    dto.setActivityId(1L);
    dto.setMinNum(2);
    dto.setMaxNum(5);
    dto.setDuration(12);
    dto.setPrice(new BigDecimal("100.00"));
    
    GroupCreateVO result = groupServerService.createGroupWork(dto, 1001L);
    
    // 验证 activity_id 是否正确保存
    OshGroupUserInitiated initiated = userInitiatedMapper.selectById(result.getWorkId());
    assertEquals(1L, initiated.getActivityId());
}
```

#### Step 4: 性能验证

```sql
-- 执行 EXPLAIN 验证索引使用
EXPLAIN 
SELECT gui.id, ga.title, ga.cpu
FROM osh_group_user_initiated gui
LEFT JOIN osh_group_activity ga ON gui.activity_id = ga.id
WHERE gui.delete_flag = 0;

-- 预期结果:
-- - type 列应为 ref 或 eq_ref
-- - key 列应显示 idx_activity_id
-- - rows 列应显示较少的扫描行数
```

## 四、风险评估与回滚方案

### 4.1 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 历史数据 activity_id 回填失败 | 低 | 高 | 提前备份,分批更新 |
| 索引创建导致锁表 | 中 | 中 | 在低峰期执行,使用 ONLINE DDL |
| 代码遗漏 activityId 设置 | 中 | 高 | 代码Review + 单元测试 |
| 查询性能未达预期 | 低 | 中 | 提前压测,准备方案B |

### 4.2 回滚方案

```sql
-- 如果出现问题,执行回滚

-- 1. 删除新添加的字段
ALTER TABLE `osh_group_user_initiated` DROP COLUMN `activity_id`;
ALTER TABLE `osh_group_user_initiated` DROP INDEX `idx_activity_id`;
ALTER TABLE `osh_group_order` DROP INDEX `idx_group_activity_id`;

-- 2. 恢复数据库备份
mysql -u root -p your_database < backup_20260508.sql

-- 3. 回滚代码
git revert <commit-hash>
```

## 五、长期优化建议

### 5.1 数据归档策略

```sql
-- 将已完成/已取消的拼团记录归档到历史表
CREATE TABLE `osh_group_user_initiated_history` LIKE `osh_group_user_initiated`;

-- 每月执行一次归档
INSERT INTO osh_group_user_initiated_history
SELECT * FROM osh_group_user_initiated
WHERE group_status IN (1, 2) 
  AND initiate_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);

-- 删除已归档的数据
DELETE FROM osh_group_user_initiated
WHERE group_status IN (1, 2) 
  AND initiate_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);
```

### 5.2 缓存优化

```java
// 使用 Redis 缓存活动模板信息
@Cacheable(value = "activityTemplate", key = "#activityId")
public OshGroupActivity getActivityTemplate(Long activityId) {
    return groupServerMapper.selectGroupActivityById(activityId);
}
```

### 5.3 分库分表准备

当数据量超过100万时,考虑:
- 按 `user_id` 分表
- 按 `initiate_time` 按月分表
- 使用 ShardingSphere 中间件

## 六、总结

### 6.1 问题根因
- 业务模式从"管理员创建"演进到"用户发起",表结构未同步重构
- `osh_group_activity` 承担双重角色,导致概念混乱
- `osh_group_user_initiated` 缺少 `activity_id`,数据关联脆弱

### 6.2 修复方案
- **短期**: 方案A(添加 activity_id 字段) - 1-2天完成
- **长期**: 方案B(模板-实例分离) - 1-2周完成

### 6.3 预期收益
- ✅ 查询性能提升 50%+(减少一次JOIN)
- ✅ 数据关联稳定性提升(不依赖order表)
- ✅ 代码可维护性提升(概念清晰)
- ✅ 为未来扩展打下基础(支持更多拼团类型)

### 6.4 下一步行动
1. 立即执行方案A的数据库变更脚本
2. 修改Java代码,添加 activityId 字段
3. 编写单元测试验证功能
4. 执行性能测试验证索引效果
5. 评估是否需要执行方案B
