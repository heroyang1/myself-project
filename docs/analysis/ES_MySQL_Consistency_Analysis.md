# Elasticsearch 与 MySQL 数据一致性保障分析报告

> **生成日期**：2026-09-23  
> **涉及模块**：backstage-system、backstage-flink、backstage-quartz、backstage-common  
> **技术栈**：Spring Boot 2.5.x、Elasticsearch RestHighLevelClient、Flink Kafka Connector、XXL-Job、Kafka

---

## 一、同步策略全景

项目共存在 **4 种 ES 同步模式**，覆盖 10+ 业务模块：

| 同步模式 | 模块 | 一致性等级 | 数据延迟 |
|---------|------|-----------|---------|
| **Outbox + Kafka + Flink** | 课程、工具、秒杀明细、拼团活动 | 高（事务发件箱） | 秒级～分钟级 |
| **实时同步双写** | 反馈系统、实用网站、信息差、电子书 | 中（依赖 ES 可用性） | 毫秒级 |
| **定时全量同步** | 首页卡片（网站/开源项目/信息差/会员套餐等） | 高（A/B 别名切换） | 5 分钟 |
| **Fire-and-forget Kafka** | 拼团活动索引 | 低（消息可丢失） | 秒级～不可达 |

---

## 二、模式 A：Outbox + Kafka + Flink（事务同步）

### 2.1 架构链路

```
MySQL 写入（@Transactional）
    ↓ 同一事务
OutboxEventService.saveEvent() → osh_outbox_event 表（status=PENDING）
    ↓ 事务提交后
TransactionSynchronization.afterCommit()
    ↓ 立即投递
OutboxEventPublisher.publishEventById() → KafkaMessageUtil.sendMessage()
    ↓ 投递失败 → 等待定时任务兜底
OutboxEventPublishTask（XXL-Job: outboxJobHandler）
    ↓ 批量扫描 PENDING 事件
Kafka Topic → Flink Job → ES IndexRequest
```

### 2.2 涉及模块与文件

| 模块 | Outbox 写入入口 | Flink Job | ES 索引 |
|------|----------------|-----------|---------|
| 课程 | `OutboxEventServiceImpl.saveCourseIndexEvent()` | `CourseSearchIndexSyncJob` | `osh_course_search_read` |
| 工具 | `OutboxEventServiceImpl.saveToolIndexEvent()` | `ToolSearchIndexSyncJob` | `osh_tool_index` |
| 秒杀明细 | `OutboxEventServiceImpl.saveSeckillItemIndexEvent()` | `SeckillItemIndexSyncJob` | `osh_seckill_item_search` |

### 2.3 一致性保障机制

**✅ 事务原子性**：Outbox 写入与业务写入在同一 `@Transactional` 中，保证"MySQL 成功 = 事件落库"。

**✅ 提交后投递**：`publishAfterCommit()` 注册 `TransactionSynchronization.afterCommit()`，确保只在事务成功后才投递 Kafka。

**✅ 定时兜底**：`OutboxEventPublishTask`（outboxJobHandler）定期扫描 PENDING 事件，补偿即时投递失败的情况。

**✅ 指数退避重试**：失败后自动重试，最多 10 次，间隔 1min → 5min → 15min → 1h，超过后标记 DEAD。

**✅ 超时恢复**：`recoverTimeoutSendingEvents()` 自动恢复 SENDING 状态超时（10 分钟）的事件。

**✅ 幂等写入**：ES 文档 ID = 业务主键（courseId/toolId/itemId），重复写入自动覆盖，不会产生重复文档。

**✅ Flink Checkpoint**：所有 Flink Job 均开启 Checkpoint，offset 在 Checkpoint 时提交，保证 AT_LEAST_ONCE 语义。

### 2.4 风险点

**R1 - Flink AT_LEAST_ONCE 可能重复消费**
- **风险**：Flink Checkpoint 失败时，offset 未提交，重启后重复消费
- **影响**：ES 收到重复 upsert，但由于文档 ID = 业务 ID，覆盖写入不会产生脏数据
- **结论**：**已天然幂等，无实际风险**

**R2 - Outbox 即时投递失败 + 定时任务间隔窗口**
- **风险**：`publishSafely()` 失败后，需等待下一轮 `outboxJobHandler` 执行（取决于 XXL-Job 调度间隔）
- **影响**：ES 数据延迟 = outboxJobHandler 调度间隔 + 重试间隔
- **建议**：确认 outboxJobHandler 的 Cron 间隔（建议 ≤ 1 分钟）

