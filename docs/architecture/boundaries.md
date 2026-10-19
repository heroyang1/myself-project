---
title: 边界与分层规则（Boundaries）
path: docs/architecture/boundaries.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 边界与分层规则（Boundaries）

> 定义"谁能动什么、代码放哪一层"。本文是**分层职责的权威源**；模块包含矩阵、跨模块/跨域调用方向、数据权限边界、配置与代码关系以 [module-dependency-rules.md](./module-dependency-rules.md) 为权威。总览见 [overview.md](./overview.md)。

## 分层规则（单向依赖，禁止反向）

```text
Controller -> Service -> Mapper/Repository -> Database
                      -> Integration Client -> External System
```

| 层 | 只做 | 禁止 |
|---|---|---|
| Controller | 参数接收、权限注解、响应包装 | 写复杂业务、临时兼容逻辑 |
| Service | 业务编排、事务边界、规则判断 | 把事务包住远程调用（长事务） |
| Mapper/Repository | SQL 与数据访问 | 做业务判断 |
| Domain/Entity | 数据模型 | 直接作为接口出入参泄露表结构 |
| DTO/VO/BO | 接口输入输出、字段投影 | 用 `Map<String,Object>` 当结果 |
| Client/Integration | 外部系统访问（R2/S3、微信支付、ES、Kafka、远程服务） | 散落业务判断 |
| Config | 配置装配 | 写业务规则 |

**硬约束**：事务只在 Service；外部调用必须隔离在 client/integration 层；Controller 不写复杂业务。

## 模块边界

模块职责、依赖链（pom 实测）、各模块允许/禁止包含的内容、允许与禁止的调用方向，以
[module-dependency-rules.md](./module-dependency-rules.md) §1/§2/§5/§6 为单一事实源，本文不再复制（历史版本的双源已收敛）。

## 权限边界（B 端 vs C 端）

| 面 | 路径 | 鉴权方式 | 异常处理 |
|---|---|---|---|
| B 端管理 | `/xxx/**` | `@PreAuthorize("@ss.hasPermi('a:b:c')")` | `GlobalExceptionHandler`（framework） |
| C 端用户 | `/pc/**` | `@OshUserLevel(N)` 等级注解 / `@Anonymous` 免登录 | `PcExceptionHandler`（admin，`@Order(1)`） |

C 端与 B 端异常处理器**分包隔离**，避免互相覆盖。错误码语义见 [../reference/error-codes.md](../reference/error-codes.md)。行级数据权限（`@DataScope`、C 端 userId 归属隔离）与字段投影边界见 [module-dependency-rules.md](./module-dependency-rules.md) §7。

## 微服务演进方向

优先按业务能力拆分：`user-service`、`course-service`、`order-service`、`tool-service`、`content-service`、`feedback-service`。

> **红线**：拆分前必须先建 OpenSpec change，不能从"移动代码文件"开始。任何影响接口契约、权限、数据模型、中间件、部署、存储的边界调整都受 [AGENTS.md 硬性规则](../../AGENTS.md) 约束。

## 相关文档

- 模块包含矩阵 / 调用方向 / 数据权限 / 配置关系 → [module-dependency-rules.md](./module-dependency-rules.md)
- 各模块真实包树与新模块落位 → [package-structure.md](./package-structure.md)
- 数据如何在层与中间件间流转 → [data-flow.md](./data-flow.md)
- 编码细节约定 → [`docs/ai/conventions/backend.md`](../ai/conventions/backend.md)、[`docs/ai/05-code-style.md`](../ai/05-code-style.md)
