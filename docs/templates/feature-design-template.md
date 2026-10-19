---
title: 功能设计文档模板（Feature Design Template）
path: docs/templates/feature-design-template.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 功能设计文档模板（Feature Design Template）

> **用途**：Agent 拿到一个功能需求后，先复制本模板填写，评审通过（Approved）后再动手写代码——把"明确意图"工程化。
> **定位**：这是 OpenSpec SDD 的**前置草稿/前门**。审批通过后，本模板内容落入 `openspec/changes/<change-id>/` 四件套（proposal / design / tasks / spec），保持**单一事实源**，不另立规范。
> **依据**：[AGENTS.md](../../AGENTS.md)、[openspec/project.md](../../openspec/project.md)、[架构边界](../architecture/boundaries.md)、[错误码](../reference/error-codes.md)。

## 使用方法

1. 复制本文件，替换 `# Feature: [功能名称]` 起的内容。
2. 逐节填写；每节已标注对应的 OpenSpec 产物。
3. 评审通过（Status = 📋 Approved）后，补齐 `proposal.md` / `tasks.md` / `specs/**/spec.md`，再开始编码。
4. 中大型或影响接口/权限/数据/中间件/部署/存储的需求，**必须**有 OpenSpec change（AGENTS.md 硬性规则）；纯文档/无行为影响可走 `OpenSpec-Exempt`。

---

# Feature: [功能名称]

## Status

📝 Draft ｜ 📋 Approved ｜ 🚧 In Progress ｜ ✅ Implemented

> 当前：📝 Draft。**Draft → Approved 需评审通过；Approved 之后才允许进入 In Progress（写代码）。**

## 目标

> 一句话说明这个功能要解决什么问题。（→ proposal.md「为什么做 / 什么算完成」）

## 非目标

> 明确列出这次**不做**什么，防止 Agent 扩大范围。（→ proposal.md「不做什么」）

- [ ] ...

## 技术方案

> （→ design.md）遵循 Controller→Service→Mapper→DB 单向依赖，外部系统调用隔离在 client/integration 层。

### 涉及的模块 / 分层

| 层 | 位置（backstage-system 真实包） | 变更 |
|---|---|---|
| Entity | `com.backstage.system.domain.<模块>` | 新增 `Xxx`：`@TableName("osh_xxx")`，继承 `BaseEntity`/`OSHBaseEntity` |
| DTO | `...domain.<模块>.dto` | 入参 `XxxCreateDTO`（Controller 侧配 `@Validated`） |
| VO | `...domain.<模块>.vo` | 出参投影 `XxxVO`（禁止直出 Entity 的 `delete_flag`/审计字段） |
| Mapper | `com.backstage.system.mapper.<模块>` | `XxxMapper extends BaseMapper<Xxx>`；复杂查询走 XML，参数用 `#{}` |
| Service | `com.backstage.system.service.<模块>` | `XxxService`/`Impl`；事务在此层 `@Transactional(rollbackFor = Exception.class)` |
| Controller | B 端 `system.controller.<模块>` / C 端 `web.controller.pc` | 端点 + 权限注解，不写业务 |
| Client/Integration | 外部调用隔离层 | R2/S3、微信支付、ES、Kafka、远程服务 |

### 数据模型变更

> 本仓库**无 Flyway**；DDL 以脚本形式放 [`sql/`](../../sql/)（如 `sql/osh_xxx.sql`）。表名 `osh_` 前缀，软删除用 `delete_flag`。
> ⚠️ CI 门禁 `code-quality-guard.sh` 会校验：必填字段四层一致（DDL/ALTER/Entity/VO）、列类型 Java↔SQL 匹配、索引无幽灵列、ALTER 与初始 DDL 同步。**改表必须同步实体/VO。**

```sql
-- 新增/变更表 DDL 写在这里，并同步落到 sql/ 脚本
CREATE TABLE `osh_xxx` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  -- 业务字段 ...
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME,
  `update_time` DATETIME,
  PRIMARY KEY (`id`)
);
```

### API 变更

> C 端主站前缀 `/pc/**`（Nuxt 把 `/api` 代理到 `/pc`）；B 端为后台管理路径。响应体统一：单条 `AjaxResult`、分页 `TableDataInfo`、C 端 `R<T>`。
> 注解顺序：`@ApiOperation` → 权限（B 端 `@PreAuthorize("@ss.hasPermi('a:b:c')")` / C 端 `@OshUserLevel(N)` 或 `@Anonymous`）→ HTTP 方法（GET 查 / POST 增或导出 / PUT 改 / DELETE 删）。

```http
POST /pc/xxx
  Request : { ... }
  Response: { "code": 200, "msg": "操作成功", "data": { ... } }     # R<T> / AjaxResult

分页：GET /pc/xxx/list  ->  { code, rows, total }   # startPage() -> Service -> getDataTable()
错误：业务可预期错误抛 ServiceException（C 端返回 601，B 端按 code 或 500），详见 docs/reference/error-codes.md
```

### 中间件 / 缓存 / ES / 配置影响（design 必答）

- [ ] 是否改数据库？（DDL + 实体/VO 同步）
- [ ] 是否用 Redis / 分布式锁 `@DistributeLock`（写操作防重复提交）？
- [ ] 是否影响 ES？（业务表 → Outbox → Kafka → Flink → ES；查询失败降级查库）
- [ ] 是否新增配置？（Nacos/环境变量，须写默认值与生产/QA 差异）
- [ ] 是否需前端同步 / 接口兼容？（不破坏 `/pc` 兼容面）
- [ ] 如何回滚？

## 验收标准

> （→ `specs/**/spec.md`）用 GIVEN/WHEN/THEN 写**具体、可验证**的行为。

- [ ] 标准 1：GIVEN ... WHEN ... THEN ...
- [ ] 标准 2：GIVEN ... WHEN ... THEN ...
- [ ] 权限失败 / 参数错误 / 资源不存在 场景有明确响应（对齐错误码）
- 验证：本地须通过 `mvn clean install -DskipTests -pl '!backstage-flink'`（JDK 17）与相关核心测试；PR 过 Spec & Process Guard + Code Quality Guard。

> 注：本仓库 CI **不强制**测试覆盖率阈值。若本功能需设覆盖率目标，请写具体数字并注明由谁把关，勿照抄"≥80%"造成与门禁不符。

## 依赖

- [ ] 依赖 feature-xxx（状态：✅ 已实现 / ⏳ 进行中）
- [ ] 关联的 OpenSpec change：`<change-id>`

## 风险与回滚

- 风险：
- 回滚：

---

## 实现阶段遵守（勿改）

- **技术栈基线禁止擅自升级**（Java 8 语义 / Spring Boot 2.5.15 等，见 [AGENTS.md](../../AGENTS.md)）；升级须先建 OpenSpec change。
- **改动只限本任务范围，保留用户本地修改**；不提交密钥/token/日志/构建产物。
- **提交规范**：Conventional Commits —— `feat:` / `fix:` / `refactor:` / `docs:` / `test:` / `chore:`，建议带 scope（如 `feat(group): ...`）。