---

## 三、模式 B：实时同步双写

### 3.1 架构链路

```
MySQL 写入成功
    ↓ 同一请求（非事务）
直接调用 ES upsert/delete API
    ↓ 成功 → 返回
    ↓ 失败 → 行为因模块而异（见下文分析）
```

### 3.2 各模块降级策略对比

| 模块 | 写入方法 | ES 失败行为 | 风险等级 |
|------|---------|------------|---------|
| **反馈系统** | `upsertFeedbackWithRebuildMirror()` | **抛出异常**，中断主流程 | ⚠️ P2 |
| **实用网站** | `WebsiteEsService.saveToEs()` | **catch + log.warn**，不影响主流程 | ✅ 安全 |
| **网站评价计数** | `WebsiteEsService.syncCountsToEs()` | **catch + log.warn**，局部更新 | ✅ 安全 |
| **信息差** | `InfoGapEsServiceImpl.syncInfoGapToEs()` | **抛出异常**，中断主流程 | ⚠️ P2 |
| **电子书** | `BookEsServiceImpl.syncBookToEs()` | **catch + log.warn**，不影响主流程 | ✅ 安全 |

### 3.3 风险点

**P2 - 反馈系统 ES 写入失败阻断主业务**

- **文件**：`AssistantFeedbackEsServiceImpl.java` L86-L103
- **问题**：`upsertFeedbackById()` 调用 `upsertFeedbackWithRebuildMirror()`，ES 写入失败时抛出 `IllegalStateException`，导致整个反馈创建/更新请求失败
- **影响**：ES 不可用时，用户无法提交反馈
- **修复建议**：ES 写入失败时 catch 异常并 log.warn，不阻断 MySQL 主流程；或改为异步投递

```java
// 修复方案：ES 写入降级为异步 + 日志
try {
    upsertFeedbackWithRebuildMirror(buildDocument(feedback, categoryMap, tagMap));
} catch (Exception e) {
    log.error("反馈同步 ES 失败，不影响主流程，feedbackId={}", feedbackId, e);
    // 可选：发送补偿消息到 outbox，等待后续全量同步修复
}
```

**P2 - 信息差 ES 写入失败阻断主业务**

- **文件**：`InfoGapEsServiceImpl.java` L108-L118
- **问题**：`syncInfoGapToEs()` 抛出 `IllegalStateException`，上层调用方若未 catch 则中断
- **修复建议**：同反馈系统，降级为 catch + log

**P1 - 双写失败无补偿机制**

- **涉及模块**：网站、电子书、信息差
- **问题**：ES 写入失败后仅打日志，没有重试或补偿机制。数据将持续不一致，直到下次全量同步（如果有的话）
- **修复建议**：
  1. 引入定时对账任务，比较 MySQL 与 ES 数据差异
  2. 或将这些模块也迁移到 Outbox 模式

---

## 四、模式 C：定时全量同步（首页卡片）

### 4.1 架构链路

```
XXL-Job: homepage-card-es-sync（cron: 0 0/5 * * * ?）
    ↓
OshHomePageCardEsSyncOrchestrator.syncAll()
    ↓ 遍历所有卡片服务
AbstractOshHomePageEsCardService.syncToEs()
    ↓
1. 解析当前别名指向的物理索引（currentIndex）
2. 计算目标物理索引（targetIndex = A 或 B 交替）
3. 从 MySQL 查询数据 → 构建 ES 文档
4. 创建目标索引（如不存在）
5. 清空目标索引（deleteAll）
6. 批量写入（bulkUpsert）
7. 校验目标索引（validateIndex：文档数 + sortNo + syncBatchId）
8. 原子切换别名（switchAlias：remove old + add new）
```

### 4.2 涉及卡片模块

| 卡片类型 | 服务类 | ES Mapper |
|---------|-------|-----------|
| 热门网站 | `OshHomePageWebsiteServiceImpl` | `OshHomePageHotWebsiteEsMapper` |
| 开源项目 | `OshHomePageOpenProjectServiceImpl` | `OshHomePageHotOpenProjectEsMapper` |
| 信息差 | `OshHomePageInfoGapServiceImpl` | `OshHomePageHotInfoGapEsMapper` |
| 会员套餐 | `OshHomePageMemberPlanServiceImpl` | `HomepageMemberPlanEsMapper`（独立实现） |

### 4.3 一致性保障机制

