# Spec: observability-governance

## ADDED Requirements

### Requirement: 全链路分布式追踪

系统 SHALL 为跨网关、多服务、Kafka 异步链路的请求提供统一 traceId 追踪，追踪上下文 SHALL 以 W3C traceparent 格式在同步调用与异步消息间连续传播。

#### Scenario: 同步跨服务追踪连续

- GIVEN 一次请求经网关进入服务 A，服务 A 通过 Feign 调用服务 B
- WHEN 请求完成
- THEN 网关、服务 A、服务 B 的 span 归属同一 traceId，可在追踪系统串联成完整调用链。

#### Scenario: 异步 Kafka 链路追踪连续

- GIVEN 服务 A 经 Outbox publishAfterCommit 或 GROUP 域 GroupEventPublisher 发布事件到 Kafka
- WHEN 消费服务处理该事件
- THEN 生产者将 traceparent 写入消息头，消费者读取后续接 span，生产与消费归属同一 traceId。

#### Scenario: 新老服务追踪互通

- GIVEN 请求在老单体（Sleuth）与新服务（Micrometer Tracing）之间流转
- WHEN 双方通过统一 W3C traceparent 传播
- THEN traceId 跨新老服务保持连续，不断链。

### Requirement: 指标监控与集中日志

每个服务 SHALL 暴露 Prometheus 指标端点，日志 SHALL 携带 traceId 与 service 名并接入集中检索，且 SHALL 遵循现有脱敏规范。

#### Scenario: 指标可抓取

- GIVEN 任一已接入服务
- WHEN 抓取其 /actuator/prometheus
- THEN 返回 QPS、P99、错误率、Kafka lag、连接池、JVM 等核心指标，Grafana 大盘正常展示。

#### Scenario: 日志可按 traceId 检索且脱敏

- GIVEN 一次请求产生的日志分散在多个服务
- WHEN 在集中日志系统按 traceId 检索
- THEN 可聚合出该请求的全部日志，日志含 service 名，且 token/密钥/身份证等敏感信息已按脱敏规范处理。

### Requirement: 分布式治理（限流/熔断/超时/重试/降级）

系统 SHALL 提供限流、熔断、超时、重试与降级能力；写操作 SHALL NOT 自动重试，仅幂等 GET SHALL 最多重试 1 次；跨服务调用 SHALL 强制配置 fallback。

#### Scenario: 限流生效

- GIVEN 某核心入口配置了 QPS 阈值
- WHEN 请求速率超过阈值
- THEN 超出部分被限流返回约定响应，阈值内请求正常处理。

#### Scenario: 熔断降级不级联失败

- GIVEN 服务 A 依赖的服务 B 故障，错误率超过熔断阈值
- WHEN 服务 A 调用服务 B
- THEN 熔断触发并走 Feign fallback 返回降级结果（如非会员价展示），服务 A 不被拖垮。

#### Scenario: 写操作不自动重试

- GIVEN 一个下单/支付类写操作（POST）
- WHEN 调用超时或失败
- THEN 不触发自动重试，改由幂等键 + 补偿任务处理，避免重复下单；幂等 GET 最多自动重试 1 次。

### Requirement: P0 告警

系统 SHALL 对支付回调失败率、Kafka lag 堆积、服务实例下线、核心链路 P99 超阈值等 P0 条件配置告警，并通过现有邮件通知链路触达。

#### Scenario: 告警触达

- GIVEN 模拟支付回调失败率飙升或 Kafka lag 持续堆积
- WHEN 指标越过告警阈值
- THEN Grafana Alerting 触发 P0 告警并经邮件通知链路送达值班人，含服务名与关键指标上下文。
