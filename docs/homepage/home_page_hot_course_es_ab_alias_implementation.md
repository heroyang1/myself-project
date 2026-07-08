# 首页热门课程 ES 双索引轮换可落地实现说明

## 1. 文档目标

本文档用于指导首页热门课程卡片从当前的：

- ES 优先 + MySQL 兜底
- 单索引写入
- 先删后写

升级为更安全的实现：

- `a/b` 双索引轮换
- 读 alias 原子切换
- 固定 5 条首页卡片
- ES 只存原始 `coverKey`
- 读取 ES 后再生成封面临时签名 URL
- 每轮同步生成 `syncBatchId`
- 全链路可观测日志

本文档面向开发落地，重点描述：

- 改哪些类
- 新增哪些方法
- 同步流程怎么跑
- alias 怎么切
- 日志怎么打
- 如何排查问题

## 2. 要解决的问题

当前首页热门课程实现已经接入 ES，但有两个关键问题：

### 2.1 封面 URL 会过期

当前 MySQL 查询结果在进入 ES 同步前，已经调用了：

- `ossService.getLimitedUrl(vo.getCover(), 1440)`

这样写入 ES 的 `cover` 实际上是 1440 分钟有效的临时 URL，而不是原始 OSS key。

后果：

- 同步后短期可用
- 过期后 ES 仍返回旧 URL
- 首页卡片封面失效

### 2.2 先删后写存在清空风险

当前同步流程是：

1. 查 MySQL Top 5
2. 删除索引内全部文档
3. 批量写入新文档

如果第 2 步成功、第 3 步失败，则 ES 会被清空。

虽然首页读接口还能回落 MySQL，但：

- ES 路径持续返回空
- 线上排查成本高
- 同步过程不够安全

## 3. 改造目标

本次改造目标如下：

1. 首页热门课程继续保持 `ES 优先 + MySQL 兜底`
2. ES 不再保存临时封面 URL，只保存原始 `coverKey`
3. 读取 ES 结果后，在返回前统一签名封面 URL
4. ES 同步从“单索引先删后写”改成“双索引轮换 + alias 切换”
5. 每次同步生成唯一 `syncBatchId`
6. 同步全链路输出结构化日志，便于排查

## 4. 最终架构

### 4.1 索引结构

固定 3 个名称：

- 读 alias：`homepage_hot_course_read`
- 物理索引 A：`homepage_hot_course_a`
- 物理索引 B：`homepage_hot_course_b`

说明：

- 首页查询永远只查 alias
- 同步时只在 `a/b` 两个物理索引之间切换
- 不新增带时间戳的新索引名
- 不会出现索引数量无限增长问题

### 4.2 同步批次标识

每轮同步生成一个批次号：

- `syncBatchId`

建议格式：

- `yyyyMMddHHmmss`

示例：

- `20260629143000`

该值用于：

- 同步日志
- ES 文档元数据
- alias 切换日志
- 问题排查

### 4.3 读写流程

读流程：

1. 接口请求首页热门课程
2. 查询 alias：`homepage_hot_course_read`
3. 反序列化 ES 文档
4. 将 `coverKey` 转换成临时签名 URL
5. 返回前端
6. 若 ES 失败，则降级 MySQL

写流程：

1. 定时任务触发同步
2. 查询当前 alias 指向哪个索引
3. 选择另一个索引作为目标写入索引
4. 清空目标写入索引
5. 写入最新 Top 5
6. 写后校验
7. alias 原子切换到目标索引
8. 旧索引保留，留待下轮复用

## 5. 当前代码涉及位置

当前首页热门课程相关代码主要在：

