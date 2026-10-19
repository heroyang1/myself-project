# Proposal: split-tool-service

## 背景

`plan-microservice-split-roadmap` 总纲确立绞杀者渐进式拆分，Phase 1 选定 tool-service 作为**首个试点**：它相对独立、无秒杀级高并发、失败影响面可控，最适合用来验证"网关路由分流 + Feign 契约 + Outbox 事件 + 灰度回滚"这套拆分机制是否跑得通。

现状：工具域代码位于 `backstage-system`，通过三个 Controller 对外提供能力：

- `OshToolController`（`/pc/tool/**`，453 行）：工具搜索/详情/浏览/推荐/标签、后台增删改、收藏、点赞/差评、点数扣减、计算器、工具提问。
- `ToolPurchaseController`（`/pc/tool/purchase/**`，116 行）：点数套餐查询、下单、购买记录、关单、套餐后台增删改。
- `ToolAnnouncementController`（`/pc/tool/announcement/**`，51 行）：工具模块公告/动态/通知查询。

工具域自有 7 张表（`osh_tool`、`osh_tool_package`、`osh_tool_package_purchase_record`、`osh_tool_collection`、`osh_tool_vote`、`osh_tool_tag`、`osh_tool_user_quota`），并通过 Outbox 向 Kafka topic `osh.tool.index` 发布 ES 索引事件（TOOL_INDEX_CREATE/UPDATE/DELETE/COUNTER）。

**关键难点**：工具域存在多条跨域依赖，正是拆分要解耦的对象——积分扣减/退还直写 `osh_user_asset`（属 auth-user-service）、下单调用 `OrderCheckoutService`（属 trade-service）、工具提问调用 `IOshQAQuestionService`（属 course-service）、背景图上传调用 `OssService`（属 platform-service）、公告读写 `osh_announcement`（属 content-service）、贡献记录 `ContributionService` 与行为埋点 `@OshUserEvent`（属 auth-user-service）。

## 目标

- 新建 `tool-service`（Spring Boot 3.2 + JDK 17，复用 Phase 0 的 gateway/observability 基建），承接工具域自有能力。
- 采用绞杀者模式：**保留 `backstage-system` 中的工具代码不删**，通过网关按 `/pc/tool/**` 路径灰度分流到新服务，双跑期可随时路由级回滚。
- 工具域自有 7 张表写权限迁移到 tool-service（Phase 1 共库不共写：仍连同一 MySQL 实例，仅切换写归属）。
- 将上述跨域"直写/直调"改造为符合"一表一主"的显式契约调用：Phase 1 阶段 auth-user/trade/course/content/platform 尚未拆出，tool-service 通过 Feign 回调**单体内暴露的内部 API**；待这些域各自拆分后，Feign 目标平滑改指向对应新服务（契约不变）。
- 点数支付/下单等跨服务流程用 Outbox 可靠消息 + SAGA 补偿替代原单一本地事务。

## 非目标

- 本次**不迁移** website（实用网站）、site（站点，含 SSH RemoteShellExecutor）、feedback、friend-link：它们虽在总纲 tool-service 远期边界内，但实际代码分散在 `website`/`site`/`assistant`/`homepage`/`ad` 包，需先做边界归位；为保持试点小而稳，作为后续增量（见 design「增量范围」）。
- 本次不删除 `backstage-system` 中任何工具代码（绞杀双跑期保留）。
- 本次不改变任何对外 URL、参数、响应结构（契约冻结）。
- 本次不做工具域物理分库（Phase 4 才做），仅切写权限归属。
- 本次不改动 `osh.tool.index` topic 名与事件语义（Flink 作业依赖，保持稳定）。

## 用户影响

