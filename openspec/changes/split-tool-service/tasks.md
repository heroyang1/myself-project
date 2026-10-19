# Tasks: split-tool-service

## 准备

- [ ] 阅读 `AGENTS.md`、`openspec/project.md`、`docs/ai/02-workflow.md`
- [ ] 阅读 `plan-microservice-split-roadmap/{proposal,design}.md`（边界宪章 + 四项决策）
- [ ] 确认 Phase 0 前置已就绪：upgrade-jdk17-springboot27、add-gateway-service、add-observability-stack
- [ ] 阅读工具域源码：`controller/tool/*`、`service/impl/tool/ToolPurchaseServiceImpl`、`service/tool/*`、`mapper/tool/*`、`domain/tool/*`、`service/order/handler/ToolPaidHandler`
- [ ] 阅读相关 `docs/design/*`（分布式锁、Outbox、积分抵扣、工具模块 Flink 设计）

## 阶段一：依赖分析（先摸清，不动代码）

- [ ] 盘点工具域自有 7 表（osh_tool / osh_tool_package / osh_tool_package_purchase_record / osh_tool_collection / osh_tool_vote / osh_tool_tag / osh_tool_user_quota）读写点
- [ ] 盘点全部跨域依赖：积分（osh_user_asset*）、订单（OrderCheckoutService/OrderService）、提问（IOshQAQuestionService）、OSS（OssService）、公告（osh_announcement）、贡献（ContributionService）、行为埋点（@OshUserEvent）
- [ ] 确认支付成功发放链路：PayNotifyController→Kafka→PaymentSuccessConsumer→OrderPaidHandlerRegistry→ToolPaidHandler 的真实调用点与 grantStatus 幂等实现
- [ ] 确认 ES 索引链路：Outbox(TOOL 聚合)→topic `osh.tool.index`→Flink；核对 ToolIndexMessage/DeleteMessage/EventType 字段
- [ ] 输出《工具域依赖与契约清单》到本 change 目录（作为 Feign 契约与事件定义输入）

## 阶段二：接口契约定义

> 进度说明（2026-09-30 增量）：已在仓库新增共享契约模块 `backstage-service-contract`（纯 POJO，Java 8，零框架依赖），
> 落地 asset/trade/oss/qa 四域入向契约 DTO + `InternalResult` + `InternalApiConstants`。tool-api 对外能力与事件信封尚未定义。

- [ ] 定义 `tool-api`（对外能力，若被其他服务调用）与 tool-service 消费的入向契约
- [x] 定义/复用 `auth-user-api`：deductPoints / refundPoints / getPoints（requestId 幂等），LoginUser POINTS 缓存刷新责任归属 auth-user
      —— 契约 DTO（PointsChangeRequest/Response、PointsBalanceResponse）已定义；单体现金实现见 `UserAssetService`（updatePointsAtomic 原子范式 + 提交后刷 POINTS 缓存）；requestId 幂等待 tool-service 侧接入
- [x] 定义/复用 `trade-api`：checkout(OrderCheckoutReqVO) / cancelByOrderNo(orderNo)
      —— 契约 DTO（OrderCheckoutRequest/Response、OrderCancelRequest）已定义，单体端点委托现有 OrderCheckoutService/OrderService
- [x] 定义/复用 `course-api`：addToolQuestion(userId, toolId, content, tags)
      —— 契约 DTO（ToolQuestionRequest/Response）已定义，单体端点委托 IOshQAQuestionService
- [~] 定义/复用 `platform-api`：oss upload / getLimitedUrl（DB 只存 objectKey）
      —— 部分完成：getLimitedUrl 签名端点（OssSignRequest/Response）已定义并落地；upload 端点尚未实现
- [ ] 定义入向事件 `osh.trade.order.paid.v1`(productType=TOOL) 信封与 tool-service 消费者契约
- [ ] 定义出向事件：`osh.tool.resource.contribution.v1`、`osh.user.behavior.v1`、`osh.content.announcement.v1`（经 Outbox）
- [ ] 冻结 API 契约清单（design.md 表）中所有端点的 URL/入参 DTO/返回 VO 字段

## 阶段三：单体侧最小改造（解开一表一主）

> 进度说明（2026-09-30 增量）：已完成 `/internal/**` 契约端点 + fail-closed 内网鉴权；未触碰 ToolPaidHandler 发放链路
> （无消费者前改事件会中断配额发放，须在 tool-service 就绪后另行改造）。
> 已补测试（均 JUnit 5，遵循“新测试一律 JUnit 5”决策）：资金单元测试 `UserAssetServiceImplTest`（backstage-system，9 用例）、
> 认证链集成测试 `InternalApiAuthIntegrationTest`（backstage-admin，@SpringBootTest + addFilters=true，3 用例）。