**✅ A/B 索引 + 别名原子切换**：`AbstractOshHomePageAliasEsMapper` 维护 indexA / indexB 两个物理索引，通过 ES `_aliases` API 原子切换，读写分离，零停机重建。

**✅ 批次校验**：`validateIndex()` 在切换前验证目标索引的文档数量、排序编号（sortNo）和批次标识（syncBatchId），确保数据完整性。

**✅ 自动初始化**：`ensureIndexReady()` 在 ES 索引/别名不存在时自动触发初始化，避免首次部署时查询失败。

**✅ 单卡失败不中断批量**：`OshHomePageCardEsSyncOrchestrator.syncAll()` 中单卡失败记录为 -1，不影响其他卡片同步。

**✅ ES 优先 + MySQL 兜底读取**：`getCards()` 先查 ES，ES 不可用或返回空时自动降级到 MySQL。

### 4.4 风险点

**P3 - 5 分钟数据延迟**
- **问题**：定时任务每 5 分钟执行一次，ES 数据最多落后 MySQL 5 分钟
- **影响**：首页卡片展示的数据可能不是最新的
- **评估**：首页卡片为运营展示位，5 分钟延迟可接受
- **如需优化**：可增加增量同步（监听 MySQL 变更事件），仅在数据变更时触发全量

**P3 - 会员套餐模块未使用 A/B 别名**
- **文件**：`HomepageMemberPlanEsMapper.java`
- **问题**：会员套餐使用单索引 `osh_homepage_member_plan`，全量同步时先 `deleteAll()` 再 `bulkUpsert()`，存在数据空窗期
- **影响**：syncAll 期间查询可能返回空结果
- **建议**：迁移到 `AbstractOshHomePageAliasEsMapper` 基类，复用 A/B 别名机制

---

## 五、模式 D：Fire-and-forget Kafka（拼团活动）

### 5.1 架构链路

```
MySQL 写入成功
    ↓ 异步
GroupEventPublisher.publishActivityIndexEvent()
    ↓ CompletableFuture.runAsync（fire-and-forget）
KafkaMessageUtil.sendMessage() → Kafka Topic
    ↓
Flink: GroupActivityIndexSyncJob → ES
```

### 5.2 风险点

**P0 - 消息丢失无感知**

- **文件**：`GroupEventPublisher.java` L167-L180
- **问题**：`sendAsync()` 使用 `CompletableFuture.runAsync()` 异步发送 Kafka，失败仅 `log.error()`，**无重试、无持久化、无兜底**
- **影响**：Kafka 不可用或网络抖动时，ES 索引事件永久丢失
- **对比**：课程/工具/秒杀模块已使用 Outbox 模式（事务发件箱），拼团模块是唯一未接入 Outbox 的异步同步模块
- **修复建议**：

```java
// 修复方案：将拼团索引事件接入 Outbox
public void publishActivityIndexEvent(Object activity, String eventType) {
    // 不再 fire-and-forget，改为写入 outbox 表
    outboxEventService.saveGroupActivityIndexEvent(activityId, message, operator);
}
```

---

## 六、降级查询机制全景

### 6.1 全局开关

```yaml
search:
  es:
    enabled: true           # ES 全局开关，false 时所有查询直接走 MySQL
    fallback-timeout-millis: 800  # ES 查询超时阈值
```

配置类：`SearchEsProperties.java`

### 6.2 各模块降级策略

| 模块 | 降级入口 | ES 失败行为 | MySQL 兜底 |
|------|---------|------------|-----------|
| 首页卡片 | `AbstractOshHomePageEsCardService.getCards()` | catch → MySQL | ✅ |
| 实用网站 | `OshPracticalWebsiteServiceImpl.selectWebsitePage()` | ES 返回 null → MySQL | ✅ + 回填实时计数 |
| 课程搜索 | `OshCourseController.courseSearch()` | catch → MySQL | ✅ |
| 电子书 | `BookController.list()` | catch → MySQL | ✅ |
| 秒杀活动 | `SeckillUserController.activeList()` | catch → MySQL | ✅ + 空结果智能检测 |
| 信息差 | `InfoGapESController.searchByEs()` | catch → MySQL | ✅ |
| 反馈系统 | `AssistantFeedbackEsServiceImpl.searchFeedbacks()` | **抛出异常** | ❌ 无兜底 |
| 工具搜索 | `OshToolEsMapper` | 抛出异常 | 待确认 |

