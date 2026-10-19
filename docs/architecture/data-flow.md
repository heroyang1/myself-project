---
title: 数据流与关键链路（Data Flow）
path: docs/architecture/data-flow.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 数据流与关键链路（Data Flow）

> 描述请求与数据在层、中间件之间怎么流转。类名/接口均已核对真实存在；深入设计以文末"权威设计文档"为准。分层规则见 [boundaries.md](./boundaries.md)。

## 1. HTTP 请求生命周期

```text
前端 -> [C端 /pc/** | B端后台接口]
     -> Spring Security 过滤器 / JWT 解析 / @Anonymous 放行
     -> Controller（参数校验、权限注解）
     -> Service（业务编排、事务）
     -> Mapper -> MySQL   |   -> Redis 缓存   |   -> ES 查询
     -> 统一响应体（AjaxResult / TableDataInfo / R）
```

- 权限：B 端 `@PreAuthorize("@ss.hasPermi(...)")`；C 端 `@OshUserLevel(N)` / `@Anonymous`。
- 写操作防重复提交：`@DistributeLock`（Redisson RLock）。
- 异常统一出口见 [../reference/error-codes.md](../reference/error-codes.md)。

## 2. 响应体约定（禁止混用）

| 场景 | 响应体 | 结构 |
|---|---|---|
| B 端单条增删改查 | `AjaxResult` | `{ code, msg, data }` |
| 分页列表 | `TableDataInfo` | `{ code, rows, total }`（`startPage()` → Service → `getDataTable()`） |
| C 端 `/pc` | `R<T>` | `{ code, msg, data }` |

## 3. 搜索数据流（业务表 → ES）

```text
业务表变更 -> 本地消息表（Outbox）-> Kafka -> Flink(backstage-flink) -> Elasticsearch
                                                    |
                                        查询失败自动降级回 MySQL
```

- 首页卡片额外用 XXL-JOB 定时同步 + a/b 双索引轮换。
- 权威设计：[`docs/design/课程模块本地事务表设计分享.md`](../design/课程模块本地事务表设计分享.md)、[`docs/design/工具模块flink 设计说明.md`](../design/工具模块flink%20设计说明.md)、[`docs/homepage/`](../homepage/)。

## 4. 支付与权益发放链路

```text
业务结算 -> OrderCheckoutService.checkout() -> 创建 osh_order + osh_payment
支付平台回调 -> PayNotifyController(/notify/pay, @Anonymous) -> orderService.handlePayNotify()
支付成功消息 -> Kafka(PAY_SUCCESS_TOPIC) -> PaymentSuccessConsumer
    -> OrderPaidHandlerRegistry.handle(productType)  （旧行为，发放失败不 ack 以重投）
    -> 或 entitlementTaskService（task 模式：任务表 + 定时兜底 + CAS）
```

- 业务模块只做商品校验与价格计算，支付逻辑委托统一订单模块。
- 主动查单与回调并发时，用 CAS 结果做幂等闸门。
- 权威文档：[`docs/modules/payment/业务模块接入支付模块说明.md`](../modules/payment/业务模块接入支付模块说明.md)、[`docs/design/工具模块订单超时处理设计.md`](../design/工具模块订单超时处理设计.md)。

## 5. 公告栏与实时通知

```text
业务事件（审核通过/支付成功）-> 写 osh_announcement（resource_type 区分模块）
    -> AnnouncementRefreshBroadcaster 广播 WebSocket 消息 type=ANNOUNCEMENT_REFRESH
    -> 前端收到"刷新信号"后重新请求 HTTP 接口拉取实际数据
```

- WebSocket 只发刷新信号，不承载业务数据。
- 权威文档：[`docs/announcement/Announcement-Backend-Integration.md`](../announcement/Announcement-Backend-Integration.md)。

## 6. 对象存储（R2/S3）

```text
上传：FileUploadUtils.upload(file, UploadPathEnum) -> Cloudflare R2/S3
读取：OssUtil 生成临时签名 URL（getLimitedUrl，带 TTL）
数据库：只存 objectKey，不存完整临时 URL（会过期导致裂图）
```

- 权威约定：[`docs/ai/conventions/object-storage.md`](../ai/conventions/object-storage.md)。

## 相关文档

- 分层与边界 → [boundaries.md](./boundaries.md)
- 中间件约定 → [`docs/ai/conventions/`](../ai/conventions/)（redis / kafka / elasticsearch / nacos / middleware）