- `backstage-system/src/main/java/com/backstage/system/controller/homepage/OshHomePageCourseController.java`
- `backstage-system/src/main/java/com/backstage/system/service/homepage/IOshHomePageCourseService.java`
- `backstage-system/src/main/java/com/backstage/system/service/impl/homepage/OshHomePageCourseServiceImpl.java`
- `backstage-system/src/main/java/com/backstage/system/service/homepage/es/AbstractOshHomePageEsCardService.java`
- `backstage-system/src/main/java/com/backstage/system/mapper/homepage/OshHomePageCourseMapper.java`
- `backstage-system/src/main/resources/mapper/homepage/OshHomePageCourseMapper.xml`
- `backstage-system/src/main/java/com/backstage/system/mapper/homepage/OshHomePageHotCourseEsMapper.java`
- `backstage-system/src/main/java/com/backstage/system/domain/homepage/es/OshHomePageHotCourseEsDocument.java`
- `backstage-system/src/main/resources/es/homepage_hot_course_index.json`
- `backstage-quartz/src/main/java/com/backstage/quartz/task/OshHomePageCardEsSyncTask.java`

## 6. 文档结构调整

### 6.1 首页热门课程 ES 文档字段调整

当前文档类：

- `OshHomePageHotCourseEsDocument`

建议字段调整如下：

#### 保留字段

- `id`
- `title`
- `price`
- `tPrice`
- `buyCount`
- `viewCount`
- `videoCount`
- `ratingScore`
- `serviceContent`
- `resourceType`
- `hotScore`
- `tags`
- `detailUrl`
- `listUrl`
- `sortNo`
- `sourceLimit`
- `syncTime`

#### 新增字段

- `syncBatchId`
- `coverKey`

#### 删除或停用字段

- `cover`

说明：

- `cover` 不再存储临时 URL
- ES 中只保存原始 `coverKey`
- 读取 ES 后再转成最终 `cover`

### 6.2 mapping 调整

建议更新 `homepage_hot_course_index.json`：

- 删除 `cover`
- 新增 `coverKey`
- 新增 `syncBatchId`

建议 mapping：

- `coverKey`: `keyword`
- `syncBatchId`: `keyword`

## 7. Service 设计调整

### 7.1 MySQL 查询分层

当前 `getHotCoursesFromMysql()` 同时做了：

- 查 MySQL
- 补标签
- 补 URL
- 补封面签名

建议拆成两层：

#### 层 1：原始首页课程卡片组装

新增内部方法，例如：

- `getHotCoursesRawFromMysql(int limit)`

职责：

- 查询热门课程
- 补标签
- 补 `detailUrl`
- 补 `listUrl`
- 不做封面签名

返回结果中的 `cover` 仍然是原始 OSS key。

#### 层 2：返回前视图处理

新增内部方法，例如：

- `signCoverUrls(List<HotCourseVO> courses)`

职责：

- 对 `cover` 做 `ossService.getLimitedUrl(...)`

这样可保证：

- 同步写 ES 用的是原始 key
- 接口返回前才签名

### 7.2 对外接口建议

建议保留现有对外方法：

- `List<HotCourseVO> getHotCourses(int limit)`
- `List<HotCourseVO> getHotCoursesFromMysql(int effectiveLimit)`
- `int syncHotCoursesToEs()`

但内部行为改成：

#### `getHotCourses(int limit)`

1. 调用模板基类走 `ES 优先 + MySQL 兜底`
2. 无论结果来自 ES 还是 MySQL，最终都统一做一次 `signCoverUrls`
3. 返回前端

#### `getHotCoursesFromMysql(int effectiveLimit)`

返回原始 key 版课程卡片，不签名。

如果担心方法名被误用，可补一个注释明确说明：

- 返回的是“用于首页读模型和同步的原始数据”

## 8. Abstract 模板层改造建议

当前抽象模板类：

- `AbstractOshHomePageEsCardService`

建议保留大框架，但将同步实现改为“目标索引写入 + alias 切换”。

### 8.1 新增抽象能力

建议新增这些抽象方法：

- `String readAliasName()`
- `String indexAName()`
- `String indexBName()`
- `void prepareTargetIndex(String indexName) throws IOException`
- `int bulkUpsert(String indexName, List<D> docs) throws IOException`
- `List<D> queryFromEs(String limitAliasOrIndex, int limit) throws IOException`
- `void switchAlias(String aliasName, String fromIndex, String toIndex) throws IOException`
- `String resolveCurrentAliasIndex(String aliasName) throws IOException`
- `boolean validateTargetIndex(String indexName, int expectedCount, String syncBatchId) throws IOException`

