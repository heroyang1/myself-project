# Spec: architecture-boundary

## ADDED Requirements

### Requirement: 服务边界与数据归属约束

系统 SHALL 以 7 个粗粒度微服务（gateway-service、auth-user-service、course-service、trade-service、content-service、tool-service、platform-service）作为业务拆分边界，每张数据表 SHALL 有且仅有一个写权限归属服务。

#### Scenario: 37 个业务域完整映射到 7 服务

- GIVEN backstage-system 现有 37 个业务域（controller/domain 包）
- WHEN 评审 design.md 服务边界表
- THEN 每个域都归属到唯一一个目标服务，无遗漏、无重叠；backstage-admin 系统管理与 backstage-generator 明确保留在单体。

#### Scenario: 一表一主写权限

- GIVEN 任一张 osh_* 或 sys_* 表
- WHEN 查询数据归属表
- THEN 该表只有一个服务拥有写权限，其余服务只能通过 Feign API 或 Kafka 事件读取。

### Requirement: 四类契约冻结

系统 SHALL 在拆分全周期冻结四类契约：对外 URL/参数/响应结构、@PreAuthorize 权限字符串、Redis 分布式锁 key 字面量、LoginUser 缓存 key 格式。

#### Scenario: 前端零感知

- GIVEN 某业务域已从老单体迁移到新服务
- WHEN 前端以原有 URL、参数发起请求
- THEN 响应结构（AjaxResult/TableDataInfo）与迁移前逐字段一致，前端无需任何改造。

#### Scenario: 新老服务共享分布式锁不双花

- GIVEN 同一资源的并发写操作分别落到老单体与新服务（双跑期）
- WHEN 双方各自尝试获取分布式锁
- THEN 因锁 key 字面量一致，仅一方获得锁，另一方按现有 CAS 失败处理规范返回，不产生超卖/双花。

### Requirement: 跨服务通信契约

服务间同步调用 SHALL 使用 OpenFeign 且契约以独立 xxx-api Maven 模块发布；异步通信 SHALL 使用统一 CloudEvents 风格信封经 Kafka 投递，消费端 SHALL 以 CAS 幂等闸门防重。

#### Scenario: 契约模块只增不删

- GIVEN 某 xxx-api 契约模块已发布 1.0.0 版本
- WHEN 后续版本需要移除某字段或方法
- THEN 必须先标注 @Deprecated 并保留至少两个版本周期，禁止直接删除。

#### Scenario: 事件重复投递幂等

- GIVEN 同一 Outbox 事件因重试被投递两次
- WHEN 消费服务先后处理这两条消息
- THEN 第二次处理被 CAS 幂等闸门拦截，业务状态只变更一次。

### Requirement: 绞杀切流与回滚

拆分 SHALL 采用绞杀者模式：新服务经网关灰度切流（1%→10%→50%→100%），双跑期内老单体对应代码 SHALL 保留不删，以支持路由级秒级回滚。

#### Scenario: 灰度切流可回退

- GIVEN 某服务已灰度到 50% 流量且出现 P0 缺陷
- WHEN 运维在网关将该路径指回老单体
- THEN 全部流量在秒级回到老单体处理，用户无感知，且无数据丢失（共库或双写保证）。

### Requirement: 拆分变更合规

每个 split-* / upgrade-* change SHALL 包含 proposal.md、design.md、tasks.md、specs/，且 design.md SHALL 覆盖 API 契约、数据归属表、事件清单、灰度与回滚级别、兼容性矩阵（对齐 openspec/project.md 升级原则七要素）。

#### Scenario: 缺失强制章节被拦截

- GIVEN 某 split-* change 的 design.md 缺少回滚方案章节
- WHEN 提交 PR
- THEN Spec & Process Guard / 评审拒绝合入，补齐后方可继续。
