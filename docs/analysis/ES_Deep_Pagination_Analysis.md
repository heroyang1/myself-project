# Elasticsearch 深度分页问题分析与解决方案

> **生成日期**：2026-09-23
> **涉及模块**：backstage-system（实用网站、秒杀、电子书、信息差、课程、工具、反馈、审核）
> **技术栈**：Spring Boot 2.5.x、Elasticsearch RestHighLevelClient 7.x
> **关联文档**：[ES_MySQL_Consistency_Analysis.md](./ES_MySQL_Consistency_Analysis.md)

---

## 一、结论速览

项目中 **8 个 ES 搜索 Mapper 全部使用 `from` + `size` 分页模式**，这是 ES 深度分页问题的典型根源。

- **全局 grep 确认**：`search_after` / `Scroll` / `PointInTime(PIT)` 使用次数为 **0**
- **所有索引 JSON 均未配置 `max_result_window`**，使用 ES 默认值 **10000**
- **所有分页参数无 pageSize 上限校验**，无分页深度限制

**风险等级：P0（存在接口报错风险）～ P1（性能劣化）**

---

## 二、问题定位

### 2.1 受影响的查询点（8 处）

| 模块 | 文件 | 行号 | 分页代码 |
|------|------|------|---------|
| 实用网站 | `OshWebsiteEsMapper.java` | L188 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 秒杀明细 | `SeckillItemEsMapper.java` | L157 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 电子书 | `BookEsMapper.java` | L140 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 信息差 | `OshInfoGapEsMapper.java` | L161 / L181 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 课程 | `OshCourseEsMapper.java` | L145 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 工具 | `OshToolEsMapper.java` | L131 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 反馈系统 | `AssistantFeedbackEsMapper.java` | L301 | `.from((pageNum-1)*pageSize).size(pageSize)` |
| 审核列表 | `ResourceAuditEsMapper.java` | L91 | `.from((pageNum-1)*pageSize).size(pageSize)` |

### 2.2 无风险点（已正确设计）

| 模块 | 文件 | 说明 |
|------|------|------|
| 首页卡片 | `AbstractOshHomePageAliasEsMapper.java` L255 | `.from(0).size(limit)`，limit 上限 5（`maxLimit()`），属 Top-N 查询 |
| 会员套餐 | `HomepageMemberPlanEsMapper.java` L159 | `.from(0).size(QUERY_SIZE)`，固定 Top-N，无翻页 |

> 首页卡片场景仅取热门 Top-N，不存在深度翻页需求，设计合理，无需改造。

---

## 三、深度分页的具体危害

### 3.1 性能随 `from` 线性劣化

ES 的 `from + size` 机制要求**每个分片**都收集并排序 `from + size` 条文档，协调节点再归并排序、丢弃前 `from` 条：

```
翻到第 1000 页（size=10）：
  单分片需处理 from+size = 10000 条文档
  协调节点归并 10000 条后丢弃前 9990 条
  实际只返回 10 条 → 内存/CPU 开销放大 1000 倍
```

### 3.2 硬上限 `max_result_window = 10000`

所有索引 JSON（如 `osh_practical_website_index.json`）均未显式配置 `max_result_window`，使用默认值 10000。当 `from + size > 10000` 时 ES 直接抛异常：

```
Result window is too large, from + size must be less than or equal to: [10000]
but was [10010]. This limit can be set by changing the [index.max_result_window]
index level setting.
```

### 3.3 pageSize 无上限校验（风险放大器）

以 `AssistantFeedbackEsMapper.resolvePageSize()`（L456）为例：

```java
private int resolvePageSize(AssistantFeedbackPageDTO dto) {
    if (dto == null || dto.getPageSize() == null || dto.getPageSize() < 1) {
        return 10;
    }
    return dto.getPageSize();   // ❌ 无最大值封顶
}
```

客户端传入 `pageSize=20000` 时，即使第 1 页也会立即触发 `max_result_window` 异常。其他模块的 `resolvePageSize` 同理。

### 3.4 异常传播差异

| 模块 | ES 深分页报错后的行为 |
|------|---------------------|
| 网站/课程/电子书/秒杀 | catch 降级到 MySQL（但 MySQL `LIMIT offset,size` 深分页同样慢，只是不报错） |
| **反馈系统** | `searchFeedbacks()` **无降级**，直接抛出 `IllegalStateException` → 接口 500 |