### 6.3 降级策略亮点

**✅ 实用网站 ES→MySQL 回填实时计数**：ES 返回快照数据后，`fillCountsFromDb()` 从 MySQL 回填最新的好/中/差评计数和收藏数，保证计数实时性。

**✅ 秒杀空结果智能检测**：`SeckillUserController.shouldFallbackAfterEsQuery()` 检测 ES 返回空结果时，额外查 MySQL 判断是"真的没数据"还是"ES 异常导致空结果"，避免误判。

**✅ 未发布课程走 MySQL**：`OshCourseController` 中 `includeUnpublished=true` 时直接走 MySQL，避免 ES 索引延迟导致新建审核中课程不可见。

### 6.4 风险点

**P1 - 反馈系统搜索无 MySQL 兜底**

- **文件**：`AssistantFeedbackEsServiceImpl.java` L61-L74
- **问题**：`searchFeedbacks()` 直接调用 `assistantFeedbackEsMapper.searchFeedbacks()`，ES 不可用时抛出 `IllegalStateException`，无 MySQL 降级
- **影响**：ES 不可用时反馈列表完全不可用
- **修复建议**：增加 MySQL 降级查询（参考其他模块的 try-catch-fallback 模式）

---

## 七、索引重建安全性对比

### 7.1 安全等级分类

| 等级 | 机制 | 数据空窗期 | 模块 |
|------|------|-----------|------|
| **✅ 零停机** | A/B 索引 + 别名原子切换 | 无 | 首页卡片、反馈系统 |
| **⚠️ 有窗口期** | 删旧建新 + 全量同步 | 有 | 网站、秒杀、电子书、信息差 |

### 7.2 零停机重建（标杆）

**反馈系统 `AssistantFeedbackEsServiceImpl.rebuildIndex()`**：

1. 分布式锁防并发（`@DistributeLock(scene="feedback:es", key="rebuild", waitTime=0, expireTime=10min)`）
2. 解析当前物理索引 → 创建下一代物理索引
3. 在 Redis 中标记重建目标索引（`REBUILD_TARGET_INDEX_CACHE_KEY`，TTL=30min）
4. 全量同步 MySQL 数据到新索引
5. 原子切换别名（`switchAlias`）
6. **重建期间的实时写入通过 Rebuild Mirror 机制双写**：`upsertFeedbackWithRebuildMirror()` 同时写入在线索引和重建目标索引
7. 失败清理：`cleanupFailedRebuildTarget()` 删除失败的目标索引
8. finally 清除 Redis 标记

**首页卡片 `AbstractOshHomePageEsCardService.syncToEs()`**：

1. 解析当前别名指向 → 计算 A/B 目标索引
2. 创建目标索引 → 清空 → 批量写入
3. `validateIndex()` 校验文档数 + sortNo + syncBatchId
4. 原子切换别名
5. 失败时旧索引不受影响，查询仍走旧索引

### 7.3 有窗口期重建（风险）

**网站 `WebsiteEsService.recreateIndex()`**：

```java
// 1. 删除旧索引（此时 ES 查询将失败）
restHighLevelClient.indices().delete(deleteIndexRequest, ...);
// 2. 创建新索引
restHighLevelClient.indices().create(createIndexRequest, ...);
// 3. 需要手动调用 /syncAll 重新同步数据
```

**问题**：
- 删除索引到重新同步完成之间，ES 查询返回空或 404
- 虽然降级机制会 fallback 到 MySQL，但窗口期搜索能力丧失
- 没有 A/B 别名机制，无法做到零停机

**秒杀 `SeckillItemEsServiceImpl.syncAllItemsToEs()`**：

```java
// 1. 清空索引
seckillItemEsMapper.deleteAllItems();
// 2. 批量写入
seckillItemEsMapper.bulkUpsertItems(documents);
```

**问题**：`deleteAllItems()` 后到 `bulkUpsertItems()` 完成前，查询返回空结果。

**电子书/信息差**：同上，先 `deleteAll` 再 `bulkUpsert`。

### 7.4 修复建议

对于有窗口期的模块，建议统一迁移到 A/B 别名模式：

```java
// 参考 AbstractOshHomePageAliasEsMapper 的设计
// 1. 维护 indexA / indexB 两个物理索引
// 2. 读请求通过别名路由
// 3. 重建时写入另一个物理索引，完成后原子切换别名
```

---

## 八、一致性风险汇总矩阵

