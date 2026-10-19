---
title: 模块边界与依赖规则（Module Dependency Rules）
path: docs/architecture/module-dependency-rules.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-10
last_updated: 2026-10-10
openspec: none          # 关联的 change-id；无则 none
---

# 模块边界与依赖规则（Module Dependency Rules）

> 本文是「**模块包含矩阵、跨模块/跨业务域调用方向、数据权限边界、配置与代码关系**」的单一事实源。
> **主从关系**：层内职责以 [boundaries.md](./boundaries.md) 的分层表为权威（本文只做摘要引用）；包树与新模块落位以 [package-structure.md](./package-structure.md) 为权威。其余文件引用规则时**指向本文，不得复制表格**，避免双源漂移。
> 所有"强制方式"标注均如实反映现状：`CI 阻断` = ArchUnit/pom 机器强制；`评审拦截` = 人工 Code Review 必查项。

## 1. 模块职责与依赖链（pom 实测）

| 模块 | 职责 | 依赖的 backstage 模块（实测） |
|---|---|---|
| `backstage-admin` | 启动入口、装配、B/C 端 Controller 薄壳、`PcExceptionHandler` | framework, quartz, generator |
| `backstage-framework` | 横切基础设施：Security/AOP/拦截器/Kafka 消费者/全局异常/框架配置装配 | system, service-contract |
| `backstage-system` | 主业务域：全部业务 Controller/Service/Mapper/domain/task/payment client | common, service-contract |
| `backstage-common` | 公共基础：注解/工具/基类/响应体/通用异常/常量枚举 | （无，最底层） |
| `backstage-service-contract` | 跨服务共享契约：纯 POJO DTO/常量 | （无） |
| `backstage-quartz` | 定时任务（Quartz + XXL-JOB） | common, system |
| `backstage-generator` | 代码生成（Velocity + 元数据） | common |
| `backstage-flink` | Kafka→ES 流处理（标准构建排除） | （独立） |
| `backstage-hbase` | HBase Client 封装 | （独立） |

**实际依赖链**：`admin → {framework, quartz, generator} → system → {common, service-contract}`。
注意方向：**framework 依赖 system**（framework 在 system 之上做横切装配），不是反过来。

## 2. 模块包含矩阵（允许 / 禁止）

| 模块 | 允许包含 | 禁止包含 | 强制方式 |
|---|---|---|---|
| common | 注解、工具类、基类（`BaseController`/`BaseEntity`）、响应体（`AjaxResult`/`R`/`TableDataInfo`）、通用异常、常量枚举、过滤器/XSS/ThreadLocal | 依赖任何上层模块；业务 Service/Controller/SQL；业务判断 | `common` 不反向依赖 framework/system/web —— ArchUnit **CI 阻断** |
| framework | 安全、AOP 切面（DataScope/Log/DataSource/RateLimiter）、拦截器、Kafka 消费者、`GlobalExceptionHandler`、框架配置装配 | 业务规则、业务 SQL 编写 | 评审拦截（消费者回调 system Service/Mapper 是**实测合法特例**，ArchUnit 已豁免） |
| system | 业务垂直域 `controller/service/mapper/domain`（+dto/vo/es）、task、支付等外部 client、业务枚举常量 | 依赖 framework / admin；把跨域调用做成直接引用他域 Mapper | Maven 方向 + 评审拦截 |
| admin | 启动类、应用装配、Swagger、C/B 端 Controller 薄壳、`PcExceptionHandler` | 复杂业务逻辑（必须下沉 system Service） | 评审拦截（分层表，见 boundaries.md） |
| service-contract | 纯 POJO（DTO/常量） | 任何 backstage 模块依赖与框架绑定 | pom 实测无依赖 |
| quartz / generator | 调度任务 / 模板生成 | 业务规则主战场 | 评审拦截 |
| flink / hbase | 独立大数据能力 | 被主链模块反向依赖 | 相对独立（构建默认排除 flink） |

## 3. 层内职责规则（摘要）

以 [boundaries.md](./boundaries.md) 七层"只做/禁止"表为权威。硬约束三铁律：**事务只在 Service；外部调用必须隔离在 client/integration；Controller 不写复杂业务**。补充调用级细则：

- Controller：`startPage()` 只跟在**目标查询**调用之前（中间插入其他查询会污染分页，已知陷阱，评审拦截——尚无机器强制）。
- Service：禁止直接操作 `HttpServletRequest/Response`、禁止返回 HTTP 响应对象。
- Mapper：参数化 `#{}`，`${}` 唯一白名单例外是 `${params.dataScope}`（数据权限拼接）。

## 4. 跨模块调用规范

### 4.1 Maven 模块级

- 只允许 §1 实测链上的依赖方向；**新增跨模块依赖边**（如 system→framework）视为架构变更，必须建 OpenSpec change。
- 跨服务共享的 DTO/常量走 `service-contract`，禁止复制粘贴同名 DTO 到两个模块。

### 4.2 业务域级（backstage-system 内部，30+ 垂直域之间）

| 方向 | 规则 | 强制方式 |
|---|---|---|
| ✅ 允许 | 经对方域 Service 接口 `I<Domain>Service` 调用（如 course 域调 `IOrderService`） | 评审拦截 |
| ✅ 允许 | 消息解耦：业务事件走 Kafka，由 framework/consumer 消费后回调目标域 Service | 评审拦截 |
| ❌ 禁止 | 直接 `@Autowired` **另一个域的 Mapper** 或跨域写别人的表 | 评审拦截；ArchUnit 冻结基线候选（规则稳定后加严） |
| ❌ 禁止 | 复制他域 SQL/实体到本域"就近查询" | 评审拦截 |

