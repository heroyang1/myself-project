---
title: 架构总览（Architecture Overview）
path: docs/architecture/overview.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 架构总览（Architecture Overview）

> 面向任务的入口文档。想了解"边界规则"见 [boundaries.md](./boundaries.md)，想了解"数据怎么流转"见 [data-flow.md](./data-flow.md)。
> 权威流程规范仍以 [`docs/ai/`](../ai/README.md) 与 [`openspec/project.md`](../../openspec/project.md) 为准，本文只做汇总导航。

## 项目定位

`osh-backend` 是 OSH 平台后端，当前为 **Spring Boot 2.5 单体多模块**，承载主站、课程、考试、电子书、拼团、秒杀、订单、工具、反馈、问答、用户与后台管理等能力，按业务边界逐步演进到微服务。

## 技术栈基线（不允许擅自升级，升级须先建 OpenSpec change）

| 维度 | 选型（取自根 [`pom.xml`](../../pom.xml)） |
|---|---|
| 语言/构建 | Java 8 运行语义（禁 `var`/`record`/text blocks），CI 用 JDK 17 构建 |
| 框架 | Spring Boot 2.5.15、Spring Cloud 2020.0.6、Spring Cloud Alibaba 2021.1 |
| 持久化 | MyBatis-Plus 3.5.3.1 + PageHelper 1.4.7 + Druid 1.2.23 + MySQL（无 Flyway，DDL 走 [`sql/`](../../sql/) 脚本） |
| 缓存/锁 | Redis + Redisson 3.17.7（`@DistributeLock`） |
| 搜索 | Elasticsearch（经 Kafka + Flink 同步） |
| 消息 | Kafka |
| 注册/配置 | Nacos |
| 对象存储 | Cloudflare R2 / S3 兼容（`OssUtil` + `FileUploadUtils`） |
| 调度 | Quartz + XXL-JOB 2.4.0 |
| 文档 | Springfox 3.0.0（OAS 3.0） |
| 鉴权 | JWT 0.9.1 + Spring Security |

## 模块地图（9 个 Maven 子模块）

| 模块 | 职责 |
|---|---|
| `backstage-admin` | 启动入口、后台与 `/pc` Controller 装配、Swagger 配置、`PcExceptionHandler` |
| `backstage-framework` | 框架核心：Security、Druid/Redis/ES/线程池配置、拦截器、Kafka 消费者（含 `PaymentSuccessConsumer`）、`GlobalExceptionHandler` |
| `backstage-system` | 主业务域：Controller / Service / Mapper，覆盖课程/考试/拼团/秒杀/订单/用户/会员/内容/工具等 |
| `backstage-common` | 公共基础：注解、切面、常量（`HttpStatus`）、枚举、异常（`ServiceException`）、工具类、响应体（`AjaxResult`/`R`）、WebSocket 接口 |
| `backstage-service-contract` | 微服务间共享契约（DTO/常量，纯 POJO，无框架依赖） |
| `backstage-quartz` | 定时任务（Quartz + XXL-JOB 双引擎） |
| `backstage-generator` | 代码生成（Velocity 模板 + 数据库元数据） |
| `backstage-flink` | Flink 流处理：Kafka → ES 索引同步 |
| `backstage-hbase` | HBase 列式存储 Client 封装 |

> 另有 `backstage-ui`：后台管理前端（Vue2 + Element UI），**非 Maven 模块**，不被根 pom 聚合；对接约定见 [../reference/api-spec.md](../reference/api-spec.md)。

**依赖链**：`admin → framework → system → common`；`quartz → common + system`；`generator → common`；`service-contract` 为纯 POJO 契约；`flink`、`hbase` 相对独立。`common` 作为底层不依赖 `framework`/`system`。

## 构建与验证

```bash
# 标准构建（排除环境依赖重的 Flink 模块）
mvn clean install -DskipTests -pl '!backstage-flink'
# 单模块测试
mvn test -pl backstage-system
```

CI 门禁详见 [`.github/workflows/pr-check.yml`](../../.github/workflows/pr-check.yml)（Spec & Process Guard + Code Quality Guard + Build）。

## 相关文档

- 分层与边界规则 → [boundaries.md](./boundaries.md)
- 模块包含矩阵 / 调用方向 / 数据权限 / 配置关系 → [module-dependency-rules.md](./module-dependency-rules.md)
- 标准包结构 / 新模块落位 → [package-structure.md](./package-structure.md)
- 请求/数据流转 → [data-flow.md](./data-flow.md)
- 错误码 → [../reference/error-codes.md](../reference/error-codes.md)
- API 规范与查看方式 → [../reference/api-spec.md](../reference/api-spec.md)
- 工作流与分支规则 → [`docs/ai/02-workflow.md`](../ai/02-workflow.md)