如果不想改动过多抽象层，也可以把这些能力仅放到 `OshHomePageHotCourseEsMapper` 里，由 `OshHomePageCourseServiceImpl` 自己 orchestration。

就首页课程卡片这个单点场景而言，推荐先在课程模块里落地，不强行抽到所有首页卡片共用模板中。

原因：

- 课程卡片是第一批需要 alias 轮换的
- 先跑通一条链路风险更低
- 后续再抽象成公共基类更稳

## 9. Mapper 设计调整

### 9.1 `OshHomePageHotCourseEsMapper` 新增能力

建议新增以下方法：

- `boolean indexExists(String indexName)`
- `void createIndexIfAbsent(String indexName, String mappingJson)`
- `void recreateIndex(String indexName, String mappingJson)`
- `int deleteAll(String indexName)`
- `int bulkUpsert(String indexName, List<OshHomePageHotCourseEsDocument> documents)`
- `List<OshHomePageHotCourseEsDocument> searchTopCourses(String indexOrAliasName, int limit)`
- `String getAliasTargetIndex(String aliasName)`
- `void switchAlias(String aliasName, String oldIndex, String newIndex)`
- `long count(String indexName)`
- `List<OshHomePageHotCourseEsDocument> searchAllForValidate(String indexName)`

### 9.2 alias 查询规则

#### 获取当前 alias 指向

逻辑：

1. 先查 alias 是否存在
2. 若 alias 不存在，返回空
3. 若 alias 指向 `a`，则当前在线索引是 `a`
4. 若 alias 指向 `b`，则当前在线索引是 `b`

### 9.3 目标索引选择规则

规则如下：

- alias 指向 `a`，本轮写 `b`
- alias 指向 `b`，本轮写 `a`
- alias 不存在，本轮默认写 `a`

### 9.4 alias 切换规则

切换时使用 ES alias 原子操作：

- 若旧 alias 存在，则先 remove old index alias
- 再 add new index alias
- remove/add 放在同一个 alias actions 请求里

这样可以保证不会出现中间态。

## 10. 同步流程详细说明

### 10.1 同步主流程

建议 `syncHotCoursesToEs()` 内部执行如下步骤：

1. 生成 `syncBatchId`
2. 读取当前 alias 目标索引
3. 计算目标写入索引
4. 查询 MySQL Top 5 原始卡片数据
5. 转换成 ES 文档，写入 `syncBatchId`、`syncTime`
6. 确保目标索引存在
7. 清空目标索引
8. 批量写入目标索引
9. 校验目标索引数据
10. alias 原子切换
11. 输出成功日志

### 10.2 同步伪代码

```java
String batchId = nowFormat("yyyyMMddHHmmss");
LocalDateTime syncTime = LocalDateTime.now();

String currentIndex = mapper.getAliasTargetIndex(READ_ALIAS);
String targetIndex = chooseTargetIndex(currentIndex);

List<HotCourseVO> vos = getHotCoursesRawFromMysql(5);
List<OshHomePageHotCourseEsDocument> docs = toDocs(vos, syncTime, batchId);

mapper.createIndexIfAbsent(targetIndex, mappingJson);
mapper.deleteAll(targetIndex);

int written = mapper.bulkUpsert(targetIndex, docs);

boolean valid = validateTargetIndex(targetIndex, 5, batchId);
if (!valid) {
    throw new IllegalStateException("validate target index failed");
}

mapper.switchAlias(READ_ALIAS, currentIndex, targetIndex);
```

## 11. 校验规则

alias 切换前必须校验目标索引。

建议至少校验：