---

## 四、解决方案

### 方案 A：`search_after` 游标分页（推荐 · C 端无限滚动/翻页）

**核心思路**：用上一页最后一条文档的排序值作为游标，去掉 `from`，性能恒定不随深度劣化。

```java
// 以 OshWebsiteEsMapper 为例
private SearchSourceBuilder buildSearchSource(WebsiteQueryDTO request, Object[] searchAfter) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .size(request.getPageSize())   // ✅ 不再设置 from
            .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));
    // ... query 构建不变 ...
    sourceBuilder.sort(SortBuilders.fieldSort("ratingScore").order(SortOrder.DESC));
    // ✅ 关键：追加唯一 tiebreaker，否则排序值相同的文档会漏读/重复
    sourceBuilder.sort(SortBuilders.fieldSort("id").order(SortOrder.DESC));

    if (searchAfter != null && searchAfter.length > 0) {
        sourceBuilder.searchAfter(searchAfter);
    }
    return sourceBuilder;
}
```

**返回游标给前端**：

```java
// 取本页最后一条 hit 的 sort 值作为下一页游标
SearchHit[] hits = searchResponse.getHits().getHits();
if (hits.length > 0) {
    Object[] nextCursor = hits[hits.length - 1].getSortValues();
    // 序列化 nextCursor 返回给前端，前端下次请求回传
}
```

**前置改造点（重要）**：当前多数 Mapper 的排序字段**缺少唯一 tiebreaker**：

| 模块 | 现有排序 | 需补充 |
|------|---------|--------|
| 网站 | `ratingScore` | + `id` |
| 课程 | `createTime` | + `id` |
| 秒杀 | `sort` + `seckillPrice` | + `id` |
| 工具 | `hotScore` + `viewCount` + `createTime` | + `id` |
| 信息差 | `scoreSort` + `updateTime` | + `id` |
| 反馈 | 多级排序（`appendSorts`） | + `id` |

> 不补 tiebreaker 会导致排序值相同的文档在翻页边界处丢失或重复。

**适用性**：`search_after` 只能顺序向前翻页，**不能跳转到"第 N 页"**。适合 C 端"加载更多"，不适合需要随机跳页的管理后台。

---

### 方案 B：`PIT` + `search_after`（推荐 · 需数据快照一致性时）

`search_after` 配合 Point In Time 可保证分页期间数据视图一致，避免翻页时新数据插入导致的错位。ES 7.10+ 支持。

```java
// 1. 开启 PIT（有效期如 2 分钟）
Request pitRequest = new Request("POST", "/" + index + "/_pit");
pitRequest.addParameter("keep_alive", "2m");
Response pitResponse = restHighLevelClient.getLowLevelClient().performRequest(pitRequest);
String pitId = parsePitId(pitResponse);

// 2. 查询时携带 PIT（注意：带 PIT 时 SearchRequest 不指定 index）
SearchRequest searchRequest = new SearchRequest();
searchRequest.source(new SearchSourceBuilder()
        .size(pageSize)
        .pit(new PointInTimeBuilder(pitId).setKeepAlive(TimeValue.timeValueMinutes(2)))
        .searchAfter(cursor)
        .sort(SortBuilders.fieldSort("id").order(SortOrder.DESC)));

// 3. 分页结束后关闭 PIT 释放资源
Request closePit = new Request("DELETE", "/_pit");
closePit.setJsonEntity("{\"id\":\"" + pitId + "\"}");
```

**适用场景**：数据实时性要求高、翻页期间不允许错位的场景（如竞价、库存列表）。

---

### 方案 C：限制分页深度 + 封顶 pageSize（必做兜底 · 改动最小）

无论是否上 `search_after`，都应立即补充参数校验，消除 `max_result_window` 报错风险：

```java
// 1. 封顶 pageSize
private int resolvePageSize(AssistantFeedbackPageDTO dto) {
    int size = (dto == null || dto.getPageSize() == null || dto.getPageSize() < 1)
            ? 10 : dto.getPageSize();
    return Math.min(size, 100);   // ✅ 封顶，防止超大 pageSize
}

// 2. 校验分页深度
private static final int MAX_RESULT_WINDOW = 10000;

if ((long) pageNum * pageSize > MAX_RESULT_WINDOW) {
    throw new ServiceException("查询页码过深，请缩小筛选范围后重试");
    // 或：自动切换到 search_after 游标模式
}
```

