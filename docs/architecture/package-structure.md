---
title: 标准包结构（Package Structure）
path: docs/architecture/package-structure.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 标准包结构（Package Structure）

> **描述现状并确立标准**：本文按实测的真实目录，规范"代码该放哪个模块、哪个包"，供新模块/新功能落位。**不提议搬动现有包**（那是需走 OpenSpec 的重构）。
> 与通用"单应用 `com/example/app`"模板不同，本项目是 **9 模块 Maven 工程**，采用「**模块 + 业务域**」双层组织。分层职责见 [boundaries.md](./boundaries.md)，模块清单见 [overview.md](./overview.md)。

## 分层到模块的映射

| 模板概念（单 app） | 本项目落位（多模块） |
|---|---|
| `domain/model`（`@TableName` Entity） | `common.core.domain.entity/model`（`BaseEntity` 等基类）+ `system.domain.<模块>`（实体） |
| `domain/dto`（Request/Response/Query） | `system.domain.<模块>.dto` / `.vo`；`common.request` / `common.response` |
| `config`（`@MapperScan`/`MybatisPlusInterceptor`/`@ConfigurationProperties`） | `framework.config`（全局装配）+ 各模块 `config` |
| `mapper`（`extends BaseMapper<T>`） | `system.mapper.<模块>` + `resources/mapper/*.xml` |
| `service` | `system.service.<模块>`（`impl/`） |
| `controller`（`@RestControllerAdvice`） | `system.controller.<模块>`（B 端）、`admin.web.controller.pc`（C 端）；异常 advice：`framework.web`（B 端 `GlobalExceptionHandler`）、`admin.web.controller.pc.PcExceptionHandler`（C 端） |
| `infrastructure`（ApiClient/日志/指标/安全） | `framework.{security,aspectj,interceptor,consumer,datasource,manager}` + `common.{filter,aspect,async,websocket,xss,threadlocal}` + `system.payment`（外部支付 client） |

## 各模块包树（实测）

### backstage-common（底层，被所有模块依赖，不得反向依赖 framework/system）

```text
com.backstage.common
├── core/            # 基础类型：domain(BaseEntity/AjaxResult/R, entity/, model/)、controller(BaseController)、page(TableDataInfo)、redis、text
├── annotation/      # 通用注解（@Anonymous、@Log、@Excel ...）
├── aspect/ async/ config/       # 切面、异步、公共配置（RuoYiConfig）
├── constant/ enums/ exception/  # HttpStatus/Constants、枚举、ServiceException 等
├── filter/ xss/ threadlocal/    # 过滤器、XSS 防护、线程上下文
├── request/ response/           # 通用出入参对象
├── utils/           # 工具（FileUploadUtils、poi/ExcelUtil ...）
└── websocket/       # WebSocket 接口（实现在 system）
```

### backstage-framework（框架核心 / 横切基础设施）

```text
com.backstage.framework
├── config/       # Security、Druid、Redis、ES、线程池、MyBatis 装配
├── security/     # 认证授权与安全过滤器
├── aspectj/      # AOP（DataScope、Log、DataSource ...）
├── interceptor/  # 拦截器
├── consumer/     # Kafka 消费者（PaymentSuccessConsumer ...）
├── datasource/   # 动态数据源
├── manager/      # 异步任务管理
├── web/          # 全局异常处理（GlobalExceptionHandler）
└── handler/      # 处理器
```

### backstage-system（主业务域，按 `<模块>` 垂直切分）

```text
com.backstage.system
├── controller/<模块>/   # B 端 Controller（REST + 权限注解，不写业务）
├── service/<模块>/      # 业务编排 + 事务（impl/）
├── mapper/<模块>/       # MyBatis-Plus Mapper（extends BaseMapper<T>）
├── domain/              # @TableName 实体；<模块>/dto、<模块>/vo、<模块>/es
├── config/ component/   # 业务配置、业务组件
├── payment/             # 支付集成（如 huifu client）
├── task/                # 定时任务
├── handler/ aspect/ interceptor/   # 业务侧横切
├── enums/ constants/ exception/ request/   # 域内枚举/常量/异常/入参
├── jackson/             # 序列化定制
└── util/ utils/         # 工具（OssUtil ...）
```

### backstage-admin（启动入口 + 装配 + C/B 端 Controller）

```text
com.backstage                 # 注意：根包是 com.backstage，非 com.backstage.admin
├── config/                   # 应用装配
└── web/
    ├── controller/{pc, system, tool, monitor, common}/   # pc=C 端（含 PcExceptionHandler）
    └── core/config/          # Swagger（SwaggerConfig）等
```

### backstage-service-contract（跨服务共享契约，纯 POJO 无框架依赖）