| 编号 | 风险等级 | 模块 | 问题描述 | 涉及文件 | 修复方案 | 预估改动 |
|------|---------|------|---------|---------|---------|---------|
| **R1** | **P0** | 拼团活动 | `GroupEventPublisher.sendAsync()` fire-and-forget，Kafka 发送失败事件永久丢失 | `GroupEventPublisher.java` | 接入 Outbox 事务发件箱 | ~50 行 |
| **R2** | **P1** | 反馈系统 | `searchFeedbacks()` 无 MySQL 降级，ES 不可用时反馈列表不可用 | `AssistantFeedbackEsServiceImpl.java` | 增加 try-catch + MySQL fallback | ~30 行 |
| **R3** | **P1** | 反馈系统 | `upsertFeedbackById()` ES 写入失败抛出异常，阻断反馈创建主流程 | `AssistantFeedbackEsServiceImpl.java` | 降级为 catch + log + 补偿 | ~15 行 |
| **R4** | **P1** | 信息差 | `syncInfoGapToEs()` ES 写入失败抛出异常，阻断主流程 | `InfoGapEsServiceImpl.java` | 降级为 catch + log | ~5 行 |
| **R5** | **P2** | 网站/秒杀/电子书/信息差 | 全量同步采用 deleteAll → bulkUpsert，存在数据空窗期 | 各模块 EsMapper/EsService | 迁移到 A/B 别名模式 | 每个模块 ~100 行 |
| **R6** | **P2** | 会员套餐 | 单索引全量同步，未使用 A/B 别名 | `HomepageMemberPlanEsMapper.java` | 迁移到 `AbstractOshHomePageAliasEsMapper` | ~80 行 |
| **R7** | **P2** | 网站/电子书 | 双写失败无补偿，ES 数据持续不一致直到下次全量同步 | `WebsiteEsService.java` / `BookEsServiceImpl.java` | 增加定时对账或接入 Outbox | ~50 行/模块 |
| **R8** | **P3** | 首页卡片 | 5 分钟定时同步延迟 | `OshHomePageCardEsSyncTask` | 可增加增量同步触发 | 视方案而定 |

---

## 九、优化方案建议

### 9.1 短期（1～2 周）

1. **拼团活动接入 Outbox**（R1）
   - 在 `OutboxEventService` 中新增 `saveGroupActivityIndexEvent()`
   - 修改 `GroupEventPublisher.publishActivityIndexEvent()` 改为写入 outbox 表
   - 新建 Flink Job 或复用现有 `GroupActivityIndexSyncJob`

2. **反馈系统增加 MySQL 降级**（R2）
   - `searchFeedbacks()` 增加 try-catch，ES 失败时 fallback 到 MySQL 分页查询
   - 参考 `OshPracticalWebsiteServiceImpl.selectWebsitePage()` 的实现模式

3. **双写失败降级为 warn 日志**（R3/R4）
   - `AssistantFeedbackEsServiceImpl.upsertFeedbackById()` 的 ES 写入改为 catch + log
   - `InfoGapEsServiceImpl.syncInfoGapToEs()` 同上

### 9.2 中期（2～4 周）

4. **统一全量同步模块到 A/B 别名模式**（R5/R6）
   - 为网站、秒杀、电子书、信息差、会员套餐模块创建对应的 `AliasEsMapper`
   - 统一使用 `syncToEs()` 模板方法：create → delete → upsert → validate → switchAlias

5. **增加 ES-MySQL 数据对账任务**（R7）
   - 定时任务对比 MySQL 记录数与 ES 文档数
   - 差异超过阈值时告警或自动触发全量同步

### 9.3 长期

6. **引入 CDC（Change Data Capture）替代双写**
   - 使用 Debezium 监听 MySQL binlog，自动将变更事件投递到 Kafka
   - 彻底消除业务代码中的双写逻辑
   - 适用于所有需要 ES 搜索的模块

---

## 十、项目最佳实践总结

### 10.1 一致性保障标杆