---

### 方案 D：`Scroll` API（仅用于全量导出/批处理）

`Scroll` 适合一次性导出全部数据（数据迁移、报表），**不适合实时用户分页**：

- 会占用快照资源、维护 scroll 上下文，高并发下开销大
- 已被 PIT 取代（ES 官方推荐）
- 项目中若有后台全量导出需求可用，但**严禁用于线上 C 端查询**

---

## 五、方案选型对比

| 方案 | 性能 | 支持跳页 | 一致性 | 改造成本 | 适用场景 |
|------|------|---------|--------|---------|---------|
| `from+size`（现状） | 随深度劣化 | ✅ | 弱 | - | 浅分页（<100页） |
| **A. search_after** | 恒定 | ❌ 仅顺序 | 中 | 中 | C 端无限滚动 |
| **B. PIT+search_after** | 恒定 | ❌ 仅顺序 | 强 | 中高 | 一致性要求高的翻页 |
| **C. 深度限制** | - | ✅ | - | 极低 | 兜底防护（必做） |
| D. Scroll | 好 | ❌ | 强 | 中 | 全量导出/批处理 |

---

## 六、实施优先级与改动清单

| 优先级 | 措施 | 涉及文件 | 预估改动 |
|--------|------|---------|---------|
| **P0** | 封顶 pageSize + 深度校验（方案 C） | 8 个 EsMapper 的 `resolvePageSize` / 查询入口 | 每模块 ~5 行 |
| **P0** | 反馈系统补 MySQL 降级 | `AssistantFeedbackEsServiceImpl.searchFeedbacks()` | ~30 行 |
| **P1** | C 端高频列表改 `search_after`（方案 A） | 网站/课程/信息差/秒杀 EsMapper + 对应 DTO/VO | 每模块 ~40 行 |
| **P1** | 补充排序 tiebreaker（`id`） | 上述 4 个 Mapper 的 `buildSearchSource` | 每模块 ~2 行 |
| **P2** | 管理后台深分页评估 PIT（方案 B） | 审核/工具管理端 Mapper | 视方案而定 |
| **P3** | 索引 JSON 显式声明 `max_result_window` | `resources/es/*.json` | 每文件 ~1 行 |

---

## 七、最佳实践建议

1. **C 端搜索优先 `search_after`**：无限滚动/加载更多场景，彻底规避深度分页
2. **管理后台限制深度**：后台跳页需求用方案 C 封顶（如最多 100 页），配合精确筛选条件缩小结果集
3. **所有排序必须有唯一 tiebreaker**：末级排序统一追加 `id`，为 `search_after` 打好基础，同时避免排序抖动
4. **参数校验前置**：`pageSize` 封顶、`from+size` 深度校验作为通用防护，统一在查询入口拦截
5. **导出走 Scroll/PIT，不走 from+size**：全量导出场景独立处理，不复用线上分页逻辑
6. **`max_result_window` 显式化**：在索引 JSON 中声明，避免依赖默认值，便于运维按数据规模调优

---

## 附录：ES 分页机制原理

### `from + size` 执行流程

```
协调节点
  ├─ 分片1：查询 → 排序 → 返回 top(from+size) 条
  ├─ 分片2：查询 → 排序 → 返回 top(from+size) 条
  └─ 分片N：查询 → 排序 → 返回 top(from+size) 条
       ↓
  协调节点归并所有分片的 (from+size) 条 → 全局排序 → 丢弃前 from 条 → 返回 size 条
```

**开销**：`N(分片数) × (from + size)` 条文档的排序与网络传输，`from` 越大开销越大。

### `search_after` 执行流程

```
协调节点
  ├─ 分片1：从游标位置向后取 size 条
  ├─ 分片2：从游标位置向后取 size 条
  └─ 分片N：从游标位置向后取 size 条
       ↓
  协调节点归并 → 返回 size 条 + 新游标
```

**开销**：`N × size` 条，与翻页深度无关，性能恒定。代价是牺牲随机跳页能力。