- 对终端用户：无感知。工具搜索/详情/购买/收藏/点赞等接口 URL 与响应不变。
- 对前端：零改造，分流由网关按用户 ID 尾号灰度控制。
- 对运维：新增 tool-service 部署单元；需在网关配置 `/pc/tool/**` 路由与灰度规则；需监控新服务的 Feign 回调链路。
- 对开发者：工具域后续迭代改在 tool-service；`backstage-system` 工具代码进入冻结维护期（双跑结束后再删）。
- 对权限：`@OshUserLevel`、`@OshUserEvent`、`@Anonymous`、`@PreAuthorize` 语义必须与拆分前一致，用户上下文由网关透传。

## 风险

- 兼容性风险：tool-service 与单体双跑期共用同一 MySQL 与同一 Redis（LoginUser、工具点数缓存），缓存 key 与锁 key 必须完全一致，否则出现读写不一致或锁失效双花。缓解：四类契约冻结（URL/权限字符串/锁 key/LoginUser key），锁 key 常量抽到共享模块。
- 数据风险：积分扣减/退还、订单创建、点数发放跨服务后不再是单一本地事务，存在中间态。缓解：SAGA 补偿（下单失败退还积分，复用现有 `cancelPendingPurchase` 补偿语义）+ 消费端 CAS 幂等 + `osh_tool_package_purchase_record` 的 grant_status 作为发放闸门。
- 性能风险：原本进程内的积分/订单/OSS 调用变为跨网络 Feign 调用，购买链路 RT 上升。缓解：读多写少的工具内容链路优先切流；购买链路灰度比例最后提升并压测。
- 安全风险：`@OshUserLevel(5)` 后台管理端点（save/update/delete/package.save/package.delete）分流后必须仍然鉴权，防止越权改工具/套餐。缓解：网关粗校验 + 服务内 `@PreAuthorize`/`@OshUserLevel` 细校验双层兜底。
- 回滚风险：低。网关将 `/pc/tool/**` 指回单体即秒级回滚；因共库且无数据结构变更，回滚无损。

## 验收标准

- [ ] tool-service 独立启动、注册 Nacos，`/actuator/prometheus` 与追踪接入（复用 add-observability-stack）。
- [ ] 网关 `/pc/tool/**` 灰度分流可用，比例 0 时全量走单体、行为与拆分前一致。
- [ ] API 契约冻结清单（design.md）中全部端点：经 tool-service 与经单体返回逐字段一致（新老双路径 smoke 对比）。
- [ ] 工具域自有 7 张表写操作全部落在 tool-service，单体侧对应用户流量不再写这些表（灰度命中时）。
- [ ] 跨域依赖改造完成：积分/订单/OSS/提问/公告/贡献经 Feign 契约调用，tool-service 不再直连其他域的表。
- [ ] 点数购买 SAGA：积分支付下单成功后扣积分、失败退还；现金支付回调后发放点数配额（`osh_tool_user_quota`）并置 grant_status=success，重复回调幂等。
- [ ] Outbox 事件 `osh.tool.index` 由 tool-service 正常发布，Flink 索引同步不受影响。
- [ ] 后台管理端点在分流后鉴权不回归（越权测试通过）。
- [ ] 路由级回滚演练通过：灰度中将 `/pc/tool/**` 指回单体，流量秒级回退且无数据不一致。
- [ ] Spec & Process Guard CI 通过，本 change 四件套齐备，无 OpenSpec 豁免。

## 关联信息

- 禅道任务：待创建
- PR：待提交
- 设计稿：design.md（本 change 内）
- 前置 change：plan-microservice-split-roadmap（总纲/边界宪章）、upgrade-jdk17-springboot27、add-gateway-service、add-observability-stack
- 关键源码：`backstage-system/.../controller/tool/*`、`service/impl/tool/ToolPurchaseServiceImpl.java`、`service/tool/*`、`mapper/tool/*`、`domain/tool/*`
- 参考规范：`openspec/project.md`（架构边界/升级原则）、`docs/ai/02-workflow.md`（SDD 流程）、`docs/design/`（分布式锁/Outbox/积分抵扣）
