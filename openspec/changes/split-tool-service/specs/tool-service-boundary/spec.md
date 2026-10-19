# Spec: tool-service-boundary

## ADDED Requirements

### Requirement: 工具域服务边界与自有表写权限

tool-service SHALL 独占工具域自有 7 张表（osh_tool、osh_tool_package、osh_tool_package_purchase_record、osh_tool_collection、osh_tool_vote、osh_tool_tag、osh_tool_user_quota）的写权限，且 SHALL NOT 直连其他域的表（osh_user_asset*、osh_order、osh_payment、osh_announcement、osh_resource_contribution*）。

#### Scenario: 自有表写操作落在 tool-service

- GIVEN 灰度命中 tool-service 的用户请求
- WHEN 执行工具增删改、收藏、点赞、点数消耗、套餐管理、购买记录写入
- THEN 所有写操作由 tool-service 对 7 张自有表执行，单体侧不再写这些表。

#### Scenario: 禁止跨域直连 Mapper

- GIVEN tool-service 代码库
- WHEN 静态检查其对其他域表的访问
- THEN 不存在对 osh_user_asset*/osh_order/osh_payment/osh_announcement/osh_resource_contribution* 的 Mapper 注入或 SQL，跨域读写一律经 Feign 契约或事件。

### Requirement: API 契约冻结

tool-service SHALL 保持 `/pc/tool/**` 全部端点的 URL、入参 DTO、返回 VO 字段结构与拆分前逐字段一致，前端零改造。

#### Scenario: 新老双路径响应一致

- GIVEN 契约冻结清单中的任一端点
- WHEN 以相同请求分别经 tool-service 与经单体 backstage-admin 调用
- THEN 两者响应状态码、`R<T>`/`PageResponse<T>` 结构、字段值逐字段一致。

#### Scenario: 灰度比例为零时全量走单体

- GIVEN 网关 `/pc/tool/**` 灰度比例配置为 0
- WHEN 任意用户请求工具接口
- THEN 请求全部路由到单体，行为与拆分前完全一致。

### Requirement: 用户上下文与鉴权透传

tool-service SHALL 从网关注入的请求头还原用户上下文，并保持 `@Anonymous`、`@OshUserLevel`、`@PreAuthorize` 权限语义与权限字符串冻结。

#### Scenario: 后台管理端点越权拦截

- GIVEN `/pc/tool/save`、`/pc/tool/update`、`/pc/tool/delete`、`/pc/tool/purchase/package/save`、`/pc/tool/purchase/package/delete` 要求 @OshUserLevel(5)
- WHEN 等级不足或未登录的用户经 tool-service 访问这些端点
- THEN 请求被拦截返回鉴权失败，语义与拆分前一致，不发生越权改工具/套餐。

#### Scenario: 匿名端点免登录可访问

- GIVEN `/pc/tool/detail/{id}`、`/pc/tool/search` 等标注 @Anonymous
- WHEN 未登录用户经 tool-service 访问
- THEN 正常返回数据，与拆分前一致。

#### Scenario: 内部头防伪造

- GIVEN 外部请求自带 X-User-Id/X-User-Level 等内部头
- WHEN 请求进入网关再到 tool-service
- THEN 以网关校验结果为准还原上下文，外部伪造头被清洗，不能提权。

### Requirement: 分布式锁 key 冻结

tool-service 与单体双跑期 SHALL 使用字面量一致的分布式锁 key，保证对同一资源的互斥。

#### Scenario: 双跑期并发改同一工具互斥

- GIVEN 同一工具的 update 请求分别落到单体与 tool-service
- WHEN 双方各自尝试获取 @DistributeLock(scene="resource") 锁
- THEN 因锁 key 字面量一致仅一方获锁，另一方按 CAS/等待策略处理，不产生并发覆盖。

### Requirement: 点数购买 SAGA 一致性

点数套餐购买 SHALL 以 SAGA 编排替代原单一本地事务：积分扣减经 auth-user 契约、下单经 trade 契约，失败 SHALL 触发积分退还补偿，写操作 SHALL NOT 自动重试。

#### Scenario: 积分支付下单成功

- GIVEN 用户以积分支付购买点数套餐且余额充足
- WHEN tool-service 执行购买
- THEN 先写入 purchase_record（status=pending、grant=pending），经 auth-user 契约扣减积分（requestId 幂等），经 trade 契约创建订单，最终由支付成功事件发放点数配额。

#### Scenario: 积分不足快速失败无副作用

