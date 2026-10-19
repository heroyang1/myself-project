# Tasks: add-observability-stack

## 准备

- [ ] 阅读 `openspec/project.md` 与 plan-microservice-split-roadmap 总纲
- [ ] 确认 add-gateway-service 已就绪（追踪由网关注入 traceparent）
- [ ] 梳理现有日志脱敏规范与 FeedbackStatusMailNotifier 邮件链路，规划监控存储容量与保留期

## 实现

- [ ] 部署追踪后端（Jaeger/Tempo）、Prometheus、Grafana、日志采集（ELK/Loki）
- [ ] 新服务接入 Micrometer Tracing + OTel；老单体接入 Sleuth 兼容层；统一 W3C traceparent 传播
- [ ] 网关注入 traceparent；Feign/RestTemplate 拦截器透传
- [ ] Kafka 生产者/消费者拦截器读写 traceparent header（覆盖 Outbox publishAfterCommit 与 GROUP GroupEventPublisher 两条路径）
- [ ] 线程池/异步 TaskDecorator 透传 MDC 与 trace 上下文
- [ ] 各服务暴露 /actuator/prometheus；配置 Grafana 核心大盘（QPS/P99/错误率/Kafka lag/连接池/JVM）
- [ ] Logback 日志格式增加 traceId/spanId/service；沿用 sys-error/sys-info 分文件；接入集中检索
- [ ] 接入 Sentinel：网关全局限流 + 服务热点限流；秒杀/拼团独立规则
- [ ] 配置 Feign 超时（connect 2s/read 5s）、fallback、Retryer（仅幂等 GET 重试 1 次，写操作不重试）
- [ ] 配置 Sentinel 熔断规则
- [ ] 配置 Grafana P0/P1 告警规则，接入邮件通知链路
- [ ] 产出"新服务可观测性接入模板"文档 + 脚手架（供 split-* 复用）

## 验证

- [ ] 跨"网关→服务→Kafka→消费服务"请求可按同一 traceId 串联全链路
- [ ] 每服务 /actuator/prometheus 指标可抓取，大盘正常展示
- [ ] 日志含 traceId 与 service 名，集中检索可用，敏感信息已脱敏
- [ ] 限流/熔断/超时/重试规则逐一验证（含写操作不自动重试）
- [ ] 模拟支付回调失败率飙升 / Kafka lag 堆积 / 实例下线，P0 告警触达邮件
- [ ] 压测确认埋点带来的 P99 增量在可接受范围

## PR

- [ ] PR 描述关联本 OpenSpec change
- [ ] 附全链路追踪截图/大盘链接与告警验证结果
- [ ] 填写风险与回滚（旁路增强，关埋点/停采集即回滚，采样率可动态调 0）
- [ ] 指派 Reviewer
