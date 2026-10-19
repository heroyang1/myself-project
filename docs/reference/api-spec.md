---
title: API 规范与接口文档索引（API Spec）
path: docs/reference/api-spec.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# API 规范与接口文档索引（API Spec）

> 本仓库**不维护手写静态 OpenAPI 文件**（易与代码脱节）。机器可读的权威接口契约是运行时 **Swagger（springfox OAS 3.0）**，本文说明如何查看它，并索引各业务模块的接口文档。约定细节见 [`docs/ai/conventions/api.md`](../ai/conventions/api.md)。

## 查看实时接口契约

| 项 | 值（取自真实配置） |
|---|---|
| 文档框架 | Springfox 3.0.0，`DocumentationType.OAS_30` |
| 扫描范围 | 带 `@ApiOperation` 的方法（`SwaggerConfig.createRestApi()`） |
| 开关 | `swagger.enabled`：local=`true`，**prod=`false`（生产关闭，避免暴露接口）** |
| pathMapping | `swagger.pathMapping` = `/` |
| 服务端口 | local `8080`，prod `8081`（`context-path: /`） |
| UI 地址 | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

> 需在 local/dev profile 启动后端后才能访问；生产环境 Swagger 已禁用。

## 路径与响应约定

- **C 端主站**：`/pc/**`（Nuxt 前端把 `/api/**` 代理到后端 `/pc/**`，新接口勿绕开代理规则）。
- **B 端管理**：后台管理接口，`@PreAuthorize` 鉴权。
- **统一响应体**（禁止混用，详见 [../architecture/data-flow.md](../architecture/data-flow.md)）：
  - 单条增删改查 → `AjaxResult` `{ code, msg, data }`
  - 分页列表 → `TableDataInfo` `{ code, rows, total }`
  - C 端 → `R<T>` `{ code, msg, data }`
- **错误码语义** → [error-codes.md](./error-codes.md)。
- 业务结果看 body `code`，**HTTP 状态恒 200**（401/403 由认证/权限层返回）；新代码优先 `R<T>`，同一接口内 `AjaxResult`/`R` 二选一保持一致。
- 分页用 `BaseController.startPage()`（PageHelper）：`startPage()` 必须紧贴在**目标查询**语句之前，中间插入其他查询会污染分页。
- 响应只用 VO 投影裁剪字段，禁止 `Map<String,Object>` 直出，禁止泄露内部异常/敏感信息。

## 前端对接约定

- 后台前端 `backstage-ui`（Vue2 + Element UI）：`VUE_APP_BASE_API=/dev-api` 代理到后端；请求头 `Authorization: Bearer <token>`。
- C 端主站为独立 Vue/Nuxt 工程（不在本仓库），调本仓库 `/pc/**` 接口；支付回调前缀 `/notify`。
- 接口变更须走 OpenSpec，且 `design.md` 必答“是否需要前端同步”。

## 各模块接口文档索引

| 模块 | 文档 |
|---|---|
| 秒杀 | [`docs/modules/seckill/秒杀模块接口文档.md`](../modules/seckill/秒杀模块接口文档.md) |
| 实用网站 | [`docs/modules/tools/实用网站模块接口文档.md`](../modules/tools/实用网站模块接口文档.md) |
| 课程提问 | [`docs/modules/course/课程提问功能需求分析与接口设计.md`](../modules/course/课程提问功能需求分析与接口设计.md) |
| 课程服务人员申请 | [`docs/modules/course/课程服务人员申请流程需求规格说明书.md`](../modules/course/课程服务人员申请流程需求规格说明书.md) |
| 课程标签 | [`docs/modules/course/course_tag_api.md`](../modules/course/course_tag_api.md) |
| 支付接入（跨模块） | [`docs/modules/payment/业务模块接入支付模块说明.md`](../modules/payment/业务模块接入支付模块说明.md) |
| 付费用户管理（B 端，18+ 接口） | [`docs/internal-member-admin-api/`](../internal-member-admin-api/) |
| 统一公告栏 | [`docs/announcement/Announcement-Backend-Integration.md`](../announcement/Announcement-Backend-Integration.md) |

## 新增/变更接口的流程

接口契约属于受管辖域：中大型或影响行为的接口变更须先建 OpenSpec change（含 `specs/**/spec.md` 用 GIVEN/WHEN/THEN 描述验收），并在 PR 走 [Spec & Process Guard](../../.github/workflows/pr-check.yml)。详见 [AGENTS.md](../../AGENTS.md) 硬性规则。