**跨域数据完整性四规则**（评审必查，违反会造成静默数据错误）：

1. 跨域调用方法名必须含业务前缀（`insertGroupOrder` 而非 `insertOrder`），防复制误调。
2. 快照字段从**直接上游单据**取，不跳跃引用主数据；快照创建时固化。
3. 关联用稳定 `code` 而非历史 `id`（id 可能因删除重建失效）。
4. 修改父记录关键关联字段时，同事务级联更新子表冗余引用。

## 5. 允许的调用方向

- 层内单向：`Controller → Service → Mapper → DB`；`Service → client/integration → 外部系统`。
- 模块间沿 §1 依赖链方向（上层可依赖下层）。
- 实测特例：`framework.consumer → system.service/mapper`（Kafka 回调）、`quartz → system`（定时任务调业务），ArchUnit 已豁免。

## 6. 禁止的调用方向

| 禁止项 | 强制方式 |
|---|---|
| `common` 依赖 framework/system/web（底层被上层反向依赖） | ArchUnit **CI 阻断** |
| Controller 直接使用 Mapper；Mapper 反向依赖 Controller/Service | ArchUnit **CI 阻断** |
| `system` 依赖 `framework`/`admin`（业务层依赖装配层） | Maven pom 方向，**CI 构建阻断** |
| client/integration 包之外发裸 HTTP 调用 | ArchUnit FreezingArchRule（只拦新增） |
| C 端接口使用 B 端 `@PreAuthorize`（无对应权限码必 403） | 评审拦截 + 集成测试 |
| 业务域间绕过 Service 直捅他域 Mapper | 评审拦截（§4.2） |

## 7. 数据权限边界

三层递进，逐层收窄：

**① 接口鉴权面**（谁能调）
- B 端 `/xxx/**`：`@PreAuthorize("@ss.hasPermi('a:b:c')")`，异常由 framework `GlobalExceptionHandler` 兜底。
- C 端 `/pc/**`：`@OshUserLevel(N)` 等级门禁 / `@Anonymous` 免登录，异常由 admin `PcExceptionHandler`（`@Order(1)`）处理，与 B 端**分包隔离**避免互相覆盖。

**② 行级数据权限**（能看到哪些行）
- B 端部门/角色数据范围：`@DataScope` 注解 → framework `DataScopeAspect` 拼接 `dept_name`/`role_id` 进 SQL（这是 `${}` 白名单唯一来源）。
- C 端归属隔离：当前用户一律取自服务端上下文——`UserContextUtil.getCurrentUserId()`（system/utils，读 ThreadLocal，由 framework 的 `OshAuthenticationFilter` 在过滤链阶段写入；`@Anonymous` 接口过滤器放行不解析 Token，需软认证时用 `getCurrentUserIdSafely()` 回退）；B 端用 `SecurityUtils.getUserId()`。注：`system/interceptor/UserContextInterceptor` 已整体注释废弃，勿引用。查询与写操作必须带 `userId` 归属条件，**禁止信任前端传入的 userId 做归属判断**（水平越权红线）。

**③ 字段边界**（一条记录里能看哪些列）
- 接口出入参用 DTO/VO 投影，禁止 Entity 直接出入泄露表结构。
- 审计与删除列（`delete_flag`/`create_by`/`update_time` 等）不得出现在 C 端响应。

## 8. 配置与代码的关系

**配置文件三级分工**（admin 模块 resources，实测）：

| 文件 | 角色 |
|---|---|
| `bootstrap.yml`（+ `bootstrap-{env}.yml`） | Nacos 接入：地址/namespace/分组；中间件连接按域拆 shared-configs（`osh-backend-mysql-*.yaml`、`redis`、`kafka`），支持 `refresh`。Nacos server-addr **不带 `http://` 前缀**（历史陷阱） |
| `application-local.yml` | **Nacos 不可用时的本地兜底**：仅本地调试，禁用于测试/生产（文件头注明） |
| `application-prod.yml` | 生产本地覆盖项 |

**其他配置位置**（均在仓库根/模块目录实测存在）：日志 `backstage-admin/src/main/resources/logback.xml`；后台前端环境 `backstage-ui/.env.development|.env.production|.env.staging`；质量门禁 `.quality-guard.conf`；`config/checkstyle/checkstyle.xml`（未接入构建，仅参考）。

**代码侧归属**：
- 框架级配置装配（Security/Druid/Redis/ES/线程池/MyBatis）集中在 `framework/config`；跨模块公共配置对象（如 `RuoYiConfig`）在 `common/config`。
- **Config 类只做装配，禁止写业务规则**（分层表硬约束）。
- 新增中间件连接信息 → 改 Nacos shared-config，不改代码；新增开关/阈值且需热更 → Nacos `@RefreshScope`；静态构建期常量 → 代码。

**治理约束**：`application*.yml|properties`、`pom.xml`、`sql/` 属**保护域**，改动不可用 OpenSpec 豁免（CI `verify-ai-process.sh` 强制）。已知技术债：仓库内个别 bootstrap 文件含中间件账号，待迁移至 Nacos/环境变量（gitleaks 只拦新增泄露）。

## 相关文档

- 分层职责权威表 → [boundaries.md](./boundaries.md)
- 各模块包树 / 新模块落位 → [package-structure.md](./package-structure.md)
- 模块清单与技术栈 → [overview.md](./overview.md)
- 数据流转 → [data-flow.md](./data-flow.md)
- 硬性 CI 门禁清单 → [../../AGENTS.md](../../AGENTS.md)