1. 写入条数是否等于 MySQL 查询结果条数
2. 若首页要求固定 5 条，则通常期望为 5
3. 从目标索引查询出的文档数是否正确
4. 每条文档的 `syncBatchId` 是否等于本轮 batchId
5. `sortNo` 是否连续且正确
6. 文档主键 `id` 是否非空

建议提供一个私有校验方法，例如：

- `validateTargetIndex(String targetIndex, List<OshHomePageHotCourseEsDocument> expectedDocs, String batchId)`

## 12. 日志设计

### 12.1 日志原则

每轮同步至少要能回答下面几个问题：

- 这一轮同步有没有开始
- 当前线上读的是 `a` 还是 `b`
- 这轮准备写哪个索引
- MySQL 取了哪些课程
- ES 写入是否成功
- alias 是否切换成功
- 当前线上最终读的是哪个索引

### 12.2 推荐日志字段

建议所有同步日志统一带这些字段：

- `cardType=course`
- `syncBatchId`
- `currentIndex`
- `targetIndex`
- `aliasName`
- `mysqlCount`
- `writtenCount`
- `validatedCount`
- `courseIds`
- `costMs`

### 12.3 必打日志点

#### 同步开始

```text
[homepage-course-sync] start, batchId=20260629143000, alias=homepage_hot_course_read, currentIndex=homepage_hot_course_a, targetIndex=homepage_hot_course_b
```

#### MySQL 查询完成

```text
[homepage-course-sync] mysql loaded, batchId=20260629143000, count=5, courseIds=[101,102,103,104,105]
```

#### 目标索引清空完成

```text
[homepage-course-sync] target index cleared, batchId=20260629143000, targetIndex=homepage_hot_course_b
```

#### ES 写入完成

```text
[homepage-course-sync] bulk upsert success, batchId=20260629143000, targetIndex=homepage_hot_course_b, written=5
```

#### 校验成功

```text
[homepage-course-sync] validate success, batchId=20260629143000, targetIndex=homepage_hot_course_b, validatedCount=5
```

#### alias 切换成功

```text
[homepage-course-sync] alias switched, batchId=20260629143000, alias=homepage_hot_course_read, from=homepage_hot_course_a, to=homepage_hot_course_b
```

#### 同步结束

```text
[homepage-course-sync] finished, batchId=20260629143000, onlineIndex=homepage_hot_course_b, costMs=287
```

#### 异常日志

```text
[homepage-course-sync] failed, batchId=20260629143000, currentIndex=homepage_hot_course_a, targetIndex=homepage_hot_course_b, error=...
```

### 12.4 查询路径日志

首页读接口建议保留现有 fallback 日志，但增加更多上下文：

- 是否命中 ES
- 命中 alias 名称
- 返回数量
- batchId
- syncTime

## 13. 读路径实现建议

### 13.1 ES 读取流程

读取 alias 后：

1. 拿到 ES 文档
2. 将 `coverKey` 映射回 `HotCourseVO.cover`
3. 调用 `ossService.getLimitedUrl(coverKey, 1440)` 生成临时 URL
4. 返回前端

### 13.2 MySQL 兜底流程

兜底 MySQL 时：

1. 查原始 key
2. 返回前统一签名

注意：

- 不要在 ES 同步前签名
- 不要把签名 URL 写回 ES

## 14. 定时任务接入建议

当前项目已有统一首页卡片同步任务入口：

- `backstage-quartz/src/main/java/com/backstage/quartz/task/OshHomePageCardEsSyncTask.java`

建议首页热门课程继续挂在统一任务编排下，不单独再起一个新任务。

如果统一任务支持按 `cardType` 执行，则课程模块只需要实现好：

- `cardType() = "course"`
- `syncToEs()`

## 15. 启动初始化策略

当前已有：

- `OshHomePageEsIndexInitializer`

建议保留初始化行为，但调整为：

1. 检查 `a/b` 索引是否存在，不存在则创建
2. 检查 alias 是否存在
3. 若 alias 不存在：
   - 默认先同步到 `a`
   - 切 alias 到 `a`

这样服务首次启动时也能自动建立首页课程读模型。