- GIVEN 用户积分不足以支付套餐
- WHEN tool-service 执行积分支付购买
- THEN 扣减契约返回余额不足，购买失败，purchase_record 不进入已扣积分状态，无积分被扣。

#### Scenario: 下单失败触发退还补偿

- GIVEN 积分已扣减但 trade 下单失败
- WHEN SAGA 检测到下单失败
- THEN 触发补偿：purchase_record 标记 cancelled，经 auth-user 契约退还积分（复用 cancelPendingPurchase 语义），退还以 orderNo/requestId 幂等。

#### Scenario: 写操作不自动重试

- GIVEN 扣减积分或下单等写操作超时
- WHEN Feign 调用失败
- THEN 不自动重试写操作，改由幂等键 + 补偿/对账处理，避免重复扣积分或重复下单。

### Requirement: 点数配额发放单一写者与幂等

`osh_tool_user_quota` 的发放 SHALL 仅由 tool-service 消费 `osh.trade.order.paid.v1`(productType=TOOL) 事件执行，且 SHALL 以 grant_status CAS 保证幂等；单体侧 ToolPaidHandler SHALL 改为发布事件而非直接写工具表。

#### Scenario: 支付成功后发放点数

- GIVEN 一笔工具点数套餐订单支付成功
- WHEN trade/单体侧发布 `osh.trade.order.paid.v1`(productType=TOOL, orderNo)
- THEN tool-service 消费该事件，对 osh_tool_user_quota 执行 increaseUserQuota（无记录则 insertUserQuota），并置 purchase_record.grant_status=1。

#### Scenario: 重复回调只发放一次

- GIVEN 同一订单的支付成功事件被重复投递
- WHEN tool-service 多次消费
- THEN grant_status CAS 幂等短路，点数配额只增加一次，不重复发放。

#### Scenario: 灰度无关的单一写者

- GIVEN 购买页由单体或 tool-service 服务（灰度命中与否）
- WHEN 支付成功触发发放
- THEN 发放始终经 tool-service 消费者执行，osh_tool_user_quota 不存在双写者，无双写竞态。

### Requirement: ES 索引事件兼容

tool-service SHALL 沿用 Outbox(TOOL 聚合) 向 topic `osh.tool.index` 发布 TOOL_INDEX_CREATE/UPDATE/DELETE/COUNTER 事件，topic 名与事件语义 SHALL 保持不变，Flink 索引同步不受影响。

#### Scenario: 工具变更后索引事件发布

- GIVEN 工具被新增/修改/删除或计数变更
- WHEN tool-service 完成本地事务
- THEN 经 Outbox 提交后向 `osh.tool.index` 发布对应事件，Flink 消费后 ES 索引与 MySQL 一致。

#### Scenario: ES 查询失败降级 MySQL

- GIVEN SearchEsProperties 开启但 ES 查询异常
- WHEN 用户调用 /pc/tool/search
- THEN 自动降级到 MySQL 查询返回结果，与拆分前降级行为一致。

### Requirement: 工具点数消耗防透支

tool-service SHALL 保留点数消耗的 DB 级 CAS 闸门，防止并发透支。

#### Scenario: 并发消耗不透支

- GIVEN 用户剩余点数有限的情况下并发调用 /pc/tool/use/consume
- WHEN 多个请求同时扣减
- THEN CAS 条件 `WHERE remaining_count >= quotaCost AND delete_flag=0` 保证扣减总量不超过余额，超额请求返回"工具点数不足"。

#### Scenario: check 通过但 consume 失败的 TOCTOU 处理

- GIVEN /pc/tool/use/check 返回可用后、/pc/tool/use/consume 前点数被其他请求扣完
- WHEN 调用 consume
- THEN consume 端 CAS 兜底返回点数不足，前端据契约处理"check 通过但 consume 失败"，不产生负数点数。

### Requirement: 绞杀双跑与路由级回滚

拆分 SHALL 保留单体工具代码不删，通过网关灰度分流；出现 P0 缺陷 SHALL 支持将 `/pc/tool/**` 秒级指回单体且无数据不一致。

#### Scenario: 路由级回滚

- GIVEN tool-service 已灰度部分流量且出现 P0 缺陷
- WHEN 运维在网关将 `/pc/tool/**` 指回单体
- THEN 流量秒级回到单体处理，因共库且 osh_tool_user_quota 单一写者，回滚不产生数据不一致或双写冲突。

#### Scenario: 发放异常预案

- GIVEN tool-service 事件消费发放出现异常
- WHEN 触发发放预案开关
- THEN 可临时恢复单体本地发放路径，保障用户点数发放不中断，恢复后再切回事件驱动。
