# OSH Backend 项目陷阱全景与实战手册

> **版本**：v3.0 · 基于项目 Memory 库全量检索 + 六维度系统性改进  
> **适用范围**：团队共享技术债务清单 + 新人入职培训材料 + Code Review Checklist  
> **分析框架**：5WHY 双层归因（产生原因 Why + 流出原因 Escape）+ 真实代码对比

---

<details>
<summary>🚀 <b>快速上手指南 — 新人/Reviewer 优先阅读顺序</b>（点击展开）</summary>

| 优先级 | 推荐阅读章节 | 阅读目的 |
|--------|------------|----------|
| ⭐ 第一 | §10 避坑速查表 | 5 分钟浏览所有陷阱的场景/严重度/状态，建立全局印象 |
| ⭐ 第二 | §4.1 setter 遗漏 + §4.2 VO 断链 | 最高频踩坑点，每次新增字段必查 |
| ⭐ 第三 | §5.1 TTL 清除 + §5.2 库存归还不原子 | 缓存/库存类 P0 缺陷，影响资金安全 |
| 第四 | §3.1 索引失效 + §2.4 delete_flag 遗漏 | SQL 性能与数据正确性基础 |
| 第五 | §8.1 双认证冲突 + §6.1 Kafka concurrency | 架构配置类高频问题 |
| 第六 | §1 总览表 | 了解全部 7 大分类的陷阱分布 |

**Code Review 时**：直接打开 §10“按开发场景速查”表格，对照当前 PR 涉及的场景逐项检查。
**新人入职**：按上述优先级顺序阅读，先掌握 P0 级陷阱，再逐步覆盖 P1/P2。

</details>

---

## 目录