```text
com.backstage.contract
├── common/
└── internal/    # asset/ oss/ qa/ trade/
```

> 辅助模块：`backstage-quartz`（`com.backstage.quartz.task`）、`backstage-generator`、`backstage-flink`、`backstage-hbase`。

## 新模块 / 新功能落位规则

1. **业务代码进 `backstage-system`**，按 `<模块>` 垂直建 `controller / service / mapper / domain`；domain 下再分 `dto / vo`。
2. **实体** `@TableName("osh_xxx")` 继承 `BaseEntity`/`OSHBaseEntity`；命名：业务表实体 `Osh<PascalCase>`、系统表实体 `Sys<PascalCase>`，Mapper 接口 `<实体>Mapper`；**Mapper** `extends BaseMapper<T>`，复杂 SQL 走 `resources/mapper/*.xml`，参数用 `#{}`。
3. **对外接口**：B 端放 `system.controller.<模块>`（`@PreAuthorize`）；C 端 `/pc` 放 `admin.web.controller.pc`（`@OshUserLevel`/`@Anonymous`）。
4. **横切/外部系统调用**：安全/切面/消费者放 `framework`；通用工具/注解/基类放 `common`；外部支付/存储 client 放 `system.payment` 或对应 integration 包。
5. **跨服务共享的 DTO/常量**放 `backstage-service-contract`（纯 POJO）。
6. 依赖方向恒为 `Controller → Service → Mapper → DB`，`common` 为最底层不被反向依赖。

## 关于 Linter（包依赖强制）

通用模板底部常配包依赖 Linter 强制分层。本项目**已引入 ArchUnit** 落地这一约束：

- **依赖**：`com.tngtech.archunit:archunit-junit5:1.3.0`，`test` 作用域（不进运行期 jar）；版本在根 [`pom.xml`](../../pom.xml) 的 `<properties>` + `<dependencyManagement>` 统一管理，子模块不写版本。
- **测试类**：`backstage-admin/src/test/java/com/backstage/architecture/LayeredArchitectureTest.java`（选 admin 因其聚合 framework→system→common，可见全工程 `com.backstage.*` 字节码）。
- **已强制规则**：① 层间方向 `Controller→Service→Mapper`（`layeredArchitecture`，用 `consideringOnlyDependenciesInLayers()` 豁免 Kafka 消费者/定时任务对 Service/Mapper 的合法调用）；② Controller 不直接用 Mapper；③ Mapper 不反向依赖 Controller/Service；④ `common` 不反向依赖 framework/system/web。
- **编码约定测试类**：`.../architecture/CodingConventionArchitectureTest.java`——践行"错误信息即 Prompt"（每条失败信息含 `[违规]/[修复]含代码/[参考]文档` 三要素）。分两档：`禁 e.printStackTrace()`（实测零违规，普通规则强制）；`禁 System.out.println`、`裸 HTTP 仅限 client/integration 包`（存量有违规，用 `FreezingArchRule` 冻结基线只拦新增，基线目录 `backstage-admin/archunit-store` 首次运行生成后需提交）。配套规范 [logging.md](../ai/conventions/logging.md)、[api-calls.md](../ai/conventions/api-calls.md)。
- **写法**：JUnit 5（`@AnalyzeClasses`/`@ArchTest`）——backstage-admin 无 junit-vintage，新测试一律 Jupiter。
- **运行 / CI**：本地 `mvn test -pl backstage-admin -am`；CI 已在 [`pr-check.yml`](../../.github/workflows/pr-check.yml) 的 build job 加 "Architecture guardrail (ArchUnit)" 步骤**阻断式**把关（文件行数、文档新鲜度为**非阻断告警**）。
- **兼容性**：ArchUnit 1.x 编译为 Java 8 字节码，兼容本项目 `<java.version>1.8</java.version>`。

尚未覆盖（建议规则稳定绿灯后再逐步加严，避免一次性把构建跑红）：更严格的 `layeredArchitecture()` 全局方向、Lombok 使用约束。**注**：通用模板常含的"禁止 `@Autowired` 字段注入"规则本项目**故意未启用**——字段注入是既有 RuoYi 代码的主流约定，若要推行构造器注入应走 `FreezingArchRule` 冻结基线、只拦新增，而非一刀切。此外 CI 的 [`code-quality-guard.sh`](../../scripts/code-quality-guard.sh) 仍只负责 DDL↔实体/VO 一致性，与 ArchUnit 互补。

## 相关文档

- 分层职责与边界 → [boundaries.md](./boundaries.md)
- 模块清单与技术栈 → [overview.md](./overview.md)
- 数据流 → [data-flow.md](./data-flow.md)
- 新功能设计模板 → [../templates/feature-design-template.md](../templates/feature-design-template.md)