- [x] 单体暴露 `/internal/**` 契约端点：asset 扣/退积分、order checkout/cancel、oss sign、qa addToolQuestion（仅供 tool-service Feign 调用，内网鉴权）
      —— 4 个内部控制器（InternalAssetController/InternalTradeController/InternalOssController/InternalQaController）纯委托现有服务；
         内网鉴权由 `InternalApiAuthFilter`（X-Internal-Token 共享密钥，未配置即 403 fail-closed）+ OshAuthenticationFilter 白名单 + SecurityConfig hasAuthority 完成；
         oss upload 端点未纳入本次增量
- [ ] 改造 `ToolPaidHandler.handle`：由"直接写工具表"改为"发布 `osh.trade.order.paid.v1` 事件"（发放逻辑迁出）
- [ ] 提供发放预案开关：异常时可临时恢复单体本地发放（回滚用）
- [ ] 锁 key 常量抽到共享模块 `osh-common-core`，单体与 tool-service 共用（字面量冻结）
- [ ] 上述改造各自配套/更新对应 OpenSpec（若触发接口/权限/支付变更）

## 阶段四：tool-service 实现

- [ ] 新建 tool-service 模块（SB3.2 + JDK17），接入 Nacos、可观测性、Sentinel、Feign 超时/重试/fallback
- [ ] 迁移工具域 controller/service/mapper/domain（核心 tool 子域），保持包内域边界
- [ ] 网关透传头还原 UserContextUtil；`@OshUserLevel`/`@Anonymous`/`@PreAuthorize`/`@DistributeLock`/`@OshUserEvent` 语义对齐
- [ ] 跨域直连 Mapper 全部替换为 Feign 契约调用（移除对 asset/order/oss/qa/announcement/contribution 的 Mapper 注入）
- [ ] 实现点数购买 SAGA：purchase_record 本地事务 + deductPoints + checkout + 失败补偿 refundPoints
- [ ] 实现 `osh.trade.order.paid.v1` 消费者：grantStatus CAS 幂等 + increaseUserQuota/insertUserQuota 发放
- [ ] 迁移 ES 索引发布：Outbox(TOOL) 保持 topic `osh.tool.index` 与事件语义
- [ ] 缺陷修复随迁移落地：calculateTool 补 @Transactional；increaseTotalUsage+saveToolIndexEvent 原子化；TOCTOU consume 端 CAS 兜底
- [ ] 日志/脱敏/traceId 接入；工具点数发放对账埋点

## 阶段五：数据与配置

- [ ] Phase 1 共库不共写：tool-service 数据源指向同一 MySQL，仅切写归属；不迁数据
- [ ] Nacos 划分 tool-service namespace/group 与配置项；敏感配置走 osh-secret
- [ ] 网关配置 `/pc/tool/**` 路由 + 灰度规则（初始比例 0）
- [ ] 校验 osh_tool_user_quota 单一写者（发放仅经 tool-service 消费者）

## 阶段六：测试与验证

- [ ] 单元测试：SAGA 分支（扣减成功/失败/补偿）、发放幂等、consumeUserQuota CAS、calculateTool 事务
- [ ] 契约测试：auth-user-api / trade-api / course-api / platform-api stub 校验
- [ ] 集成测试：tool-service + Feign 回调单体 + 事件消费发放全链路
- [ ] 新老双路径 smoke：契约冻结清单全部端点响应逐字段一致
- [ ] 越权测试：@OshUserLevel(5) 端点分流后鉴权不回归
- [ ] 发放对账：购买记录 grant_status 与 osh_tool_user_quota 增量一致；重复回调幂等
- [ ] 压测：购买链路 Feign 化后 RT/P99 增量在阈值内
- [ ] 回滚演练：灰度中 `/pc/tool/**` 指回单体，流量秒级回退且数据一致

## 阶段七：灰度发布

- [ ] 只读内容链路灰度 1%→10%→50%→100%，每档观察错误率/P99/追踪
- [ ] 购买/后台写链路最后提升灰度并压测
- [ ] 全量后进入双跑观察期，单体工具代码冻结维护
- [ ] 观察期满评估是否删除单体工具代码（另立 change）

## PR

- [ ] PR 描述关联本 OpenSpec change，无 OpenSpec-Exempt
- [ ] 附新老双路径 smoke 对比、发放对账、压测数据、回滚演练结果
- [ ] 填写风险与回滚（路由级秒级 + 发放预案开关）
- [ ] 确认 Spec & Process Guard CI 通过
- [ ] 指派 Reviewer，按 `CODE_REVIEW.md` 审查