## 16. 排查指南

### 16.1 典型排查问题

#### 问题 1：首页课程卡片没走 ES

检查：

- `searchEsProperties.isEnabled()` 是否为 `true`
- alias `homepage_hot_course_read` 是否存在
- alias 是否指向 `a` 或 `b`
- `getCards()` 是否打印了 fallback 日志

#### 问题 2：ES 有数据，但封面失效

检查：

- ES 文档里存的是不是 `coverKey`
- 返回前是否重新签名
- 是否仍误把临时 URL 写进 ES

#### 问题 3：同步日志显示成功，但首页还是旧数据

检查：

- alias 最终指向的是哪一个索引
- 当前在线索引里文档的 `syncBatchId`
- 前端读的是 alias 还是物理索引

#### 问题 4：a 索引有数据但线上没生效

检查：

- alias 当前是否仍指向 `b`
- 本轮 alias 切换是否真正成功
- `switchAlias` 是否抛异常

### 16.2 建议增加一个调试查询能力

建议后续新增一个只读调试方法或内部接口，返回：

- 当前 alias 名称
- 当前 alias 指向索引
- `a` 索引文档数
- `b` 索引文档数
- 当前在线索引最新 `syncBatchId`
- 当前在线索引最新 `syncTime`
- 当前在线课程 ID 列表

这样排查效率会很高。

## 17. 建议修改清单

### 17.1 必改文件

- `backstage-system/src/main/java/com/backstage/system/domain/homepage/es/OshHomePageHotCourseEsDocument.java`
- `backstage-system/src/main/java/com/backstage/system/mapper/homepage/OshHomePageHotCourseEsMapper.java`
- `backstage-system/src/main/java/com/backstage/system/service/impl/homepage/OshHomePageCourseServiceImpl.java`
- `backstage-system/src/main/resources/es/homepage_hot_course_index.json`

### 17.2 可选改造文件

- `backstage-system/src/main/java/com/backstage/system/service/homepage/es/AbstractOshHomePageEsCardService.java`
- `backstage-system/src/main/java/com/backstage/system/service/homepage/IOshHomePageCourseService.java`
- `backstage-system/src/main/java/com/backstage/system/service/homepage/es/OshHomePageEsIndexInitializer.java`

## 18. 实施顺序建议

建议按以下顺序开发：

1. 修改 ES 文档结构，替换 `cover -> coverKey`，新增 `syncBatchId`
2. 修改 MySQL 查询后处理，拆出“原始 key 查询”和“返回前签名”
3. 修改 ES 读取逻辑，返回前统一签名
4. 扩展 ES mapper，支持 `a/b + alias`
5. 将首页课程同步改成目标索引写入 + alias 切换
6. 增加同步校验与日志
7. 本地/测试环境验证首次初始化、连续两轮同步、失败回滚

## 19. 验证清单

上线前至少验证下面场景：

1. 首次启动，alias 不存在时，是否能自动初始化成功
2. 同步一次后，alias 是否指向 `a` 或 `b`
3. 第二次同步后，alias 是否切到另一个索引
4. ES 文档是否存的是 `coverKey` 而不是签名 URL
5. 首页接口返回的 `cover` 是否是可访问的临时 URL
6. 模拟 bulk 写入失败时，alias 是否仍指向旧索引
7. ES 关闭或不可达时，首页是否正常回落 MySQL

## 20. 最终结论

首页热门课程这条链路，建议采用如下最终实现：

- 读 alias：`homepage_hot_course_read`
- 写索引：`homepage_hot_course_a / homepage_hot_course_b`
- 双索引轮换
- alias 原子切换
- 固定 5 条
- ES 只存 `coverKey`
- 返回前再签名
- 每轮生成 `syncBatchId`
- 全链路日志可追踪

这套方案可以同时解决：

- 封面 URL 过期
- 先删后写导致 ES 空仓
- 同步后难排查 `a/b` 状态

并且非常适合首页卡片这种“小数据量、高稳定性要求”的场景。
