# Proposal: add-observability-stack

## 背景

微服务拆分后，一次用户请求可能跨越网关、多个业务服务、Kafka 异步链路与多个中间件。单体时代"看一个进程的日志就能定位问题"的方式将彻底失效。若没有统一的可观测性（追踪/指标/日志）与分布式治理（限流/熔断/超时/重试/降级），拆分会把"可调试的单体"变成"不可调试的分布式泥潭"。因此本变更与 gateway-service 同属 Phase 0 基建，是所有 split-* change 的前置条件——先能看见、能治理，再动手拆。

`openspec/project.md` 与拆分总纲要求：新服务以 Spring Boot 3.2 起步，天然适配 Micrometer Tracing + OpenTelemetry 体系；项目已有日志脱敏规范与邮件告警链路（FeedbackStatusMailNotifier）可复用。

## 目标

- 建立三支柱可观测性：分布式追踪（traceId 全链路透传，含 Kafka 消息头）、指标监控（Prometheus + Grafana 大盘）、集中日志（沿用脱敏规范，traceId 入 MDC，按 service 名区分）。
- 建立分布式治理四件套：限流（Sentinel 网关全局 + 服务热点）、熔断降级（Feign fallback 强制）、超时（connect/read 分级）、重试（仅幂等 GET，写操作严禁自动重试）。
- 定义 P0 告警规则：支付回调失败率、Kafka lag、服务实例下线、核心链路 P99 超阈值，接入现有邮件通知链路。
- 产出可复制的"新服务可观测性接入模板"，后续每个 split-* 服务开箱即用。

## 非目标

- 本次不迁移任何业务域。
- 本次不改变业务接口的对外行为。
- 本次不替换现有日志文件格式约定（sys-error/sys-info 分文件模式保留）。
- 本次不引入 APM 商业产品，采用开源栈（Jaeger/Tempo + Prometheus + Grafana + ELK/Loki）。

## 用户影响

- 对终端用户：无感知（可观测性为旁路，不改业务响应）。
- 对运维/开发：新增追踪/指标/日志/告警基础设施需部署与维护；排障方式从"看单进程日志"升级为"按 traceId 跨服务追踪"。
- 对性能：埋点引入轻微开销（采样率可调），需在压测中确认 P99 增量可接受。
- 对历史数据/缓存/权限：无影响。

## 风险

- 兼容性风险：老单体（SB2.7/javax）与新服务（SB3/jakarta）追踪埋点体系不同（Sleuth vs Micrometer Tracing），需保证 traceId 跨新旧服务连续。缓解：统一 W3C traceparent 传播格式，网关注入、双方透传。
- 数据风险：无（不落业务库）；日志/指标存储需规划容量与保留期。
- 性能风险：全量采样在高并发下有开销。缓解：秒杀/拼团链路采用尾部采样或降低采样率。
- 安全风险：日志/追踪可能泄露敏感信息。缓解：沿用现有日志脱敏规范，trace/span tag 禁止记录密钥、token、身份证等。
- 回滚风险：低。可观测性为旁路增强，回滚即关闭埋点/停用采集，不影响业务主链路。

## 验收标准

- [ ] 一次跨"网关→服务→Kafka→消费服务"的请求，可在追踪系统按同一 traceId 串联全链路。
- [ ] 每个服务暴露 /actuator/prometheus 指标，Grafana 大盘展示 QPS、P99、错误率、Kafka lag、连接池。
- [ ] 日志含 traceId 与 service 名，集中检索可用，敏感信息按脱敏规范处理。
- [ ] 限流：核心入口超阈值被限流；熔断：下游故障时 Feign fallback 生效返回降级结果而非级联失败。
- [ ] 超时/重试：Feign connectTimeout/readTimeout 生效；写操作不自动重试，幂等 GET 最多重试 1 次。
- [ ] P0 告警：模拟支付回调失败率飙升 / Kafka lag 堆积 / 实例下线，告警按规则触达邮件。
- [ ] 产出"新服务可观测性接入模板"文档与脚手架，split-* 服务可直接复用。

## 关联信息

- 禅道任务：待创建
- PR：待提交
- 设计稿：design.md（本 change 内）
- 参考：plan-microservice-split-roadmap 总纲；add-gateway-service；现有日志脱敏规范与 FeedbackStatusMailNotifier 邮件链路