| 机制 | 实现位置 | 说明 |
|------|---------|------|
| **事务发件箱（Outbox）** | `OutboxEventServiceImpl` | MySQL 事务内写入事件表，提交后投递 Kafka，定时任务兜底 |
| **A/B 别名切换** | `AbstractOshHomePageAliasEsMapper` | 两个物理索引交替使用，别名原子切换，零停机重建 |
| **Rebuild Mirror** | `AssistantFeedbackEsServiceImpl` | 重建期间实时写入双写到新索引，切换后数据完整 |
| **批次校验** | `validateIndex()` | 切换前验证文档数 + sortNo + syncBatchId |
| **分布式锁防并发** | `@DistributeLock` | 重建操作加锁，waitTime=0 避免排队重复执行 |
| **ES 超时控制** | `SearchSourceBuilder.timeout()` | 所有 ES 查询设置 800ms 超时，防止慢查询拖垮服务 |
| **全局开关** | `SearchEsProperties.enabled` | 一键关闭 ES，全部降级到 MySQL |

### 10.2 设计原则

1. **MySQL 是数据源真相（Source of Truth）**：ES 始终作为 MySQL 的搜索加速层，MySQL 数据优先
2. **ES 可降级不可依赖**：所有搜索场景都必须有 MySQL 兜底，ES 不可用时不阻断核心业务
3. **写入幂等**：ES 文档 ID = 业务主键，重复写入自动覆盖
4. **重建零停机**：使用 A/B 索引 + 别名切换，避免数据空窗期
5. **异步优先于双写**：通过 Outbox + Kafka + Flink 异步同步，避免 ES 写入延迟影响主流程 RT

---

## 附录 A：同步链路文件索引

| 文件 | 路径 | 职责 |
|------|------|------|
| `OutboxEventServiceImpl` | `backstage-system/.../service/impl/outbox/` | 事务发件箱，写入 outbox 表 |
| `OutboxEventPublishTask` | `backstage-quartz/.../task/` | XXL-Job 定时投递 outbox 事件到 Kafka |
| `GroupEventPublisher` | `backstage-system/.../service/servergroup/event/` | 拼团事件 fire-and-forget 发布 |
| `SeckillItemIndexSyncJob` | `backstage-flink/.../seckill/` | 秒杀明细 Kafka→ES Flink Job |
| `GroupActivityIndexSyncJob` | `backstage-flink/.../group/` | 拼团活动 Kafka→ES Flink Job |
| `CourseSearchIndexSyncJob` | `backstage-flink/.../course/` | 课程 Kafka→ES Flink Job |
| `ToolSearchIndexSyncJob` | `backstage-flink/.../tool/` | 工具 Kafka→ES Flink Job |
| `AbstractOshHomePageEsCardService` | `backstage-system/.../service/homepage/es/` | 首页卡片定时全量同步模板 |
| `AbstractOshHomePageAliasEsMapper` | `backstage-system/.../mapper/homepage/` | A/B 别名 ES 读写底座 |
| `AssistantFeedbackEsServiceImpl` | `backstage-system/.../service/assistant/impl/` | 反馈系统 ES 服务（含 rebuild） |
| `WebsiteEsService` | `backstage-system/.../service/website/impl/` | 实用网站 ES 服务 |
| `BookEsServiceImpl` | `backstage-system/.../service/impl/book/` | 电子书 ES 服务 |
| `InfoGapEsServiceImpl` | `backstage-system/.../service/impl/info_gap/` | 信息差 ES 服务 |
| `SeckillItemEsServiceImpl` | `backstage-system/.../service/impl/seckill/` | 秒杀明细 ES 服务 |
| `SearchEsProperties` | `backstage-system/.../config/properties/` | ES 全局开关与超时配置 |
| `OshHomePageCardEsSyncTask` | `backstage-quartz/.../task/` | 首页卡片 XXL-Job 同步任务 |
| `SeckillActivityTask` | `backstage-quartz/.../task/` | 秒杀活动状态流转 + ES 事件 |

## 附录 B：ES 索引命名一览

| 索引名 | 模块 | 别名 | 重建方式 |
|--------|------|------|---------|
| `osh_course_search_read` | 课程 | 无 | deleteAll + bulkUpsert |
| `osh_tool_index` | 工具 | 无 | deleteAll + bulkUpsert |
| `osh_seckill_item_search` | 秒杀明细 | 无 | deleteAll + bulkUpsert |
| `osh_practical_website` | 实用网站 | 无 | delete + create + syncAll |
| `osh_infogap_search_index` | 信息差 | 无 | delete + create + syncAll |
| `osh_book_search` | 电子书 | 无 | deleteAll + bulkUpsert |
| `osh_feedback_read_{v}` | 反馈系统 | `osh_feedback_read` | A/B 别名切换 |
| `osh_homepage_{card}_a/b` | 首页卡片 | `osh_homepage_{card}_read` | A/B 别名切换 |
