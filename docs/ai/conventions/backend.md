# 后端通用约定

## 职责边界

- Controller：入口、参数、权限、响应。
- Service：业务编排、事务、规则判断。
- Mapper：SQL 和数据访问。
- Domain/Entity：数据模型。
- DTO/VO：接口输入输出。
- Client/Integration：外部系统访问。

## 变更要求

- 新增业务规则必须可测试。
- 复杂条件必须有命名方法。
- 涉及多个模块时先写 design。
- 禁止把临时兼容逻辑散落在 Controller。

## 错误处理

- 业务错误返回可理解的错误信息。
- 系统错误记录日志。
- 不把堆栈直接返回给前端。
- 不吞异常。异常→错误码映射见 [../../reference/error-codes.md](../../reference/error-codes.md)。

## 核心基类

| 类 | 位置 | 用途 |
|---|---|---|
| `BaseController` | `common/core/controller` | 分页（`startPage()`）、响应包装、用户工具 |
| `BaseEntity` | `common/core/domain` | create_by/create_time/update_by/update_time/remark 公共字段 |
| `AjaxResult` / `R<T>` | `common/core/domain` | 响应包装（用法见 [../../reference/api-spec.md](../../reference/api-spec.md)） |
| `TableDataInfo` | `common/core/page` | 分页响应 |
| `TreeEntity` / `TreeSelect` | `common/core/domain` | 树结构 |

## 自定义注解速查（均在 `com.backstage.common.annotation`）

| 注解 | 用途 |
|---|---|
| `@Log` | 操作日志（AOP 落库） |
| `@Anonymous` | C端匿名访问白名单 |
| `@RepeatSubmit` | 防重复提交 |
| `@RateLimiter` | 限流 |
| `@DistributeLock` | Redisson 分布式锁（参数决定 lock/tryLock 分支） |
| `@DataScope` | 数据权限 SQL 拼接（边界见 [module-dependency-rules.md §7](../../architecture/module-dependency-rules.md)） |
| `@DataSource` | 主从数据源切换 |
| `@Sensitive` | 数据脱敏 |
| `@Excel` / `@Excels` | 导入导出 |
| `@PerformanceMonitor` | 性能监控 |
| `@OshResourceId` / `@OshUserEvent` / `@OshUserLevel` | OSH 用户域：资源/事件/等级 |

B 端权限用 Spring Security `@PreAuthorize`；**禁混入 C 端 `/pc/**` 接口（无对应权限码必 403）**。