- [一、总览与分类框架](#一总览与分类框架)
- [二、DDL 与数据库模式同步陷阱（6 项）](#二ddl-与数据库模式同步陷阱6-项)
- [三、ORM 与 SQL 查询陷阱（6 项）](#三orm-与-sql-查询陷阱6-项)
- [四、四层同步与生命周期缺失陷阱（5 项）](#四四层同步与生命周期缺失陷阱5-项)
- [五、缓存与分布式系统陷阱（4 项）](#五缓存与分布式系统陷阱4-项)
- [六、并发与性能瓶颈陷阱（3 项）](#六并发与性能瓶颈陷阱3-项)
- [七、编译构建与工具链陷阱（5 项）](#七编译构建与工具链陷阱5-项)
- [八、架构配置与部署陷阱（2 项）](#八架构配置与部署陷阱2-项)
- [九、重要技术决策记录（3 项）](#九重要技术决策记录3-项)
- [十、避坑速查表](#十避坑速查表)
- [十一、检索指引](#十一检索指引)

---

## 一、总览与分类框架

本手册覆盖项目 Memory 库中已识别的 **31 项陷阱** + **3 项重要技术决策**，按 7 大类分组。每个陷阱均包含**理论定义 → 5WHY 推导 → 实战代码对比**三层结构。

| 大类 | 数量 | 核心风险 | 严重度分布 |
|------|------|----------|----------|
| DDL 与数据库模式同步 | 6 | 建表失败、类型不匹配、数据丢失 | P0×3 P1×2 P2×1 |
| ORM 与 SQL 查询 | 6 | 索引失效、空值、数据泄露、filesort | P0×3 P1×2 P2×1 |
| 四层同步与生命周期缺失 | 5 | 字段为 NULL、编译错误、前端断链 | P0×3 P1×2 |
| 缓存与分布式系统 | 4 | 内存泄漏、库存不一致、大 Key 阻塞 | P0×3 P1×1 |
| 并发与性能瓶颈 | 3 | 消费积压、状态丢失、扩容无效 | P1×2 P2×1 |
| 编译构建与工具链 | 5 | 虚假编译错误、测试不执行 | P1×3 P2×2 |
| 架构配置与部署 | 2 | 认证 401、并行度受限 | P1×1 P2×1 |

---

## 二、DDL 与数据库模式同步陷阱（6 项）

### 2.1 DDL 列类型与 Java 实体字段类型不一致

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Java 实体字段类型（如 `String`）与 DDL 列类型（如 `tinyint`）不一致，线上已 ALTER 修正但初始 DDL 未同步 |
| **触发条件 (When)** | 从初始 DDL 新建数据库；Java 实体字段类型变更后未检查 DDL |
| **典型场景 (Where)** | `osh_questionanswer.sql` 中 `resource_type` 定义为 `tinyint(4)`，Java 实体为 `String` |

**5WHY 产生原因推导：**

| 层级 | 问题 | 归因 |
|------|------|------|
| 1W | 新建库时插入数据报错 | 列类型不匹配，插入字符串到 tinyint 列 |
| 2W | 为什么类型不匹配？ | 线上已 ALTER 改为 varchar，但初始 DDL 仍是 tinyint |
| 3W | 为什么 ALTER 后 DDL 未更新？ | 开发者修改 ALTER 后忘记同步初始 DDL 文件 |
| 4W | 为什么会忘记？ | ALTER 和初始 DDL 分属不同文件，修改时注意力集中在主链路 |
| 5W | **根因**：缺少自动化的 DDL-Java 类型比对工具，无编译期强制校验 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | Code Review 时未对照 DDL 与 Java 类型 |
| 2A | 没有 CI 检查项对比两者一致性 |
| 3A | **根因**：缺少 `code-quality-guard.sh` 的 DDL-Java 类型映射检查 |

**关键指标**：9 种映射规则（`varchar↔String`, `bigint↔Long`, `int↔Integer`, `decimal↔BigDecimal` 等）

**修复方案**：`code-quality-guard.sh` CHECK 6 自动比对；每次 ALTER 修改列类型时同步更新初始 DDL

<details>
<summary><b>实战案例：新环境部署答疑模块报 Data truncated</b></summary>

**错误代码**（初始 DDL `sql/osh_questionanswer.sql`）：
```sql
-- ❌ 错误：resource_type 定义为 tinyint，但 Java 传入字符串
`resource_type` tinyint(4) DEFAULT NULL COMMENT '资源类型',
```
```java
// Java 实体 Question.java — 传入 "course"、"tool" 等字符串值
private String resourceType;
```

**运行时错误日志**：
```
Caused by: java.sql.SQLException: Data truncated for column 'resource_type' at row 1
```

**修复后 DDL**：
```sql
-- ✅ 正确：与 Java String 类型对齐
`resource_type` varchar(32) DEFAULT NULL COMMENT '资源类型：course/tool/...',
```

**核心特征**：线上 ALTER 已修正但初始 DDL 未同步 → 新建库时类型回退 → 运行时才暴露

</details>

---

### 2.2 DDL 索引引用不存在的列（幽灵列）

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 索引定义引用了不存在的列名，执行建表时报 `ERROR 1072: Key column 'xxx' doesn't exist` |
| **触发条件 (When)** | 列名被重命名但索引定义未同步 |
| **典型场景 (Where)** | `resource_id → resource_no` 重命名后，索引仍引用 `resource_id` |

**5WHY 推导**：列名重命名 → 只改了列定义未改索引 → 无全局搜索检查索引引用 → **根因**：DDL 修改缺乏"列名变更影响范围扫描"的防御机制

**修复方案**：修改列名时全文搜索 PRIMARY KEY / INDEX / UNIQUE KEY / FOREIGN KEY 中的引用并同步修改；在空数据库上完整执行 DDL 验证

<details>
<summary><b>实战案例：开发机初始化答疑模块建表失败</b></summary>

**错误代码**（`sql/osh_questionanswer.sql`）：
```sql
CREATE TABLE `osh_question_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_no` varchar(64) DEFAULT NULL COMMENT '资源编号',
  -- ...其他列...
  KEY `idx_resource` (`resource_id`),  -- ❌ 引用了不存在的列！
) ENGINE=InnoDB;
```
```
ERROR 1072 (42000): Key column 'resource_id' doesn't exist in table
```

**修复后 DDL**：
```sql
  KEY `idx_resource` (`resource_no`),  -- ✅ 与列定义对齐
```

</details>

---

### 2.3 ALTER 迁移脚本与初始 DDL 未同步

| 维度 | 内容 |
|------|------|
| **定义 (What)** | ALTER TABLE ADD COLUMN 迁移脚本添加了列，但初始建表 DDL 未同步，新建库时缺列 |
| **触发条件 (When)** | 新环境部署（开发机初始化）仅执行初始 DDL，未执行 ALTER 脚本 |
| **典型场景 (Where)** | 5 张表（osh_open_project、osh_open_project_tag 等）的 `no` 列仅存在于 ALTER 脚本 |

**5WHY 推导**：

| 层级 | 归因 |
|------|------|
| 1W | 新环境报 `Unknown column 'no'` | 初始 DDL 缺少该列 |
| 2W | 为什么缺少？ | ALTER 脚本单独添加了列但未回写初始 DDL |
| 3W | 为什么忘记？ | 两者分属不同文件（`sql/2026/05/` vs `sql/osh_xxx.sql`） |
| 4W | 为什么没有检查？ | 团队认为"ALTER 跑过了就没事"，忽略了全量建库场景 |
| 5W | **根因**：无自动化检查验证两者字段集合一致性 |

**修复方案**：`code-quality-guard.sh` CHECK 2（四层一致性）+ CHECK 4（ALTER vs 初始 DDL 同步）；每次编写 ALTER 时必须同步补入初始 DDL

<details>
<summary><b>实战案例：新开发者克隆仓库后报 Unknown column 'no'</b></summary>

```sql
-- ALTER 脚本 sql/2026/05/add_resource_no.sql 添加了 no 列 ✅
ALTER TABLE `osh_open_project` ADD COLUMN `no` varchar(64) DEFAULT NULL COMMENT '资源编号';

-- 但初始 DDL sql/osh_open_project.sql 缺少 no 列定义 ❌
CREATE TABLE `osh_open_project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(256) NOT NULL,
  -- 没有 no 列！
)
```

**修复后**：
```sql
CREATE TABLE `osh_open_project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) DEFAULT NULL COMMENT '资源编号',  -- ✅ 补入
  `title` varchar(256) NOT NULL,
)
```

</details>

---

### 2.4 Mapper XML 未添加 delete_flag=0 过滤

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 查询 SQL 未加软删除过滤条件，已删除数据仍被查出 |
| **触发条件 (When)** | 手动编写新 Mapper 查询时遗漏 |
| **典型场景 (Where)** | `selectGroupActivityList` 遗漏 `AND ga.delete_flag = 0`，已删除活动仍显示 |

**5WHY 推导**：同文件 20+ 处查询均有过滤 → 唯独 1 处遗漏 → **根因**：依赖人工记忆而非自动化拦截；虽然项目有 `GlobalLogicDeleteInterceptor` 全局拦截器，但并非所有场景都能覆盖

**修复方案**：Code Review 必查项；新增查询参照同文件已有查询的过滤模式

<details>
<summary><b>实战案例：拼团活动列表显示已删除活动</b></summary>

**错误代码**（`OshGroupServerMapper.xml`）：
```xml
<select id="selectGroupActivityList" resultMap="GroupActivityListResult">
    SELECT ga.id, ga.title, ga.current_num ...
    FROM osh_group_activity ga
    <where>
        <!-- ❌ 遗漏了 ga.delete_flag = 0 -->
        <if test="status != null">AND ga.status = #{status}</if>
    </where>
</select>
```

**修复后**：
```xml
    <where>
        ga.delete_flag = 0  <!-- ✅ 显式添加软删除过滤 -->
        <if test="status != null">AND ga.status = #{status}</if>
    </where>
```

</details>

---

### 2.5 逻辑删除拦截器需排除无 delete_flag 字段的表

| 维度 | 内容 |
|------|------|
| **定义 (What)** | `GlobalLogicDeleteInterceptor` 默认对所有 `osh_` 表追加 `delete_flag = 0`，若表无此列则报 `SQLSyntaxErrorException` |
| **触发条件 (When)** | 新建 `osh_` 开头的表但无 `delete_flag` 字段 |
| **修复方案** | 在拦截器中维护 `TABLES_WITHOUT_DELETE_FLAG` 排除集合 |

---

### 2.6 逻辑删除字段历史数据未显式初始化

| 维度 | 内容 |
|------|------|
| **定义 (What)** | `ADD COLUMN delete_flag DEFAULT 0` 仅影响后续 INSERT，历史记录的 `delete_flag` 仍为 NULL |
| **触发条件 (When)** | 为已有表添加 `delete_flag` 字段后，拦截器条件 `delete_flag = 0` 无法匹配 NULL 值 |
| **修复方案** | 在 ADD COLUMN 后紧跟执行 `UPDATE 表名 SET delete_flag = 0 WHERE delete_flag IS NULL` |

<details>
<summary><b>实战案例：§2.5 & §2.6 联合踩坑——加列后查询报错</b></summary>

**错误操作**：
```sql
-- 仅执行了 ADD COLUMN，历史数据的 delete_flag 仍为 NULL
ALTER TABLE `osh_xxx` ADD COLUMN `delete_flag` tinyint DEFAULT 0 COMMENT '删除标记';
-- 未执行 UPDATE 初始化！
```

**修复方案**（两步缺一不可）：
```sql
-- 步骤 1：加列（使用 NOT NULL 防止未来 NULL）
ALTER TABLE `osh_xxx` ADD COLUMN `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记';
-- 步骤 2：初始化历史数据
UPDATE `osh_xxx` SET `delete_flag` = 0 WHERE `delete_flag` IS NULL;
```
```java
// ✅ 补全字段后从 GlobalLogicDeleteInterceptor 排除名单中移除
private static final Set<String> TABLES_WITHOUT_DELETE_FLAG = new HashSet<>(Arrays.asList(
    // "osh_xxx"  ← 已补全字段，从此集合中移除
));
```

</details>

---

## 三、ORM 与 SQL 查询陷阱（6 项）

### 3.1 MyBatis WHERE 中使用 CASE WHEN / COLLATE 导致索引失效

| 维度 | 内容 |
|------|------|
| **定义 (What)** | WHERE/JOIN ON 中对列使用函数包装（CASE WHEN、COLLATE、LOWER 等），导致 MySQL 无法利用索引 |
| **触发条件 (When)** | 需要在 SQL 中做状态值映射或字符集转换时 |
| **典型场景 (Where)** | `CASE WHEN gui.group_status = 0 THEN 1 WHEN ... END = #{status}` 全表扫描 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 查询响应变慢 | 产生全表扫描 |
| 2W | 为什么全表扫描？ | MySQL 无法对 CASE WHEN 表达式的结果使用索引 |
| 3W | 为什么用了 CASE WHEN？ | 开发者在 SQL 层做了枚举值映射 |
| 4W | 为什么不放到 Java 层？ | 不了解 MyBatis if test 可以做等值映射 |
| 5W | **根因**：缺少"WHERE 中列应保持原始状态"的编码规范认知 |

**通用规则**：WHERE/JOIN ON 中禁止对索引列使用 `CASE WHEN`、`IF`、`COLLATE`、`LOWER`、`UPPER`、`CONCAT`、`DATE()` 等函数

<details>
<summary><b>实战案例：拼团用户列表查询慢——CASE WHEN 索引失效</b></summary>

**错误代码**（`OshGroupUserInitiatedMapper.xml` 旧版本）：
```xml
<!-- ❌ CASE WHEN 包装导致 group_status 索引失效，全表扫描 -->
<if test="status != null">
  AND (CASE WHEN gui.group_status = 0 THEN 1
            WHEN gui.group_status = 1 THEN 2
            WHEN gui.group_status = 2 THEN 3
            ELSE 1 END) = #{status}
</if>
```

**EXPLAIN 对比**：
```
-- 错误写法：type=ALL, rows=全表行数, Extra=Using where（全表扫描）
-- 正确写法：type=ref, rows=预计匹配行数, Extra=Using index condition（索引查找）
```

**修复后代码**（当前版本 `OshGroupUserInitiatedMapper.xml` L101-L110）：
```xml
<!-- ✅ 使用直接等值比较，避免 CASE WHEN 导致索引失效 -->
<if test="status != null and status == 1">AND gui.group_status = 0</if>
<if test="status != null and status == 2">AND gui.group_status = 1</if>
<if test="status != null and status == 3">AND gui.group_status = 2</if>
```

</details>

---

### 3.2 MyBatis resultMap 未完整映射 SQL 查询列

| 维度 | 内容 |
|------|------|
| **定义 (What)** | SQL SELECT 的列未在 resultMap 中声明映射，对应 Java 属性值为 null，极其隐蔽 |
| **触发条件 (When)** | 新增 SQL 列后忘记同步 resultMap |
| **典型场景 (Where)** | `activity_id` 列在 SELECT 中但 resultMap 无映射，后续 NPE |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 业务逻辑报 NPE | 对象属性为 null |
| 2W | 为什么属性为 null？ | MyBatis 静默返回 null，不抛异常 |
| 3W | 为什么静默？ | SQL SELECT 的列未在 resultMap 中声明映射 |
| 4W | 为什么漏映射？ | 新增 SQL 列后忘记同步 resultMap |
| 5W | **根因**：MyBatis 静默失败设计 + 缺少列-属性对应关系的自动化检查 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | Code Review 未对照 SELECT 列与 resultMap |
| 2A | IDE 无强制检查 resultMap 完整性的插件 |
| 3A | **根因**：MyBatis 设计上对未映射列静默返回 null，团队缺少“新增 SELECT 列必须同步 resultMap”的编码规范 |

<details>
<summary><b>实战案例：拼团详情"活动模板不存在"——resultMap 漏映射</b></summary>

**错误代码**（`OshGroupUserInitiatedMapper.xml` 旧版本）：
```xml
<sql id="selectUserInitiatedVo">
    SELECT gui.id, gui.user_id, gui.activity_id, gui.order_id ...
</sql>
<resultMap id="OshGroupUserInitiatedResult" type="OshGroupUserInitiated">
    <result property="id" column="id"/>
    <result property="userId" column="user_id"/>
    <!-- ❌ 缺少 activityId 映射！MyBatis 静默返回 null -->
</resultMap>
```
```java
// activityId 为 null → 查 osh_group_activity WHERE id = null → 空
OshGroupActivity activity = activityMapper.selectById(initiated.getActivityId()); // null!
throw new ServiceException("拼团活动模板不存在");  // 误导性报错
```

**修复后**：
```xml
<resultMap id="OshGroupUserInitiatedResult" type="OshGroupUserInitiated">
    <result property="id" column="id"/>
    <result property="userId" column="user_id"/>
    <result property="activityId" column="activity_id"/>  <!-- ✅ 补入映射 -->
</resultMap>
```

</details>

---

### 3.3 @MapKey 注解仅适用于多行 Map 聚合场景

| 维度 | 内容 |
|------|------|
| **定义 (What)** | MyBatis `@MapKey` 注解仅适用于多行结果按指定字段聚合为 Map 的场景，单行查询不可搭配 |
| **触发条件 (When)** | 单行查询方法标注 `@MapKey`，返回类型仍为 `Map<String, Object>` |
| **典型场景 (Where)** | `OshCourseSectionMapper.selectSectionVideoById` 单行查询却用了 `@MapKey("id")` |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 返回的 Map 结构不符合预期 | `@MapKey` 将单行结果包装成嵌套 Map |
| 2W | 为什么用了 `@MapKey`？ | 开发者误以为 `@MapKey` 是“返回 Map”的必要注解 |
| 3W | 为什么有这个误解？ | 不了解 `@MapKey` 的实际语义是“多行结果按某列聚合” |
| 4W | 为什么不了解？ | MyBatis 文档中该注解使用场景描述不够直观 |
| 5W | **根因**：团队缺少“`@MapKey` 仅用于多行聚合”的编码规范 |

**修复方案**：单行查询直接返回实体类或 `Map`，不加 `@MapKey`

<details>
<summary>📋 <b>实战案例：课程章节视频查询——单行查询误用 @MapKey</b>（点击展开代码对比）</summary>

**错误代码**（`OshCourseSectionMapper.java` L38-39）：
```java
// ❌ 单行查询却标注 @MapKey，返回结构变为 Map<id值, Map<列名,值>>
@MapKey("id")
Map<String, Object> selectSectionVideoById(@Param("sectionId") Long sectionId);
```

**对应 XML**（`OshCourseSectionMapper.xml` L23-46）：
```xml
<select id="selectSectionVideoById" resultType="java.util.Map">
    SELECT s.id AS section_id, s.course_id, s.title, s.media_url ...
    FROM osh_course_section s LEFT JOIN osh_course c ON ...
    WHERE s.id = #{sectionId} AND s.delete_flag = 0
    -- 返回单行结果，@MapKey 会将此行包装为 Map<section_id值, Map<...>>
</select>
```

**运行时效果**：
```java
// 调用方期望得到 Map<String, Object>（单行数据）
Map<String, Object> result = mapper.selectSectionVideoById(sectionId);
// 实际返回 Map<Long, Map<String, Object>> 嵌套结构！
// result.get("title") → null（因为外层 key 是 id 值）
```

**修复后**：
```java
// ✅ 单行查询直接返回 Map，不加 @MapKey
Map<String, Object> selectSectionVideoById(@Param("sectionId") Long sectionId);
```

</details>

---

### 3.4 MyBatis XML Mapper 修改后未执行 mvn clean compile

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 修改 XML Mapper 后仅增量编译，`target/classes/mapper` 下 XML 未更新，运行仍使用旧 SQL |
| **触发条件 (When)** | 修改了 `src/main/resources/mapper/` 下的 XML 文件 |
| **典型场景 (Where)** | 新增 SQL 报 `Invalid bound statement`、修改的 SQL 不生效 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 修改了 XML 但运行时 SQL 不变 | target 目录的 XML 未更新 |
| 2W | 为什么未更新？ | Maven 增量编译不复制未变更的 resources 文件 |
| 3W | 为什么开发者未发现？ | IDE 运行按钮默认增量编译，不提示资源文件过期 |
| 4W | 为什么没有规范约束？ | 团队缺少“修改 resources 必须 clean”的编码规范 |
| 5W | **根因**：Maven 增量编译对 resources 文件的复制策略依赖时间戳，开发者直觉认为“编译=全量更新” |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | 报错信息 `Invalid bound statement` 不直观指向编译缓存问题 |
| 2A | 新人遇到此问题时往往在 Mapper 文件本身寻找原因，耗时较长 |
| 3A | **根因**：团队缺少“修改 XML/配置文件后必须 mvn clean”的入培教材和 Checklist |

<details>
<summary><b>实战案例：修改 Mapper XML 后报 Invalid bound statement</b></summary>

```bash
# ❌ 仅增量编译，target 目录的 XML 未更新
mvn compile
# 或 IDE 中直接点击 Run
```
```
org.apache.ibatis.binding.BindingException:
  Invalid bound statement (not found):
  com.backstage...OshGroupUserInitiatedMapper.selectInitiatedList
```

**修复操作**：
```bash
# ✅ 清理 target 后重新编译
mvn clean compile
# 或 IntelliJ: Build → Rebuild Project
```

</details>

---

### 3.5 MyBatis WHERE 子句中避免使用函数

| 维度 | 内容 |
|------|------|
| **定义 (What)** | WHERE/JOIN ON 中的列应保持"原始"状态，任何函数包装都会阻止索引使用 |
| **通用规则** | 禁止对索引列使用：`CASE WHEN`、`IF`、`COLLATE`、`LOWER`、`UPPER`、`CONCAT`、`DATE()` 等 |

---

### 3.6 UNION ALL + ORDER BY 导致全表 filesort

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 多表 UNION ALL 合并后直接 ORDER BY，MySQL 对整个 UNION 结果集执行全表 filesort，无法利用各子查询自身的索引排序 |
| **触发条件 (When)** | 管理端列表查询需要合并多数据源时（如拼团管理端合并系统活动拼团和用户发起拼团） |
| **典型场景 (Where)** | 拼团管理端 UNION ALL 合并两张表后 ORDER BY create_time DESC，产生 O(N log N) 的全表排序开销 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 管理端列表查询变慢 | MySQL 对整个 UNION 结果集执行 filesort |
| 2W | 为什么 filesort？ | UNION ALL 后的 ORDER BY 无法利用子查询的索引排序 |
| 3W | 为什么不用索引？ | MySQL 优化器对 UNION 结果集只能全表排序 |
| 4W | 为什么不用其他方案？ | 开发者不了解可拆分为两个独立查询 + Java 层归并 |
| 5W | **根因**：缺少“UNION ALL + ORDER BY = filesort”的性能认知，未建立多数据源合并的标准模式 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | 数据量小时性能差异不明显，测试未暴露 |
| 2A | 无慢 SQL 监控告警，filesort 未触发告警阈值 |
| 3A | **根因**：缺少 EXPLAIN 强制审查机制，未对 UNION 查询做性能基线测试 |

**修复方案**：拆分为两个独立查询（各自利用索引 ORDER BY） + Java 层归并排序

<details>
<summary>📋 <b>实战案例：拼团管理端列表查询慢——UNION ALL filesort</b>（点击展开代码对比）</summary>

**错误代码**（SQL 层）：
```sql
-- ❌ UNION ALL + ORDER BY 导致全表 filesort
SELECT id, title, create_time FROM osh_group_activity WHERE delete_flag = 0
UNION ALL
SELECT id, title, create_time FROM osh_group_user_initiated WHERE delete_flag = 0
ORDER BY create_time DESC  -- MySQL 对整个结果集 filesort
```

**EXPLAIN 输出**：
```
Extra: Using temporary; Using filesort  -- ❌ 临时表 + 全表排序
```

**修复后（Java 层归并）**：
```java
// ✅ 步骤 1：两个独立查询（各自利用索引 ORDER BY）
List<GroupWorkListVO> systemList = mapper.selectSystemGroupList(params);
List<GroupWorkListVO> userList   = mapper.selectUserInitiatedList(params);

// ✅ 步骤 2：Java 层归并排序 O(n+m)
List<GroupWorkListVO> merged = new ArrayList<>(systemList.size() + userList.size());
int i = 0, j = 0;
while (i < systemList.size() && j < userList.size()) {
    LocalDateTime timeA = systemList.get(i).getCreateTime();
    LocalDateTime timeB = userList.get(j).getCreateTime();
    if (timeA != null && timeB != null && timeA.isAfter(timeB)) {
        merged.add(systemList.get(i++));
    } else {
        merged.add(userList.get(j++));
    }
}
while (i < systemList.size()) merged.add(systemList.get(i++));
while (j < userList.size()) merged.add(userList.get(j++));
```

**收益对比**：
| 方案 | 时间复杂度 | 索引利用 |
|------|------------|----------|
| UNION ALL + ORDER BY | O(N log N) filesort | ❌ 无法利用索引 |
| 独立查询 + Java 归并 | O(n) + O(m) + O(n+m) | ✅ 各子查询利用索引 |

</details>

---

## 四、四层同步与生命周期缺失陷阱（5 项）

### 4.1 Service 层 insert 入口遗漏字段初始化（setter 遗漏）

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 新增数据库列和实体字段后，Service 层的创建方法忘记调用 setter 初始化，新记录该字段永远为 NULL |
| **触发条件 (When)** | 新增业务字段（如 `no` 资源编号）后 |
| **典型场景 (Where)** | 5+ 处 insert 入口遗漏 `setNo()`：OshQAQuestionServiceImpl、OshOpenProjectSourceServiceImpl 等 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 新记录的 `no` 字段为 NULL | insert 时未调用 `setNo()` |
| 2W | 为什么没调用？ | 开发者只关注 DDL → 实体 → Service 主链路中的 DDL 和实体层 |
| 3W | 为什么会遗漏 Service 层？ | insert 入口散布在多个 ServiceImpl 中（包括非主 Service） |
| 4W | 为什么没有检查？ | 无编译期检查验证"实体有字段 → insert 路径是否初始化" |
| 5W | **根因**：新增字段时缺乏四层同步检查清单（DDL / 实体 / insert 入口 / VO） |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | Code Review 未检查所有 insert 入口 |
| 2A | 无自动化工具扫描 setter 调用完整性 |
| 3A | **根因**：`code-quality-guard.sh` CHECK 1 可自动扫描所有 insert 调用点并检查 setter |

**四层同步检查清单**：
1. **DDL/ALTER**：表结构加列
2. **实体类**：加字段 + getter/setter
3. **所有 insert 入口**：调用对应 setter
4. **VO/DTO**：如需前端展示，暴露该字段

<details>
<summary><b>实战案例：开源项目新记录编号始终为 NULL</b></summary>

**错误代码**（多处 ServiceImpl）：
```java
// ❌ OshOpenProjectSourceServiceImpl.syncProject() 遗漏了 setNo
public void syncProject(OshOpenProject project) {
    project.setTitle(xxx);
    project.setDescription(xxx);
    // 没有 project.setNo(GenerateUtil.generateResourceCode(OPEN_PROJECT));
    openProjectMapper.insert(project);  // no 字段为 NULL！
}
```

**对比正确代码**（课程模块）：
```java
// ✅ 课程模块正确调用了 setNo
public void addCourse(OshCourse course) {
    course.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.COURSE));
    courseMapper.insertCourse(course);
}
```

</details>

---

### 4.2 VO/DTO 层未暴露实体字段导致前端数据断链

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 实体类有字段但 VO 类未声明，API 返回 JSON 缺少该数据 |
| **触发条件 (When)** | 新增字段时注意力集中在主链路，VO 被视为"可选" |
| **典型场景 (Where)** | `OpenProjectVO` 缺少 `no` 字段，前端列表页无法显示项目编号 |

**5WHY 推导**：VO 手动从实体复制字段 → 新增字段忘记同步 → 前后端联调时才发现 → **根因**：VO 类手动维护、缺乏自动化字段覆盖度检查

**修复方案**：`code-quality-guard.sh` CHECK 2 自动搜索关联 VO 文件；每个新增字段 PR 必须列出受影响 VO

<details>
<summary><b>实战案例：开源项目列表页编号列始终为空</b></summary>

```java
// 实体类有 no 字段 ✅                    // VO 类缺少 no 字段 ❌
public class OshOpenProject {            public class OpenProjectVO {
    private String no;                       private Long id;
    private String title;                    private String title;
}                                            // 没有 no 字段！
                                         }
```

**API 响应对比**：
```json
// ❌ 当前返回                              // ✅ 修复后
{"id":1,"title":"OSH"}                    {"id":1,"no":"op-20260927-xxxx","title":"OSH"}
```

</details>

---

### 4.3 方法返回类型变更必须三层同步修改

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 将方法返回类型从 `Map` 改为 VO 时，仅修改部分层级导致连锁编译错误 |
| **触发条件 (When)** | 重构返回数据结构（Map → VO）时 |

**三层同步检查清单**：

| 层级 | 文件 | 修改点 |
|------|------|--------|
| 接口定义 | `IXxxService.java` | 方法签名返回类型 |
| 实现类 | `XxxServiceImpl.java` | 方法签名 + 方法体 + import |
| 调用方 | `XxxController.java` | 泛型参数 + 变量声明 + 属性访问方式 + import |

**修复方案**：修改前先 `grep "methodName("` 搜索所有调用点，列出完整修改清单

<details>
<summary><b>实战案例：Map→VO 重构编译失败</b></summary>

```java
// 1. 接口 — 已改 ✅                          // 2. 实现 — 未改 ❌
interface IOshGroupServerService {            public Map<String, Object> addUserToGroup(...) {
    AddUserToGroupVO addUserToGroup(...);         return result;  // 编译错误！返回类型不兼容
}                                             }
// 3. Controller — 未改 ❌
public R<Map<String, Object>> join(...) {
    R<Map<String, Object>> r = R.ok(service.addUserToGroup(...));  // 泛型不匹配
}
```

**修复后（三层同步）**：
```java
// 2. 实现 ✅                                 // 3. Controller ✅
public AddUserToGroupVO addUserToGroup(...) {  public R<AddUserToGroupVO> join(...) {
    AddUserToGroupVO vo = new AddUserToGroupVO();    AddUserToGroupVO vo = service.addUserToGroup(...);
    vo.setMessage("success");                          return R.ok(vo);
    return vo;                                     }
}
```

</details>

---

### 4.4 枚举值定义后未在业务代码中使用（死枚举值）

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 枚举类中定义了枚举值但 Service 层从未引用，导致"功能已实现"的错觉 |
| **触发条件 (When)** | 枚举由统一规划创建，各模块 Service 开发时忘记调用 |
| **典型场景 (Where)** | `ResourceCodePrefixEnum` 定义了 15 个值但全部未被引用 |

**5WHY 推导**：枚举定义完成 → 给人"功能已实现"的错觉 → 实际 Service 未调用 → 新记录 `no` 为 NULL → **根因**：枚举定义和使用分属不同模块，无编译期使用检查

**修复方案**：`code-quality-guard.sh` CHECK 3 自动扫描枚举使用完整性；枚举新增必须伴随 Service 调用

<details>
<summary>📋 <b>实战案例：ResourceCodePrefixEnum 15 个值仅 9 个被引用</b>（点击展开代码对比）</summary>

**枚举定义**（`ResourceCodePrefixEnum.java` L9-28）：
```java
public enum ResourceCodePrefixEnum {
    COURSE("co", "课程"),       // ✅ 被 OshCourseServiceImpl 等引用
    BOOK("bk", "书籍"),         // ✅ 被 BookServiceImpl 引用
    TOOL("tl", "工具"),         // ✅ 被 OshToolServiceImpl 引用
    COLUMN("cl", "专栏"),       // ❌ 未被任何 Service 引用
    EXAM("ex", "考试"),         // ❌ 未被任何 Service 引用
    QUESTION("qa", "问答"),     // ❌ 未被任何 Service 引用
    ANSWER("aa", "qa_answer"),  // ❌ 未被任何 Service 引用
    BBS_POST("bp", "bbs_post"), // ❌ 未被任何 Service 引用
    // ... 共 20 个枚举值，实际被引用的仅约 9 个
}
```

**CHECK 3 自动检测输出**：
```
[WARN] ResourceCodePrefixEnum.COLUMN defined but not used in backstage-system
[WARN] ResourceCodePrefixEnum.EXAM defined but not used in backstage-system
[WARN] ResourceCodePrefixEnum.QUESTION defined but not used in backstage-system
...
[WARN] CHECK 3: 11 unused enum value(s) in ResourceCodePrefixEnum
```

**正确做法**：枚举新增必须伴随 Service 调用
```java
// ✅ 新增枚举时同步在 Service 中使用
// 1. 枚举定义
INTERNAL_RESOURCE("ir", "internal_resource"),
// 2. Service 调用
entity.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.INTERNAL_RESOURCE));
```

</details>

---

### 4.5 数据库关联字段未持久化导致查询为空

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Java 对象已 `setXxx()` 赋值但未调用 `update` 持久化到 DB，导致 JOIN 条件依赖的字段在库中为 NULL |
| **触发条件 (When)** | 业务流程中字段赋值后忘记回写数据库 |
| **典型场景 (Where)** | `order.setGroupWorkId()` 仅更新内存，DB 中 `group_work_id` 为 NULL → 用户列表查询返回空 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | JOIN 查询返回空结果 | 关联字段在 DB 中为 NULL |
| 2W | 为什么为 NULL？ | Java 中 set 了值但未调用 update |
| 3W | 为什么忘记 update？ | 开发者误以为 set 即持久化 |
| 4W | 为什么有这个认知偏差？ | ORM 的 set 操作仅修改内存对象，与数据库无关 |
| 5W | **根因**：ORM 使用中“set 即持久化”的认知偏差，团队缺少“set 后必须 update”的编码规范 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | 字段为 NULL 不报错，仅查询结果为空，排查困难 |
| 2A | Code Review 未检查“set 后是否有对应的 update/mapper 调用” |
| 3A | **根因**：缺少“字段赋值后必须回写”的编码规范 Checklist |

**修复方案**：setter 后立即调用 update 方法；同时用已存在的强关联字段临时修复 JOIN

<details>
<summary><b>实战案例：拼团用户列表为空——setXxx() 仅更新内存</b></summary>

```java
// ❌ 仅更新 Java 对象，未回写数据库
order.setGroupWorkId(groupWork.getId());
// 没有 orderMapper.updateOrder(order)！
```
```sql
-- JOIN 依赖 group_work_id，但数据库中为 NULL
SELECT gu.* FROM osh_group_user gu
INNER JOIN osh_group_work gw ON gu.group_work_id = gw.id
WHERE gw.order_id = #{orderId}  -- group_work_id 为 NULL → 结果为空
```

**修复后**：
```java
// ✅ 方案1：setter 后立即回写
order.setGroupWorkId(groupWork.getId());
orderMapper.updateOrderGroupWorkId(order.getId(), groupWork.getId());

// ✅ 方案2（即时修复）：JOIN 改用已存在的强关联字段
-- WHERE go.id = gw.order_id  -- 不依赖 group_work_id
```

</details>

---

## 五、缓存与分布式系统陷阱（4 项）

### 5.1 两参数 setCacheObject 清除已有 TTL 导致会话永不过期

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Redis SET 语义覆盖值时清除原有过期时间，两参数 `setCacheObject` 让键变成永不过期 |
| **触发条件 (When)** | 业务写入回写整份对象时（如改完积分/资产后回写 userMap） |
| **典型场景 (Where)** | `ToolPurchaseServiceImpl`、`OrderServiceImpl` 等回写 userMap 时清除登录会话 TTL |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | Redis 内存只增不减 | 会话键失去 TTL，永不过期 |
| 2W | 为什么失去 TTL？ | 两参数 `setCacheObject` 覆盖写入时清除了原有过期时间 |
| 3W | 为什么用两参数版本？ | 开发者以为"创建时已设过 TTL，后续更新不影响" |
| 4W | 为什么有这个认知偏差？ | 不了解 Redis SET 语义会清除原有过期时间 |
| 5W | **根因**：Redis SET 覆盖语义与开发者直觉不符，且框架提供了两参数重载未做防护 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | 内存持续增长但无告警 | 缺少"无 TTL 键"的监控 |
| 2A | `RedisCache` 封装提供了两参数重载，未加防护 |
| 3A | **根因**：框架层未强制要求所有覆盖写入带 TTL |

**修复方案**：统一走带过期时间的重载；在 `RedisCache` 中收口，去掉两参数版本或加告警日志

<details>
<summary><b>实战案例：购买工具点数后用户会话永不过期</b></summary>

**错误代码**（`ToolPurchaseServiceImpl.java` L355）：
```java
// ❌ 两参数版本：不带 TTL，覆盖写入时清除了原有过期时间
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);
```

**Redis 层面效果**：
```
# 登录时写入（带 TTL）✅
SET login_tokens:userId xxx EX 30000   # TTL = 500分钟

# 购买积分后回写（无 TTL）❌
SET login_tokens:userId xxx            # TTL 被清除！键变成永不过期
```

**同类错误**（`OrderServiceImpl.java` L1337, `SysConfigServiceImpl.java` 4 处）：
```java
// OrderServiceImpl.java L1337 ❌
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);
// SysConfigServiceImpl.java L77/L123/L146/L180 ❌
redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
```

**修复方案**：
```java
// ✅ 统一使用带 TTL 的重载
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap,
        500, TimeUnit.MINUTES);  // 与会话登录有效期一致

// ✅ 更彻底的方案：在 RedisCache 中收口
public <T> void setCacheObject(final String key, final T value) {
    log.warn("【Redis告警】使用无TTL的setCacheObject，key={}", key);
    redisTemplate.opsForValue().set(key, value);
}
```

</details>

---

### 5.2 秒杀 cancelOrder 库存归还未与超时路径 Lua 原子化对齐

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 用户主动取消路径用 `increment` 非原子归还 DB+Redis 库存，超时路径已用 Lua 脚本原子归还 |
| **触发条件 (When)** | 用户取消秒杀订单时 Redis 操作失败（连接超时等） |
| **典型场景 (Where)** | `OshSeckillOrderServiceImpl.cancelOrder` 先 DB 归还再 Redis increment，"半更新"窗口 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | DB 和 Redis 库存不一致 | 取消归还时两步操作非原子 |
| 2W | 为什么非原子？ | 先 `incrStock` 再 `increment`，中间可能失败 |
| 3W | 为什么不用 Lua？ | 超时路径已用 Lua（`RETURN_STOCK_SCRIPT`），但取消路径未复用 |
| 4W | 为什么未复用？ | 两条路径由不同人在不同时期开发，未横向对比实现一致性 |
| 5W | **根因**：同一业务多条归还路径缺乏统一实现范式的约束 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | cancelOrder 和 SeckillOrderTimeoutTask 由不同人在不同迭代开发 |
| 2A | Code Review 未横向对比同一业务的不同路径实现一致性 |
| 3A | **根因**：缺少“同一业务的所有写路径必须统一实现范式”的 Review Checklist |

**修复方案**：复用 `RETURN_STOCK_SCRIPT` 并传入 totalStock 参数，覆盖 cancelOrder 和 rollbackRedis 两条路径

<details>
<summary><b>实战案例：用户取消秒杀订单——DB-Redis 库存不一致</b></summary>

**❌ 用户取消路径**（`OshSeckillOrderServiceImpl.java` L90-97）：
```java
// 先 DB 归还，再裸 increment 归还 Redis — 两步非原子
int qty = order.getQuantity() != null ? order.getQuantity() : 1;
itemMapper.incrStock(order.getItemId(), qty);  // DB 归还 ✅
stringRedisTemplate.opsForValue().increment(stockKey, qty);  // Redis ❌ 若此处超时→不一致
```

**❌ 消费者回滚路径**（`SeckillOrderConsumer.java` L178-187）：
```java
private void rollbackRedis(SeckillOrderMessage msg, int quantity) {
    stringRedisTemplate.opsForValue().increment(msg.getStockKey(), quantity);
    // 无上限保护！重复归还可使 Redis 库存 > totalStock → 超卖风险
}
```

**✅ 超时路径已用 Lua**（`SeckillOrderTimeoutTask.java` L55-68）：
```lua
-- RETURN_STOCK_SCRIPT：原子归还 + 上限保护
local cur = tonumber(redis.call('GET', KEYS[1])) or 0
local total = tonumber(ARGV[2])     -- totalStock 上限
local qty   = tonumber(ARGV[1])
local newVal = math.min(cur + qty, total)  -- ✅ 不超过上限
redis.call('PSETEX', KEYS[1], ttl, tostring(newVal))
return newVal
```

**修复方案**：cancelOrder 和 rollbackRedis 统一复用 `RETURN_STOCK_SCRIPT`

</details>

---

### 5.3 Redis 分布式锁释放每次 new DefaultRedisScript

| 维度 | 内容 |
|------|------|
| **定义 (What)** | `DistributedLockUtil.releaseLock()` 方法体内每次 `new DefaultRedisScript`，与其他 Lua 脚本风格不一致 |
| **修复方案** | 提为 `static final` 常量，对齐其他 3 处 Lua 脚本的定义风格 |

<details>
<summary><b>实战案例：风格对比——4 处 Lua 脚本 3 种写法</b></summary>

```java
// ❌ DistributedLockUtil — 每次 new
public static void releaseLock(...) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();  // 每次创建！
    script.setScriptText("if redis.call('get'...");
    template.execute(script, ...);
}
// ✅ 其他 3 处均为 static final
private static final String SECKILL_LUA_SCRIPT = "local stock = ...";       // 扣减脚本
private static final DefaultRedisScript<Long> RETURN_STOCK_SCRIPT = ...;    // 归还脚本
@Bean public DefaultRedisScript<Long> limitScript() { ... }                // 限流脚本
```

</details>

---

### 5.4 Redis Cluster 大 Key 阻塞单节点主线程

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Redis Cluster 模式下大 Key（如膨胀到几十万条元素的 List）只存在于一个 hash slot 对应的单个节点，`LRANGE key 0 -1` 阻塞该节点主线程，该节点上所有 Key 的读写操作均被阻塞 |
| **触发条件 (When)** | 某个 List/Set/Hash Key 元素量超过万级、业务逻辑存在“只写不删”缺陷 |
| **典型场景 (Where)** | `RedisCache.setCacheList` 无上限 + `getCacheList` 全量读取 `range(key, 0, -1)` |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | Redis 某节点响应变慢 | `LRANGE key 0 -1` 阻塞主线程 |
| 2W | 为什么阻塞？ | List 元素量膨胀到几十万条，全量读取耗时极长 |
| 3W | 为什么膨胀？ | `setCacheList` 用 `rightPushAll` 无上限，无 LTRIM |
| 4W | 为什么无上限？ | RedisCache 作为基础工具类，未在 API 层强制约束集合大小 |
| 5W | **根因**：业务“只写不删” + 基础工具类未做防护，两者叠加导致 Key 无限膨胀 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | 无 Redis 大 Key 监控告警，膨胀过程无感知 |
| 2A | 缺少 `redis-cli --bigkeys` 定期扫描机制 |
| 3A | **根因**：未建立 Redis Cluster 的大 Key 防御体系（LTRIM + 分页读取 + SLOWLOG 监控） |

**修复方案**：`setCacheList` 添加 LTRIM 保护；`getCacheList` 改为分页读取

<details>
<summary>📋 <b>实战案例：RedisCache List 操作无上限——大 Key 阻塞单节点</b>（点击展开代码对比）</summary>

**错误代码**（`RedisCache.java` L139-154）：
```java
// ❌ 写入无上限，无 LTRIM
public <T> long setCacheList(final String key, final List<T> dataList) {
    Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
    return count == null ? 0 : count;
}

// ❌ 读取无上限，range(0, -1) 全量读取
public <T> List<T> getCacheList(final String key) {
    return redisTemplate.opsForList().range(key, 0, -1);
}
```

**修复后**：
```java
// ✅ 写入端防护：LTRIM 只保留最后 N 条
private static final int MAX_LIST_SIZE = 5000;
public <T> long setCacheList(final String key, final List<T> dataList) {
    Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
    redisTemplate.opsForList().trim(key, -MAX_LIST_SIZE, -1);  // LTRIM 保护
    return count == null ? 0 : count;
}

// ✅ 读取端防护：分页读取
public <T> List<T> getCacheList(final String key) {
    return redisTemplate.opsForList().range(key, 0, 999);  // 最多返回 1000 条
}
```

**Redis Cluster 推荐配置**：
```
maxmemory 4gb                    # 物理内存的 70%
maxmemory-policy allkeys-lru     # 内存满时淘汰最久未使用的 Key
lazyfree-lazy-eviction yes       # 大 Key 淘汰异步执行
lazyfree-lazy-expire yes         # 大 Key 过期异步删除
```

</details>

---

## 六、并发与性能瓶颈陷阱（3 项）

### 6.1 Spring Kafka @KafkaListener 默认 concurrency=1 致高并发消费瓶颈

| 维度 | 内容 |
|------|------|
| **定义 (What)** | `@KafkaListener` 未显式设置 `concurrency`，默认单线程消费，重型消费链路形成吞吐量天花板 |
| **触发条件 (When)** | 秒杀开抢、批量支付回调等高并发场景 |
| **典型场景 (Where)** | `SeckillOrderConsumer` 单条处理 100-500ms，QPS 上限 5-20 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 秒杀用户等待时间从秒级延长到分钟级 | 消息积压 |
| 2W | 为什么积压？ | 单线程串行处理，QPS 上限低 |
| 3W | 为什么单线程？ | `@KafkaListener` 未设置 `concurrency`，默认 1 |
| 4W | 为什么未设置？ | 开发阶段低流量下无法暴露吞吐量问题 |
| 5W | **根因**：全局配置和注解中均未设置 concurrency 默认值，无压测验证 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | 开发环境消息量小，无积压，无法暴露问题 |
| 2A | 无 Consumer Group Lag 监控告警 |
| 3A | **根因**：Spring Kafka 默认 concurrency=1 未在文档中标注为“需显式配置”，团队无压测习惯 |

<details>
<summary><b>实战案例：秒杀开抢后消息积压——QPS 仅 5-20</b></summary>

**错误代码**（`SeckillOrderConsumer.java` L59-62）：
```java
@KafkaListener(
    topics = KafkaConstants.SECKILL_ORDER_CREATE_TOPIC,
    groupId = "${spring.kafka.consumer.group-id}"
    // ❌ 未设置 concurrency，默认 1
)
public void consumeSeckillOrder(String message, Acknowledgment ack) {
    // JSON解析→幂等查库→checkout→DB扣库存→订单落库→Redis TTL
    // 单条 100-500ms → QPS 5-20 → 消息积压
}
```

**修复后**：
```java
@KafkaListener(
    topics = KafkaConstants.SECKILL_ORDER_CREATE_TOPIC,
    groupId = "${spring.kafka.consumer.group-id}",
    concurrency = "${spring.kafka.listener.concurrency:3}"  // ✅ 显式设置
)
```
```bash
# 配套：Topic partition 数必须 ≥ concurrency
kafka-topics.sh --alter --topic seckill.order.create --partitions 3
```

</details>

---

### 6.3 Kafka Partition 数与消费者并行度不匹配

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Kafka Topic partition 数与消费者 concurrency/parallelism 不匹配，导致扩容无效或部分 partition 不被消费 |
| **触发条件 (When)** | 消费者扩容 concurrency 后未同步扩容 Topic partition，或 Topic 创建时未显式指定 partition 数 |
| **典型场景 (Where)** | 全部 9 个 Kafka Topic 默认 partition=1，消费者扩容 concurrency 到 3 但只有 1 个线程工作 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | 消费者扩容后吞吐量无变化 | 多出的消费者线程空闲 |
| 2W | 为什么空闲？ | Kafka 保证每个 partition 只被同一 group 内的一个消费者处理 |
| 3W | 为什么 partition 不够？ | Topic 创建时使用 broker 默认值 `num.partitions=1` |
| 4W | 为什么未显式指定？ | 团队未建立 Topic 配置规范 |
| 5W | **根因**：partition 数和并行度的关系是 Kafka 消费模型的核心概念，但团队缺乏配置规范和监控手段 |

**黄金法则**：`消费者并发度 ≤ Topic partition 数`，最佳实践是两者相等。

**各 Topic 推荐 Partition 数**：

| Topic | 推荐 Partition | 推荐消费者并发 | 依据 |
|-------|--------------|-------------|------|
| `seckill.order.create` | 3-6 | 3-6 | 秒杀高峰期并发最密集 |
| `pay-success-topic` | 2-3 | 2-3 | 含 DB + 权益发放 |
| `osh.group.order` | 2-3 | 2-3 | 2 个 Flink Job 消费 |
| `user-action` | 1 | 1 | 低优先级日志 |
| 其余索引类 Topic | 1-2 | 1-2 | ES 写入频率低 |

<details>
<summary>📋 <b>实战案例：扩容 concurrency 无效——partition 仍为 1</b>（点击展开）</summary>

```bash
# ❌ Topic 默认 partition=1，扩容 concurrency 无效
kafka-topics.sh --describe --topic seckill.order.create
# PartitionCount: 1  ← 只有 1 个 partition

# ✅ 扩容 partition（只增不减！）
kafka-topics.sh --alter --topic seckill.order.create --partitions 3
# 注意：partition 扩容后 partition key 的 hash 分布不变，同 key 仍路由到原 partition
```

**边界条件**：Kafka 只允许增加 partition，不允许减少；partition 扩容不影响已有消息，只影响新消息的路由。

</details>

---

### 6.2 Flink Checkpoint 存储在本地 /tmp 致重启后状态丢失

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Flink Job 的 Checkpoint 存储在 `/tmp`，服务器重启或 OS 清理后状态全丢 |
| **触发条件 (When)** | 服务器重启、磁盘清理、systemd-tmpfiles 定时清理 |
| **典型场景 (Where)** | 3 个 Flink Job 的 `setCheckpointStorage("file:///tmp/flink-checkpoints/...")` |

**5WHY 推导**：Job 重启后数据丢失 → Checkpoint 不存在 → `/tmp` 被清空 → 开发时用了本地临时目录 → **根因**：开发阶段快速验证，生产化时未迁移到持久化存储

<details>
<summary><b>实战案例：Flink 重启后 ES 索引不完整</b></summary>

```java
// ❌ 存储在 /tmp，服务器重启后被 OS 清理
env.getCheckpointConfig().setCheckpointStorage(
    "file:///tmp/flink-checkpoints/group-activity-index");
```
```
WARN - Checkpoint directory file:///tmp/flink-checkpoints/... does not exist
INFO - Starting from latest Kafka offset (no checkpoint to restore from)
```

**修复后**：
```java
// ✅ 持久化目录 + 参数化
String ckptDir = ApplicationPropertiesConfig.read(
    "flink.checkpoint.dir", "FLINK_CHECKPOINT_DIR",
    "file:///data/flink-checkpoints");
env.getCheckpointConfig().setCheckpointStorage(ckptDir + "/group-activity-index");
```

</details>

---

## 七、编译构建与工具链陷阱（5 项）

### 7.1 编译缓存残留导致虚假编译错误

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 源码正确但 IDE 报"非法前向引用"等不合理错误，增量编译混用新旧 class 文件 |
| **触发条件 (When)** | 修改方法签名、返回类型等结构性变更后依赖增量编译 |

**修复方案（按优先级）**：
1. `Build → Rebuild Project`
2. `File → Invalidate Caches... → Invalidate and Restart`
3. 删除 `target/` 后重新编译

---

### 7.2 Spring Boot 2.5.x 需显式添加 junit-vintage-engine

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Spring Boot 2.5.x 的 `spring-boot-starter-test` 默认不含 `junit-vintage-engine`，JUnit 4 测试类不被识别 |
| **典型场景 (Where)** | `Tests run: 0`、`No tests to run` 但实际存在 `@Test` 方法 |
| **修复方案** | `backstage-system/pom.xml` 添加 `junit-vintage-engine` 依赖 |

<details>
<summary>📋 <b>实战案例：JUnit 4 测试类不执行——Tests run: 0</b>（点击展开）</summary>

**现象**：
```bash
mvn test -pl backstage-system -Dtest=GroupOrderTimeoutCancelTaskTest
# Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
# 但实际存在 @Test 方法！
```

**根因**：Spring Boot 2.5.15 的 `spring-boot-starter-test` 不含 `junit-vintage-engine`

**修复后**（`backstage-system/pom.xml`）：
```xml
<!-- ✅ 添加 JUnit 4 支持 -->
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

---

### 7.3 Maven Surefire 跨模块测试筛选问题

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 多模块项目用 `-Dtest=XxxTest` 时，无匹配测试的模块报错导致构建失败 |
| **修复方案** | 同时使用 `-pl module -am` + `"-Dsurefire.failIfNoSpecifiedTests=false"`（注意参数名不是 `-DfailIfNoTests`） |

<details>
<summary>📋 <b>实战案例：跨模块测试筛选——参数名错误导致构建失败</b>（点击展开）</summary>

**错误命令**：
```bash
# ❌ 错误参数名，无匹配测试的模块仍报错
mvn test -Dtest=OshCourseServiceTest -pl backstage-system -am
# [ERROR] No tests matching the pattern [OshCourseServiceTest] were found
```

**正确命令**：
```bash
# ✅ 正确参数名 + PowerShell 中 -D 需双引号包裹
mvn test -pl backstage-system -am -batch-mode "-Dtest=OshCourseServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

</details>

---

### 7.4 Maven Surefire 需配置 includes 跨模块执行测试

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Surefire 默认不跨模块执行测试，父 POM 中 `mvn test` 仅执行当前模块 |
| **修复方案** | 配置 `<includes><include>**/*Test.java</include></includes>` |

<details>
<summary>📋 <b>实战案例：父 POM 运行测试只执行当前模块</b>（点击展开）</summary>

**错误配置**：
```xml
<!-- ❌ 缺少 includes，子模块测试不被执行 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
</plugin>
```

**修复后**：
```xml
<!-- ✅ 添加 includes 支持跨模块 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
        </includes>
    </configuration>
</plugin>
```

</details>

---

### 7.5 Bash 脚本 Windows/Linux 跨平台兼容性（6 类问题）

| 问题类型 | 错误写法 | 正确写法 |
|----------|----------|----------|
| grep -P 不可用 | `grep -P '\s+'` | `grep -E '[[:space:]]+'` |
| PowerShell 不支持 && | `cmd1 && cmd2` | `cmd1; cmd2` 或 `bash -c "..."` |
| sed -i 不一致 | `sed -i 's/a/b/'` | 输出重定向 + mv |
| Set-Content 破坏编码 | `Set-Content` | 使用 IDE SearchReplace |
| backtick 混用 | `\`` | `[^`]` 字符类 |
| Read 工具缓存 | 依赖 Read 验证 | `wc -l` 或 Base64 校验 |

---

## 八、架构配置与部署陷阱（2 项）

### 8.1 OSH 双认证系统机制与注解冲突

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 两套互不兼容的认证系统（OshUser C端 vs RuoYi Admin 后台），token 不可混用 |
| **触发条件 (When)** | API 测试时选错认证头；Controller 同时标注 `@Anonymous` + `@PreAuthorize` |

**认证系统对比**：

| 维度 | OshUser (C端) | RuoYi Admin (后台) |
|------|---------------|---------------------|
| Header | `token` (自定义) | `Authorization: Bearer <token>` (标准) |
| Filter | `OshAuthenticationFilter` | `JwtAuthenticationTokenFilter` |
| 上下文 | 自定义 User | `SecurityUtils.getLoginUser()` |

**注解冲突**：`@Anonymous` 绕过 Security 过滤器链 → SecurityContext 为空 → `@PreAuthorize` 读取空上下文抛异常 → **两者不可同时使用**

<details>
<summary><b>实战案例：API 测试 401 + 注解冲突 NPE</b></summary>

```http
# ❌ C 端接口用了 Admin 认证头
POST /api/wx/group/work/join
Authorization: Bearer eyJhbGciOi...    ← 401 Unauthorized

# ✅ C 端用自定义 token 头
POST /api/wx/group/work/join
token: eyJhbGciOi...                   ← 200 OK
```

```java
// ❌ @Anonymous + @PreAuthorize 同时使用 → 运行时 NPE
@Anonymous  // Security 过滤器链绕过 → SecurityContext = 空
@PreAuthorize("@ss.hasPermi('group:work:join')")  // AOP 层读空上下文 → 异常
@PostMapping("/join")

// ✅ 根据接口性质二选一
@Anonymous                          // 公开接口：免登录
@PostMapping("/detail")

@PreAuthorize("@ss.hasPermi(...)")  // 管理接口：需权限
@GetMapping("/admin/list")
```

</details>

---

### 8.2 Flink Job 并行度硬编码致无法动态扩缩容

| 维度 | 内容 |
|------|------|
| **定义 (What)** | 6 个 Flink Job 中 4 个硬编码 `env.setParallelism(1)`，Kafka partition 扩容后 Flink 仍单线程消费 |
| **典型场景 (Where)** | `GroupActivityIndexSyncJob`、`SeckillItemIndexSyncJob` 等 Config 类缺少 parallelism 字段 |

**5WHY 推导**：Kafka partition=3 但 Flink parallelism=1 → 只消费 partition 0 → 其他 partition 消息不被消费 → **根因**：开发时硬编码为 1，未对齐 Course/Tool 模块的可配置化模式

**修复方案**：所有 Config 类添加 `parallelism` 字段，Job 入口使用 `config.getParallelism()`

---

### 8.3 ES/Kafka 中间件超时配置不当致连接反复中断

| 维度 | 内容 |
|------|------|
| **定义 (What)** | Kafka consumer `session.timeout.ms` 低于 Kafka 4.x broker 最小值（45000ms），broker 拒绝注册并反复踢出消费者；ES `socket-timeout` 过短（10s），bulk 操作或 GC pause 时客户端超时断开 |
| **触发条件 (When)** | 使用 Kafka 4.x broker（默认 `session.timeout.ms` 最小值 45s）；ES 执行 bulk index 或遇到 GC 停顿；后端服务与中间件网络波动 |
| **典型场景 (Where)** | `application-local.yml` 中 Kafka consumer 未显式配置超时参数，ES `socket-timeout: 10000`；Kafka `controller.log` 出现 `DuplicateBrokerRegistrationException` 和 `Fencing broker 1 because its session has timed out`；ES gc.log 显示多次进程重启 |

**5WHY 产生原因推导：**

| 层级 | 归因 |
|------|------|
| 1W | Kafka 消费者反复断开重连 | broker 将消费者踢出 consumer group |
| 2W | 为什么被踢出？ | session.timeout.ms 低于 broker 最小值 45000ms |
| 3W | 为什么配置过短？ | Spring Kafka 未显式配置，使用默认 10000ms |
| 4W | 为什么未发现？ | 超时错误被 Spring 重试机制掩盖，只在 broker 侧日志可见 |
| 5W | **根因**：未对齐 Kafka 4.x broker 的默认参数约束，配置中缺少显式超时声明 |

**5WHY 流出原因推导：**

| 层级 | 归因 |
|------|------|
| 1A | Kafka 版本升级后 broker 默认参数变更，客户端配置未同步更新 |
| 2A | 后端 sys-error.log 中无显式中间件连接错误（Spring Kafka 重试机制静默处理） |
| 3A | **根因**：缺少中间件超时参数的显式配置和监控，未建立版本升级后的配置审查清单 |

<details>
<summary>📋 <b>实战案例：本地开发环境 Kafka/ES 连接不稳定——超时参数诊断全过程</b>（点击展开）</summary>

**现象**：
- Kafka `controller.log` 出现 `DuplicateBrokerRegistrationException`、`Fencing broker 1 because its session has timed out`
- ES gc.log 显示 4 次实例创建（前 2 次堆内存 15.8GB 而非 2GB，GC 压力极大）
- 后端 sys-error.log 中 **0 条**显式中间件连接错误（Spring Kafka 重试机制静默处理）

**Kafka 日志证据**（`C:\middleware\kafka\logs\controller.log`）：
```
[2026-09-28 00:48:16,123] INFO [BrokerServer id=1] Fencing broker 1 because its session has timed out
[2026-09-28 00:48:24,456] ERROR Registration request from broker 1 failed: DuplicateBrokerRegistrationException
```

**ES GC 日志证据**（`C:\middleware\elasticsearch\elasticsearch-7.17.25\logs\gc.log.*`）：
```
# gc.log.00 - PID 7548 (第1次启动，堆内存错误 15.8GB)
# gc.log.01 - PID 9012 (第2次启动，堆内存错误 15.8GB)
# gc.log.02 - PID 10832 (第3次启动，堆内存正确 2GB)
# gc.log    - PID 4196  (第4次启动，稳定运行)
```

**修复前**（`application-local.yml`）：
```yaml
# ❌ Kafka consumer 未配置超时参数，使用 Spring Kafka 默认值 10000ms
consumer:
  group-id: osh-backend-local
  auto-offset-reset: earliest

# ❌ ES socket-timeout 10s 过短
elasticsearch:
  socket-timeout: 10000
```

**修复后**：
```yaml
# ✅ Kafka consumer 显式配置超时参数，对齐 Kafka 4.x broker 要求
# ⚠️ Spring Boot 2.5.x 不支持 session-timeout/heartbeat-interval 作为顶层属性
#    必须通过 properties 映射传递原始 Kafka 属性名
consumer:
  group-id: osh-backend-local
  auto-offset-reset: earliest
  properties:
    session.timeout.ms: 45000         # 45s —— 与 Kafka 4.x broker 最小值对齐
    heartbeat.interval.ms: 15000      # 15s —— 推荐为 session-timeout 的 1/3
    max.poll.interval.ms: 300000      # 5min —— 消费逻辑含 DB+ES 写入时防止超时
    request.timeout.ms: 60000         # 60s —— 网络请求超时

# ✅ ES socket-timeout 调整为 60s
elasticsearch:
  connect-timeout: 10000    # 10s 建立连接超时
  socket-timeout: 60000     # 60s 数据读写超时
```

**关键诊断维度**：

| 维度 | 检查方法 | 关键日志特征 |
|------|----------|---------------|
| Kafka broker 侧 | 查看 `controller.log` | `Fencing broker` / `DuplicateBrokerRegistration` |
| Kafka consumer 侧 | 查看 `sys-info.log` | `Discovered group coordinator` / `Successfully joined group` |
| ES 客户端侧 | 查看 `sys-error.log` | `SocketTimeoutException` / `ConnectionClosed` |
| ES GC 状态 | 查看 `gc.log.*` 轮转文件 | 多个不同 PID 表示多次重启，gc.log.00/01/02 对应不同实例 |
| 系统资源 | `Get-Process` + 内存/CPU | 内存使用率 > 90% 时可能触发 OOM Killer |

</details>

---

## 九、重要技术决策记录（3 项）

### 9.1 Redis 多命令原子性：选择 Lua 脚本而非 MULTI 事务

| 维度 | 内容 |
|------|------|
| **结论** | 秒杀库存扣减、限流计数、分布式锁释放、库存归还统一采用 Lua 脚本 |
| **理由** | Lua 支持脚本内条件分支（MULTI 做不到）、天然 CAS、一次网络往返、逻辑内聚 |
| **否决方案** | MULTI/EXEC + WATCH 乐观锁重试——在需要"读→判断→写"闭环的场景下能力不足 |
| **失效条件** | Redis Cluster 跨 slot 场景需拆脚本或改用函数；纯顺序批量写入可评估 Pipeline |

### 9.2 拼团延迟双删不设重试，改用 TTL 兜底

| 维度 | 内容 |
|------|------|
| **结论** | 第二次删除失败仅记录 warn 日志，依赖 TTL（2-5 分钟）自然过期 |
| **理由** | 业务容忍分钟级不一致；引入 MQ/重试表增加复杂度与运维成本 |
| **待改进** | 注册到 `TransactionSynchronizationManager.afterCommit` 保证事务提交后再删（最高优先级但未实施） |
| **失效条件** | 支付回调等关键路径要求秒级一致时必须重新评估 |

### 9.3 Redis KEYS 安全整改：根源修复封装而非逐点替换

| 维度 | 内容 |
|------|------|
| **结论** | 只改 `RedisCache.keys()` 内部实现（KEYS → SCAN），所有调用方自动受益 |
| **理由** | 收口到单一封装，改动集中、行为一致；SCAN count(200) 是网络往返与内存占用的折中 |
| **否决方案** | 逐个调用点各自实现 SCAN（重复代码多）；Lua 脚本服务端遍历（复杂度高） |

---

## 十、避坑速查表（含技术债务追踪）

### 按开发场景速查

| 开发场景 | 必查陷阱 | 编号 | 检查项 |
|----------|----------|------|--------|
| **新增数据库列** | 四层同步（DDL/实体/insert/VO） | 2.1, 2.3, 4.1, 4.2 | DDL + ALTER 同步；insert 入口 setter；VO 字段覆盖 |
| **重命名数据库列** | 索引引用更新 + 拦截器排除 | 2.2, 2.4 | KEY/INDEX 引用扫描；delete_flag 过滤检查 |
| **新增 Mapper 查询** | delete_flag + resultMap + 索引列无函数 | 2.4, 3.2, 3.1 | 软删除条件；列-属性映射；WHERE 中列保持原始 |
| **多数据源合并查询** | UNION ALL + ORDER BY filesort | 3.6 | 拆分为独立查询 + Java 归并 |
| **修改返回类型** | 三层同步（接口/实现/调用方） | 4.3 | grep 搜索所有调用点；import 检查 |
| **新增枚举值** | 验证业务代码引用 | 4.4 | 枚举新增必须伴随 Service 调用 |
| **回写缓存对象** | 必须带 TTL | 5.1 | setCacheObject 必须用带 timeout 的重载 |
| **秒杀/库存操作** | 归还路径统一用 Lua + 上限保护 | 5.2 | cancelOrder 和 rollbackRedis 复用 RETURN_STOCK_SCRIPT |
| **List 缓存操作** | setCacheList LTRIM + getCacheList 分页 | 5.4 | 无上限写入 + 全量读取 = 大 Key |
| **新增 Kafka 消费者** | 显式设置 concurrency + partition 匹配 | 6.1, 6.3 | concurrency ≤ partition 数 |
| **新增 Flink Job** | Checkpoint 持久化 + 并行度可配置 | 6.2, 8.2 | 路径不在 /tmp；Config 有 parallelism 字段 |
| **修改 XML Mapper** | mvn clean compile | 3.4 | 修改 resources 文件后必须 clean |
| **新增 Controller 接口** | 认证方式二选一，不可混用 | 8.1 | @Anonymous 和 @PreAuthorize 不同时使用 |
| **升级/安装中间件** | 超时参数对齐 + 堆内存配置 | 8.3 | Kafka session.timeout ≥ broker 最小值；ES socket-timeout ≥ 30s |

### 按严重度速查（含技术债务状态）

| 严重度 | 陷阱 | 核心风险 | 修复状态 | 责任人 |
|--------|------|----------|---------|-------|
| **P0** | 5.2 库存归还不原子 | 数据不一致 / 超卖 | 未开始 | — |
| **P0** | 5.4 Redis 大 Key 阻塞 | 节点级服务不可用 | 未开始 | — |
| **P0** | 3.6 UNION ALL filesort | 查询性能崩溃 | 未开始 | — |
| **P0** | 4.1 setter 遗漏 | 新记录字段为 NULL | 部分修复 | — |
| **P0** | 3.1 索引失效 | 全表扫描 | 已修复 | — |
| **P0** | 5.1 TTL 清除 | 内存泄漏 / 会话永不过期 | 未开始 | — |
| **P0** | 4.2 VO 断链 | 前端数据缺失 | 部分修复 | — |
| **P1** | 2.3 DDL 不同步 | 新环境建表失败 | 部分修复 | — |
| **P1** | 3.2 resultMap 缺失 | 静默空值 | 未开始 | — |
| **P1** | 4.3 三层不同步 | 编译失败 | 未开始 | — |
| **P1** | 6.1 Kafka 单线程 | 消息积压 | 未开始 | — |
| **P1** | 6.3 Partition 不匹配 | 扩容无效 | 未开始 | — |
| **P1** | 8.1 认证冲突 | API 401 / NPE | 未开始 | — |
| **P1** | 8.3 中间件超时 | Kafka/ES 连接中断 | 已修复 | — |
| **P2** | 2.2 幽灵列 | 建表失败 | 已修复 | — |
| **P2** | 3.4 XML 缓存 | SQL 不生效 | 未开始 | — |
| **P2** | 6.2 Flink 状态丢失 | 重启后数据丢失 | 未开始 | — |
| **P2** | 7.x 构建工具链 | 测试不执行 / 虚假错误 | 未开始 | — |

---

## 十一、检索指引

### 按关键词检索

| 关键词 | 命中陷阱 | 核心文件 |
|--------|----------|----------|
| `delete_flag` / 软删除 | 2.4, 2.5, 2.6 | `GlobalLogicDeleteInterceptor.java` |
| `resultMap` / 字段映射 | 3.2, 3.3 | `*Mapper.xml` |
| `CASE WHEN` / `COLLATE` / 索引 | 3.1, 3.5 | `OshGroupUserInitiatedMapper.xml` |
| `UNION ALL` / `filesort` / 归并 | 3.6 | `*ServiceImpl.java` |
| `setNo` / `setter` / insert | 4.1, 4.4 | `*ServiceImpl.java` |
| `VO` / `DTO` / 前端缺失 | 4.2 | `*VO.java` |
| `TTL` / 过期时间 / 内存泄漏 | 5.1 | `RedisCache.java`, `ToolPurchaseServiceImpl.java` |
| `Lua` / 原子操作 / 库存 | 5.2, 5.3, 9.1 | `OshSeckillOrderServiceImpl.java`, `SeckillOrderTimeoutTask.java` |
| `大Key` / `LTRIM` / `range(0,-1)` | 5.4 | `RedisCache.java` L139-L154 |
| `Kafka` / `concurrency` / `partition` | 6.1, 6.3 | `SeckillOrderConsumer.java`, `KafkaConstants.java` |
| `Flink` / Checkpoint / 并行度 | 6.2, 8.2 | `*Job.java` |
| `clean compile` / 缓存 / Rebuild | 3.4, 7.1 | `target/` 目录 |
| `Surefire` / JUnit / 测试 | 7.2, 7.3, 7.4 | `pom.xml` |
| `@MapKey` / 单行查询 / 嵌套 Map | 3.3 | `OshCourseSectionMapper.java` L38 |
| `认证` / `@Anonymous` / `@PreAuthorize` | 8.1 | `OshAuthenticationFilter.java` |
| `session.timeout` / `socket-timeout` / 中间件超时 | 8.3 | `application-local.yml`, `controller.log` |
| `Fencing broker` / `DuplicateBrokerRegistration` / `GC log` | 8.3 | Kafka `controller.log`, ES `gc.log.*` |
| `DDL` / `ALTER` / 类型不一致 | 2.1, 2.3 | `sql/*.sql` |
| `PowerShell` / `Bash` / 跨平台 | 7.5 | `scripts/code-quality-guard.sh` |

### 按文件路径检索

| 文件/目录 | 命中陷阱 |
|-----------|----------|
| `*.xml` (Mapper) | 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6 |
| `*ServiceImpl.java` | 4.1, 4.3, 4.5, 5.2, 3.6 |
| `*VO.java` / `*DTO.java` | 4.2, 4.3 |
| `sql/*.sql` | 2.1, 2.2, 2.3 |
| `RedisCache.java` | 5.1, 5.4, 9.3 |
| `*Consumer.java` | 6.1, 6.3 |
| `*Job.java` (Flink) | 6.2, 8.2 |
| `*Mapper.java` (接口) | 3.3 |
| `code-quality-guard.sh` | 2.1, 2.3, 4.1, 4.2, 4.4 |
| `KafkaConstants.java` | 6.3 |
| `application-local.yml` | 8.3 |
| `ElasticsearchConfig.java` | 8.3 |

### 自动化检查覆盖矩阵

| CHECK 编号 | 检查内容 | 覆盖陷阱 | 检测命令 | 触发方式 |
|-----------|----------|----------|----------|----------|
| CHECK 1 | ServiceImpl insert 入口 setter 完整性 | §4.1 | 动态实体发现 + setter 扫描 | PR 时自动 |
| CHECK 2 | VO 字段覆盖度 + ALTER vs 初始 DDL 同步 | §4.2, §2.3 | find + grep VO 字段 | PR 时自动 |
| CHECK 3 | 枚举值使用完整性 | §4.4 | grep 枚举常量引用 | PR 时自动 |
| CHECK 4 | ALTER vs 初始 DDL 字段同步 | §2.3 | grep ALTER ADD COLUMN | PR 时自动 |
| CHECK 5 | DDL 索引列引用合法性 | §2.2 | grep KEY/INDEX 引用 | PR 时自动 |
| CHECK 6 | DDL 列类型 vs Java 实体类型映射 | §2.1 | 类型映射表比对 | PR 时自动 |
| **CHECK 7** | **`@KafkaListener` concurrency 参数扫描** | **§6.1, §6.3** | `grep -A2 "@KafkaListener" \| grep -v concurrency` | PR 时自动 |
| **CHECK 8** | **两参数 `setCacheObject` 调用扫描** | **§5.1** | `grep -rn "setCacheObject(" \| grep -v "TimeUnit\|expire"` | PR 时自动 |
| **CHECK 9** | **Flink Checkpoint 路径检查** | **§6.2** | `grep -rn "setCheckpointStorage" \| grep "/tmp"` | PR 时自动 |
| **CHECK 10** | **delete_flag 遗漏扫描** | **§2.4** | `grep -rL "delete_flag" --include="*.xml" mapper/` | PR 时自动 |
| **CHECK 11** | **Kafka consumer 超时参数扫描** | **§8.3** | `grep -rn "session-timeout\|heartbeat-interval" --include="*.yml"` | PR 时自动 |
| **CHECK 12** | **ES socket-timeout 配置检查** | **§8.3** | `grep -rn "socket-timeout" --include="*.yml" \| grep -v "[3-9][0-9]000"` | PR 时自动 |

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | — | 初版全景手册（28 项陷阱 + 3 项决策） |
| v1.1 | — | 合并代码示例手册（18 个实战案例） |
| v2.0 | — | 整合为全景与实战手册 |
| v3.0 | 2026-09-27 | 六维度系统性改进：新增 §3.6/§5.4/§6.3 共 3 项陷阱；补充 §3.3/§4.4/§7.2/§7.3/§7.4 共 5 个实战案例；深化 §3.2/§3.4/§4.5/§5.2/§6.1 共 5 项 5WHY 流出原因链；新增 CHECK 7-10 自动化检查；升级速查表含技术债务状态；添加快速上手指南 |
| v3.1 | 2026-09-28 | 新增 §8.3 ES/Kafka 中间件超时配置陷阱；新增 CHECK 11-12 自动化检查；修复 application-local.yml Kafka consumer 超时参数（session-timeout=45s）和 ES socket-timeout（10s→60s） |

---

> **维护说明**：本手册应随项目迭代持续更新。每次发现新陷阱时，按“定义 → 5WHY → 实战案例”三层结构补充到对应章节，并更新速查表和检索指引。`code-quality-guard.sh` 新增 CHECK 时同步更新自动化检查覆盖矩阵。
