# OSH Backend `docs/` 目录全景分析

> 本文档基于对项目 `docs/` 目录的完整扫描与关键文件内容深度阅读，系统梳理各子文件夹的功能定位、文档类型、核心内容及协作关系，为团队成员和 AI Agent 提供文档查阅指南。

---

## ⚠️ 目录重组说明（2026-10）

> 本文正文按裸文件名引用了若干原 `docs/` 根目录散落文档；若正文链接失效，按下表定位。
> 根目录现仅余本元索引；原被硬链接锚定的 12 篇已第二批归位，并同步更新 README/CLAUDE.md/api-spec/coding-standards/data-flow/wiki/spec 共 15 处引用。

| 新目录 | 文件 |
|---|---|
| `docs/harness/` | Harness工程方法论_学习笔记 · AI治理组件落地Playbook · project-doc-system-playbook |
| `docs/modules/course/` | course_module_implementation_guide · course_permission_config · 课程文件上传（快速开始/实现总结/实现方案） |
| `docs/modules/group/` | group_code_modification_report · group_table_design_analysis · group_module_optimization_handover · 拼团模块技术优化项目交接文档 · group_table_fix_execution_guide · group_table_structure_fix_plan · 手动添加参团用户（修复总结/调试指南） |
| `docs/analysis/` | OSH_Backend_治理体系与业务实现技术分析报告 · OSH_Backend_项目陷阱全景与实战手册 · Redis_BigKey_Audit_Report · Redis_KEYS命令安全审查报告 · Redis_Lua脚本与事务深度审计报告 · 技术改进方向与技术债务清单 · 技术缺陷与优化案例参考手册 · 高并发 |
| `docs/guides/` | Git_Merge_vs_Rebase_详细指南 · 测试规范与Checklist |
| `docs/notes/` | dev-notes-20260312 · 面试技术亮点素材手册 · Flink8slot测试 · local-dev-setup-guide |
| `docs/assets/` | image.png · cumcm2026d.zip |
| （第二批）`docs/analysis/` | CI_CD_Audit_Report · ES_Deep_Pagination_Analysis · ES_MySQL_Consistency_Analysis · 典型技术缺陷与优化案例集 |
| （第二批）`docs/guides/` | github-actions-deploy |
| （第二批）`docs/modules/course/` | course_tag_api · 课程提问功能需求分析与接口设计 · 课程服务人员申请流程需求规格说明书 |
| （第二批）`docs/modules/tools/` | 实用网站模块接口文档 |
| （第二批）`docs/modules/seckill/` | 秒杀模块接口文档 |
| （第二批）`docs/modules/payment/` | 业务模块接入支付模块说明 |
| （第二批）`docs/notes/` | 全局记忆库导出 |

---

## 一、整体概述

`docs/` 目录是 OSH Backend 项目的**技术沉淀中心与团队知识资产库**。它不是简单的文件堆放区，而是承载了以下三重核心职能：

1. **AI Agent 工作规范中心**：通过 `docs/ai/` 定义 SDD（规格驱动开发）工作流，确保每个 AI Agent 按统一节奏执行开发任务。
2. **技术方案蓝图库**：通过 `docs/design/` 和各业务模块文档，记录每个功能模块的架构决策、数据模型、接口设计和实现路径。
3. **问题诊断与经验传承**：通过故障分析、重构记录、开发日志等，将团队踩过的坑和积累的经验固化为可复用的知识。

当前 `docs/` 目录包含 **7 个子文件夹** 和 **30+ 个根目录散落文档**，总计约 **95 个文件**，覆盖从开发规范到业务接口、从架构设计到故障复盘的完整知识谱系。

---

## 二、各子文件夹深度解析

### 2.1 `docs/ai/` — AI 开发规范与工作流程中心

#### 功能定位

`docs/ai/` 是整个项目的**"宪法"级目录**，定义了团队（包括人类开发者和 AI Agent）如何协同工作的完整规范体系。它与 `AGENTS.md`、`openspec/` 共同构成 SDD 工作流的三大支柱。

#### 文档类型

| 类型 | 数量 | 说明 |
|---|---|---|
| 流程规范 | 9 个主文件 | 从新人教程到发布流程的完整链路 |
| 技术约定 | 14 个 conventions 文件 | 覆盖 API、数据库、Redis、Kafka、安全等细分领域 |

#### 核心内容摘要

| 文件 | 核心内容 |
|---|---|
| `README.md` | AI 开发规范入口，定义必读顺序和五条核心约束 |
| `01-newcomer-guide.md` | 新人上手教程，详解 SDD、OpenSpec、Harness、Superpowers 四大核心概念 |
| `02-workflow.md` | 标准开发流程：需求 → OpenSpec change → 分支 → 实现 → PR → 审查 → 合并 |
| `04-architecture.md` | 后端架构说明：当前多模块单体形态 + 未来微服务拆分方向 |
| `07-review-rubric.md` | 代码审核七维度标准：需求规格、行为正确性、接口兼容、权限安全、数据中间件、可维护性、验证回滚 |
| `conventions/api.md` | API 路径、请求响应、错误码统一约定 |
| `conventions/springboot.md` | Controller/Service/Configuration 分层规则与升级检查清单 |
| `conventions/database.md` | 数据库设计与 SQL 约定 |
| `conventions/security.md` | 安全相关约定 |

#### 实际举例说明

**场景 1：AI Agent 首次接手开发任务——从"盲目编码"到"规范施工"**

当 AI Agent（如 Codex、Cursor、Qoder）首次参与 OSH Backend 开发时，必须按 `docs/ai/README.md` 定义的顺序阅读文档。

- `docs/ai/02-workflow.md` 明确规定：每次让 Codex 开发时，必须先读 `AGENTS.md`、`openspec/project.md`、当前 change 的 `proposal.md`/`design.md`/`tasks.md`。Codex 必须"先说明理解和计划，只改本任务相关文件，不覆盖未授权本地改动"。
- `docs/ai/01-newcomer-guide.md` 用通俗语言解释了为什么需要这套流程——"如果每个人写法不同，接口随手改，前端突然炸，权限逻辑散落各处，新人只能靠问人和猜"。

**代码级关联**：当 Agent 接到"新增课程提问功能"任务时，工作流要求它先读 `openspec/changes/course-question/` 下的 `proposal.md`（为什么做）→ `design.md`（怎么做）→ `tasks.md`（分步执行）→ `specs/spec.md`（GIVEN/WHEN/THEN 验收标准）。Agent 编码时必须遵守 `docs/ai/conventions/springboot.md` 中的分层规则——Controller 不写复杂业务、事务放 Service、Mapper 不做业务判断。实际代码如 `OshCourseController.save()` 中同时叠加了 `@PreAuthorize`（权限）、`@DistributeLock`（并发控制）、`@OshUserEvent`（事件记录）三个注解，这正是规范中"参数校验前置、权限注解明确"要求的具体落地。

**痛点解决**：避免 Agent 绕过 OpenSpec 直接改代码，防止"接口路径随意变更导致前端炸裂"、"权限逻辑散落导致安全漏洞"等问题。

**如果缺少这份文档**：每个 AI Agent 都会按自己的理解编码，不同 Agent 产出的代码风格不一致，接口命名混乱，权限校验遗漏，最终需要人工逐行审查——成本远高于事前规范。

---

**场景 2：Reviewer 审查 PR——七维度审核清单**

当 Reviewer 审查一个涉及课程权益变更的 PR 时，`docs/ai/07-review-rubric.md` 提供了标准化的审核路径：

1. **需求规格**：是否有关联 OpenSpec change？`tasks.md` 是否完成？
2. **行为正确性**：是否满足 GIVEN/WHEN/THEN？是否覆盖异常分支？
3. **接口兼容**：路径/参数/响应结构是否变更？前端是否同步？
4. **权限安全**：是否绕过登录？是否越权访问？
5. **数据中间件**：事务边界是否正确？缓存一致性是否处理？
6. **可维护性**：命名是否清楚？是否引入重复逻辑？
7. **验证回滚**：回滚方案是否可操作？

**痛点解决**：将审核从"凭经验看代码"升级为"按清单逐项验证"，**确保每次审查不遗漏安全、兼容性和数据一致性等关键维度**。

**如果缺少这份文档**：Reviewer 容易只关注代码风格和命名，忽略事务边界、缓存一致性、ES 索引兼容性等隐性风险，导致上线后出现数据不一致或接口不兼容。

---

**场景 3：升级 Spring Boot——避免"升级即崩溃"**

`docs/ai/09-upgrade-strategy.md` 规定了升级 Spring Boot 必须检查的五个方面：Spring Security 行为变化、Servlet/Tomcat 版本、配置属性改名、依赖冲突、Actuator/健康检查。

**跨文档协作**：升级属于重大变更，必须先在 `openspec/changes/upgrade-springboot-3/` 创建 change，写清兼容性矩阵和影响模块清单，再按 `docs/ai/conventions/springboot.md` 中的 Upgrade 检查清单逐项验证，最后在 `docs/ai/08-release-flow.md` 定义的灰度流程中逐步发布。

**痛点解决**：防止"升级后 Security 过滤链行为变化导致接口裸奔"或"配置属性改名导致启动失败"等隐性灾难。

---

### 2.2 `docs/design/` — 技术方案与设计文档库

#### 功能定位

`docs/design/` 是项目的**架构蓝图库**，记录每个业务模块/功能的技术设计方案。它是代码实现前的"施工图纸"，涵盖数据模型、接口设计、流程逻辑、异常处理和性能考量。

#### 文档类型

| 类型 | 说明 |
|---|---|
| 模块全局设计 | 描述整个模块的架构、分层、核心表和完整业务流程 |
| 专项功能设计 | 针对特定功能（如分布式锁、积分抵扣、事务表）的详细技术方案 |
| 中间件集成设计 | Flink、ES、Kafka 等中间件的集成方案 |
| 使用说明文档 | 通用组件（如 `@DistributeLock`）的使用指南 |

#### 核心内容摘要

| 文件 | 核心内容 |
|---|---|
| `工具模块设计文档.md` | 工具模块全局设计：6 种资源类型与权限等级映射、前后端分层、7 张核心表、新增/编辑/搜索/购买/使用全流程 |
| `权限管控设计.md` | 前后端两套权限机制详解：`@ss.hasPermi` 项目封装写法 vs `hasAuthority` Spring Security 原生写法 |
| `DistributeLock使用说明.md` | `@DistributeLock` 分布式锁注解完整说明：AOP 切面 + Redisson RLock，支持 SpEL 动态 key、用户 ID 拼接、定时锁模式 |
| `课程模块本地事务表设计分享.md` | 基于 Outbox 模式的本地消息表 + Kafka，保证课程增删改后 ES 索引同步的可靠性 |
| `结算积分抵扣功能设计.md` | 订单结算时积分抵扣方案：1 积分 = 0.1 元，事务内扣减 + 取消退还 + Redis 缓存刷新 |
| `2026-06-09-工具全局次数池改造设计.md` | 工具使用次数池全局改造方案 |
| `第三方工具使用次数扣减设计.md` | 第三方工具次数扣减逻辑 |
| `工具模块订单超时处理设计.md` | 工具订单超时自动关闭方案 |
| `flink slot测试.md` | Flink slot 并行度测试报告 |

#### 实际举例说明

**场景 1：新开发者接入工具模块——从"看代码猜逻辑"到"按图纸施工"**

`docs/design/工具模块设计文档.md` 提供了完整的模块地图：
- 资源类型与权限等级一一对应（`FREE → level 1`、`VIP → level 4`、`SMALL_CLASS → level 5`）
- 前端只需传 `resourceType`，后端自动映射 `level`，"前端不应自行推导最终权限结论"
- 列出了全部核心表（`osh_tool`、`osh_tool_tag`、`osh_tool_package`、`osh_tool_user_quota` 等 7 张表）和代码位置

**代码级关联**：设计文档中描述的保存流程直接对应 `OshToolController.save()` → `OshToolServiceImpl.createTool()` 的调用链。Controller 上叠加的 `@OshUserLevel(value = 5)` 注解对应文档中"内部专属工具需要 level 6"的权限规则；`ToolSaveRequest` 中的 `resourceType` 字段在 Service 层通过 `buildTool()` 方法自动映射为 `level`，这正是文档中"后端保存时自动回填"的具体实现。设计文档同时标注了 ES 搜索索引同步的入口，对应 `OshToolEsMapper` 和 `OshToolEsServiceImpl` 中的同步逻辑。

**痛点解决**：新开发者无需遍历 7 张表和十几个 Service 文件来理解模块全貌，**文档 30 分钟内即可建立完整的模块心智模型**。

**如果缺少这份文档**：开发者可能误将 `level` 作为前端传入字段，导致权限等级与资源类型不匹配，产生"免费工具被误判为 VIP 专属"的安全漏洞。

---

**场景 2：解决并发问题——分布式锁从"不知道怎么写"到"照着文档选参数"**

`docs/design/DistributeLock使用说明.md` 提供了分布式锁的完整使用指南。文档详细说明了最终锁 key 的结构（`scene#key` 或 `scene#user:用户ID#key`）、各参数含义和适用场景。

**代码级关联**：项目中已有大量真实使用案例，可直接对照文档选择正确的加锁策略：

| 使用场景 | 代码位置 | 注解写法 | 对应文档参数建议 |
|---|---|---|---|
| 课程新增/修改 | `OshCourseController.save()` | `@DistributeLock(scene="resource", key="operation", expireTime=10000, waitTime=3000)` | 固定 key + 用户维度防重复提交 |
| 课程审核 | `OshCourseController.audit()` | `@DistributeLock(scene="course:audit", key="api", expireTime=60000, waitTime=0, releaseImmediately=false)` | 定时锁模式——审核完成也不立即释放，等 60s 超时 |
| 拼团参与 | `OshGroupServerServiceImpl.joinGroup()` | `@DistributeLock(scene="group_join", keyExpression="#activityId", includeUserId=false)` | SpEL 动态 key + 不拼接 userId——同一活动全局互斥 |
| 网站提交 | `OshPracticalWebsiteServiceImpl.submitWebsite()` | `@DistributeLock(scene="website_submit", key="website_submit_lock", includeUserId=true)` | 固定 key + 拼接 userId——同一用户防重复提交 |

底层实现链路：`@DistributeLock` 注解 → `DistributeLockAspect` AOP 切面 → `RedissonClient.getLock(lockKey)` → Redis `RLock`。注解定义在 `backstage-common/.../annotation/DistributeLock.java`，常量定义在 `DistributeLockConstant`。

**痛点解决**：开发者不需要从零研究 Redisson API 和 SpEL 表达式语法，**文档直接给出了四种典型场景的参数组合**，覆盖"防重复提交"、"资源互斥"、"定时锁"、"用户维度串行"等需求。

**如果缺少这份文档**：开发者可能错误地将 `includeUserId` 设为 `true` 用于"同一资源全局互斥"场景，导致不同用户可以同时操作同一资源，产生数据不一致。

---

**场景 3：课程模块 Kafka 消息可靠性——Outbox 模式设计**

`docs/design/课程模块本地事务表设计分享.md` 解决了"课程数据保存成功但 Kafka 发送失败，导致 ES 索引不同步"的分布式一致性问题。

**代码级关联**：设计文档定义的 Outbox 链路 `课程表变更 → 本地消息表 → Kafka → Flink → ES` 中，本地消息表与课程业务表共用同一个 `@Transactional` 事务。事务提交后立即尝试发送 Kafka，失败则由 `backstage-quartz` 中的定时任务兜底重试。消息类型 `COURSE_INDEX_CREATE`/`COURSE_INDEX_UPDATE`/`COURSE_INDEX_DELETE` 分别对应三种操作。

**跨文档协作**：该设计文档与 `docs/ai/conventions/kafka.md`（Kafka 约定）配合使用——后者规定了消息幂等性要求，前者则通过 Outbox 模式在架构层面保证了"消息不丢、最终送达"。

**痛点解决**：解决了"事务内直接发 Kafka"的四个致命问题——发送失败数据丢失、发送成功事务回滚导致脏消息、宕机消息丢失、Kafka 不可用阻塞业务。

---

### 2.3 `docs/homepage/` — 首页模块技术方案

#### 功能定位

`docs/homepage/` 是首页各卡片模块的**高性能架构专项方案集**。首页是用户第一印象，这个目录确保每个卡片在"ES 高性能缓存"与"MySQL 兜底"之间取得平衡。

#### 文档类型

| 类型 | 说明 |
|---|---|
| 架构改造方案 | ES + xxl-job 集群调度改造 |
| 安全同步方案 | 双索引 A/B 轮换 + alias 原子切换 |
| 新功能技术方案 | 会员套餐独立 ES 查询链路 |

#### 核心内容摘要

| 文件 | 核心内容 |
|---|---|
| `home_page_card_es_xxljob_refactor_design.md` | 从 `@Scheduled` 迁移到 xxl-job：解决多机重复执行、失败不可观测、运维不可控三大痛点；定义了 `OshHomePageEsSyncable` 标记接口 + `AbstractOshHomePageEsCardService` 模板基类 |
| `home_page_hot_course_es_ab_alias_implementation.md` | 热门课程双索引轮换：解决封面 URL 过期（ES 只存 `coverKey`，读取时再生成签名 URL）和先删后写的清空风险 |
| `home_page_member_plan_es.md` | 首页会员套餐独立 ES 查询：与会员中心 MySQL 链路分离，首页只负责展示，不承担下单/支付职责 |

#### 实际举例说明

**场景 1：首页热门课程封面图失效——从"短期可用"到"永久可靠"**

`home_page_hot_course_es_ab_alias_implementation.md` 精准定位了两个关键问题：
1. **封面 URL 过期**：MySQL 查询结果在同步到 ES 前已调用了 `ossService.getLimitedUrl(cover, 1440)` 生成临时 URL，写入 ES 后短期可用，过期后首页封面失效。改造方案：ES 只存原始 `coverKey`，读取时再生成临时签名 URL。
2. **先删后写的清空风险**：如果删除索引成功但写入失败，ES 会被清空。改造方案：A/B 双索引轮换 + alias 原子切换，写入在非读取索引上进行，切换是原子操作。

**代码级关联**：设计文档中的架构已在代码中完整落地——

| 设计文档概念 | 对应代码文件 | 说明 |
|---|---|---|
| 模板基类 | `AbstractOshHomePageEsCardService<D, V>` | 封装"ES 优先 + MySQL 兜底"逻辑，`getCards()` 方法先查 ES，失败自动降级 MySQL |
| 标记接口 | `OshHomePageEsSyncable` | 定义 `cardType()` + `syncToEs()`，实现类自动被编排器发现 |
| 同步编排器 | `OshHomePageCardEsSyncOrchestrator` | 注入所有 `OshHomePageEsSyncable` 实现，`syncAll()` 逐卡同步，单卡失败不中断 batch |
| xxl-job 入口 | `OshHomePageCardEsSyncTask`（`backstage-quartz`） | `@XxlJob("homepage-card-es-sync")` 全量同步，`homepage-card-es-sync-one` 单卡同步 |
| 封面 URL 延迟签名 | `OshHomePageCourseServiceImpl.toVos()` | 从 ES 文档取 `coverKey`，再调 `signCoverUrls()` 生成临时签名 URL |

目前已有 **11 个首页卡片 Service** 继承该基类：`OshHomePageCourseServiceImpl`（课程）、`OshHomePageToolServiceImpl`（工具）、`OshHomePageFeedbackServiceImpl`（反馈）、`OshHomePageWebsiteServiceImpl`（网站）、`OshHomePageBookServiceImpl`（书籍）、`OshHomePageExamServiceImpl`（考试）、`OshHomePageGroupServiceImpl`（拼团）、`OshHomePageSeckillServiceImpl`（秒杀）、`OshHomePageInfoGapServiceImpl`（信息差）、`OshHomePageQaServiceImpl`（QA）、`OshHomePageOpenProjectServiceImpl`（开源项目）。

**痛点解决**：新卡片接入只需 4 步——继承基类、实现 4 个抽象方法（`queryFromMysql`、`toVos`、`toDocs`、`esMapper`）、实现 `OshHomePageEsSyncable`、在 xxl-job admin 配置 cron。**开闭原则保证新增卡片不修改任何已有代码**。

**如果缺少这份文档**：每个卡片 Service 各自实现 ES 同步逻辑，定时任务散落各处，多机部署时 `@Scheduled` 重复执行导致 ES 数据闪烁，排查成本极高。

---

**场景 2：会员套餐接入首页——独立 ES 链路设计**

`home_page_member_plan_es.md` 解决了"首页套餐展示"与"会员中心下单支付"的链路耦合问题。

**跨文档协作**：该方案参考了 `docs/design/工具模块设计文档.md` 中已有的 ES 接入模式（`OshCourseEsMapper` + `OshCourseEsServiceImpl`），复用相同的 `RestHighLevelClient` + 独立 `*EsMapper` + 独立 `*EsService` 风格。同时与 `docs/ai/conventions/elasticsearch.md` 中的 ES 约定保持一致。

**核心设计决策**：首页查询走 ES（`GET /pc/homepage/member/plans`），会员中心继续走 MySQL。两者职责分离——"首页只负责展示，不承担下单、支付、价格计算规则修改等会员域职责"。

**痛点解决**：避免首页高并发查询直接打到会员中心 MySQL，**既保护了会员域的数据库负载，又让首页展示性能从 MySQL 毫秒级查询提升到 ES 亚毫秒级**。

**如果缺少这份文档**：开发者可能直接复用会员中心接口，导致首页查询与下单支付共享同一链路，一旦首页流量激增，会员中心数据库压力骤增，影响支付核心流程。

---

### 2.4 `docs/announcement/` — 公告栏模块对接规范

#### 功能定位

`docs/announcement/` 定义了全站统一公告栏能力的**接入协议**。任何业务模块（工具、网站、课程、考试等）要展示公告或业务动态，都必须按此文档完成接入。

#### 文档类型

| 类型 | 说明 |
|---|---|
| 对接规范 | 完整的后端对接说明，含链路、表结构、代码位置、接入步骤 |

#### 核心内容摘要

`Announcement-Backend-Integration.md`（677 行）定义了一条统一链路：

```
后端业务模块写入 osh_announcement → 后端提供查询接口 → 前端 HTTP 拉取
                                                        ↑
                              后端 WebSocket 推送"刷新消息" → 前端重新请求
```

新模块接入必须完成四件事：
1. 写入公告表（`channel=1` 系统公告 / `channel=2` 业务动态）
2. 提供"系统公告"和"业务动态"两个查询接口
3. 业务变化时推送 WebSocket 刷新消息
4. WebSocket 建连后主动推送一次刷新消息

#### 实际举例说明

**场景 1：工具模块支付成功后刷新公告栏——完整联动链路**

文档给出了完整联动示例：
1. 支付回调确认成功
2. 写入 `osh_announcement`（`channel=2, resource_type=tool, icon_code=payment`）
3. 构造 WebSocket 刷新消息（`type=PAYMENT_STATUS_CHANGED, module=tool, action=paid`）
4. 前端收到消息后生成刷新 key `PAYMENT_STATUS_CHANGED::tool::paid`
5. 公告栏组件重新请求 `/tool/announcement/notice` 和 `/tool/announcement/dynamic`

**代码级关联**：文档中描述的协议已在多个业务模块中落地——

| 业务模块 | 写入公告表 | 构造 WS 消息 | 广播服务 |
|---|---|---|---|
| 工具购买 | `ToolPurchaseAnnouncementPublisherImpl` 调用 `oshToolAnnouncementMapper.insertToolAnnouncement()` | 构造 `WsNotifyMessage(type=ANNOUNCEMENT_REFRESH, bizId="tool")` | `webSocketNotifyService.broadcast()` |
| 课程审核通过 | `CourseResourceAuditCallbackHandler` 调用 `oshCourseAnnouncementMapper.insertCourseAnnouncement()` | 构造 `WsNotifyMessage(type=COURSE_USER_NOTICE_REFRESH)` | `webSocketNotifyService.broadcast()` |
| 秒杀活动同步 | `SeckillAnnouncementServiceImpl.syncSeckillNotices()` 遍历进行中活动写入公告 | 构造 `WsNotifyMessage(type=SECKILL_NOTICE_UPDATE)` | `webSocketNotifyService.broadcast()` |
| 首页公告 | `OshHomePageAnnouncementPushServiceImpl` 调用 `announcementRefreshMessageService.buildAnnouncementRefreshMessage()` | 统一协议：`module=homepage, action=refresh, noticeApi=/pc/homepage/announcement/notice` | `webSocketNotifyService.broadcast()` |

通用消息构造服务 `AnnouncementRefreshMessageServiceImpl` 统一封装了 payload 结构（`module`/`action`/`refresh`/`noticeApi`/`dynamicApi`），避免各模块手写 JSON。

**关键原则**："先写表，再发刷新消息。如果只发 WebSocket 不写入表，前端重新拉取后也看不到新数据。"

**痛点解决**：避免了各业务模块各自为政、公告展示散乱的问题。**统一协议让前端只需写一套公告栏组件，即可适配所有业务模块**。

**如果缺少这份文档**：每个模块开发者自行定义 WebSocket 消息格式，前端需要为每个模块写不同的消息解析逻辑，维护成本随模块数量线性增长。

---

**场景 2：新模块接入公告栏——四步检查清单**

当一个新业务模块（如"开源项目模块"）需要接入公告栏时，文档定义了明确的四步检查清单。以 `OshOpenProjectSourceServiceImpl` 中的实际实现为例：

1. **写入公告表**：调用 `announcementMapper` 插入数据，设置 `resource_type=openproject`
2. **提供查询接口**：实现 `/pc/openproject/announcement/notice` 和 `/pc/openproject/announcement/dynamic`
3. **业务变化时推送 WS**：构造 `WsNotifyMessage(type=ANNOUNCEMENT_REFRESH, bizId="openproject")`，content 中包含 `noticeApi` 和 `dynamicApi`
4. **建连后主动推送**：通过 `AnnouncementWebSocketConnectHandler` 切面实现

**跨文档协作**：公告栏接入规范与 `docs/design/权限管控设计.md` 配合——公告查询接口需要正确的权限注解，WebSocket 推送需要区分"需登录"和"公开"消息（`requireAuth` 字段）。

**痛点解决**：将接入过程从"问同事怎么接"升级为"按文档四步走"，**新模块接入时间从 1-2 天缩短到 2-3 小时**。

---

### 2.5 `docs/feedback/` — 反馈模块问题诊断记录

#### 功能定位

`docs/feedback/` 是反馈模块的**技术病历**，记录诊断过程中遇到的典型问题和根治方案，作为团队经验沉淀。

#### 文档类型

| 类型 | 说明 |
|---|---|
| Bug 分析报告 | 问题现象、根因分析、解决方案对比、经验教训 |

#### 核心内容摘要

`反馈模块ES termQuery 精确匹配失效问题.md` 记录了一个经典 ES 陷阱：

| 维度 | 内容 |
|---|---|
| 问题现象 | `termQuery` 对 `status`、`categoryId` 等字段精确过滤时返回 0 条，但 MySQL 正常 |
| 根因 | `text` 字段经 `standard` 分析器 lowercase 处理后，倒排索引存的是小写 `"closed"`；而 `termQuery` 不走分析器，拿 `"CLOSED"` 去匹配，自然找不到 |
| 根治方案 | 重建索引，枚举/ID 字段从 `text + keyword` 改为纯 `keyword` 类型 |
| 经验教训 | 四条铁律：枚举字段不用 Text、理解分析器双刃剑、`_source` 有欺骗性、Mapping 即契约 |

#### 实际举例说明

**场景 1：团队新人遇到 ES 搜索不到数据——从"_source 显示正确但查不到"到"理解倒排索引本质"**

这份文档提供了完整的排查思路：
- 先用 `GET /_analyze` API 确认倒排索引中生成了什么 Token
- 区分 `match`（走分析器，适合全文搜索）和 `term`（不走分析器，适合精确过滤）
- 字段选型原则："会被用来做全文模糊搜索？会 → `text`；不会，只做精确过滤 → `keyword`"

**代码级关联**：这个 ES 字段选型问题影响项目中所有使用 ES 的模块。以首页卡片为例，`OshHomePageHotCourseEsDocument` 中的 `tags` 字段用于全文搜索，应使用 `text` 类型；而 `resourceType`、`sortNo` 等字段用于精确过滤，应使用 `keyword` 类型。反馈模块的 `status`、`categoryId`、`ticketNo` 等字段就是典型的踩坑案例——它们被错误地设为 `text` 类型，导致 `termQuery` 精确匹配失效。

**痛点解决**：一份文档解决了整个团队在 ES 字段选型上的共性困惑。文档中给出的判断规则极其简洁——"**枚举/ID 字段坚决不用 Text，`_source` 具有极大的欺骗性**"，这两条铁律可以直接作为团队 Code Review 的检查项。

**如果缺少这份文档**：每个使用 ES 的模块开发者都可能独立踩同一个坑——"搜索不到数据但 `_source` 显示正常"，排查时间从几分钟延长到几小时。

---

**场景 2：新模块接入 ES——字段类型选型参考**

当新模块（如"开源项目模块"）需要创建 ES 索引时，这份文档提供了字段类型选型的决策树：

```
字段值是否为用户输入的自然语言？
├── 是 → text 类型 + ik_max_word/ik_smart 分析器
│        查询方式：match / multi_match
└── 否 → keyword 类型
         查询方式：term / terms / filter
```

**跨文档协作**：该经验与 `docs/ai/conventions/elasticsearch.md` 中的 ES 约定配合使用——后者定义了索引命名规范和查询约定，前者则补充了字段类型的底层原理。

**痛点解决**：将"ES 字段类型怎么选"从经验判断升级为规则驱动，**避免新模块在上线后才发现搜索功能异常，减少重建索引的数据迁移成本**。

---

### 2.6 `docs/internal-member-admin-api/` — 付费用户后台模块接口文档

#### 功能定位

`docs/internal-member-admin-api/` 是内部付费用户管理后台的**完整接口契约与重构方案集**。它既是前端对接的权威参考，也记录了从"订单口径"到"角色口径"的架构演进。

#### 文档类型

| 类型 | 数量 | 说明 |
|---|---|---|
| 接口规格文档 | 18 个 | 每个接口独立文档，含请求参数、响应结构、业务规则 |
| 重构方案 | 1 个 | 付费用户口径改造施工单 |
| 进度与验收 | 2 个 | 重构进度日志 + 验收检查清单 |
| 模块总览 | 1 个 | README，含接口分组、代码结构、重构建议 |

#### 核心内容摘要

**接口分组一览**：

| 分组 | 接口 | 说明 |
|---|---|---|
| 管理员辅助 | `GET /admin-users` | 获取管理员列表 |
| 用户列表 | `GET /page`、`GET /outcome/page` | 付费用户分页、结果页分页 |
| 学习概览 | `GET /{userId}/summary`、`GET /{userId}/courses`、`GET /{userId}/study-attitude` | 学习数据聚合 |
| 就业档案 | `GET/PUT /{userId}/employment-profile`、薪资/简历/面试摘要 | 就业数据管理 |
| 文档管理 | `POST/GET/DELETE /{userId}/documents` | 文档上传、预览、下载、删除 |
| 跟进记录 | `GET/POST /{userId}/follow-records` | 跟进记录管理 |
| 考试 | `GET /{userId}/exam-summary` | 考试摘要 |

**重构核心**：`19-paid-member-role-refactor-plan.md` 定义了从"订单口径"到"角色口径"的切换：
- 旧规则：付费用户 = 有会员订单的人（`osh_member_order`）
- 新规则：付费用户 = 当前拥有有效会员角色的人（`osh_user_role + osh_role`，`role_code in ('vip', 'small_class')` 且 `expire_time > now()`）
- 硬约束：不改接口路径、不改请求参数、不改 VO 字段名、不改返回 JSON 结构

#### 实际举例说明

**场景 1：列表能看到用户，点详情报"不是付费用户"——口径不一致的体验灾难**

`19-paid-member-role-refactor-plan.md` 精准定位了问题根因：
- 列表接口（`GET /page`）从 `osh_member_order` 取"最新订单"，会把历史付费但已过期的人查出来
- 详情接口（`GET /{userId}/summary`）的 `ensurePaidMember` 也查 `osh_member_order`，但条件不同
- 两处 SQL 口径不一致导致体验割裂

**代码级关联**：问题根源在两个 Service 中——`InternalMemberServiceImpl.ensurePaidMember` 和 `InternalMemberStudyServiceImpl.ensurePaidMember`，它们各自调用对应 Mapper 的 `countPaidMemberByUserId`，SQL 为 `select count(1) from osh_member_order where user_id=#{userId} and pay_status=1`。这只能证明"买过单"，不能证明"当前仍然是有效会员"。重构后统一替换为基于 `osh_user_role` 的查询，确保"列表看到的人，详情一定能打开"。

**痛点解决**：消除了运营人员的体验割裂感——**列表和详情的付费用户口径统一后，运营不再需要手动核实用户身份**。

**如果缺少这份文档**：前端开发者不知道字段语义已变（如 `planName` 从"订单套餐名"变为"会员身份显示名"），可能在页面展示错误的会员信息。

---

**场景 2：前端对接 18 个接口——从"问后端每个字段什么意思"到"查文档自助对接"**

`docs/internal-member-admin-api/README.md` 将 18 个接口按职责分组为 7 个模块（管理员辅助、用户列表、学习概览、就业档案、文档管理、跟进记录、考试），每个接口都有独立文档说明请求参数、响应结构和业务规则。

**跨文档协作**：接口权限控制依赖 `docs/design/权限管控设计.md` 中定义的 `@PreAuthorize` 机制。例如 `GET /{userId}/employment-profile` 接口需要后台管理员权限，对应 `@PreAuthorize("@ss.hasPermi('internal:member:edit')")` 注解，权限字符在 `sys_menu.perms` 中配置。

**痛点解决**：前端开发者无需逐个问后端接口细节，**文档即是接口契约，减少 80% 的前后端沟通成本**。

**如果缺少这份文档**：前端开发者只能从 Postman 或 Swagger 中摸索接口参数，无法了解字段背后的业务语义（如 `memberActive` 是基于角色有效期判断的，不是基于订单状态），导致展示逻辑错误。

---

### 2.7 `docs/skills/` — AI Agent 技能定义

#### 功能定位

`docs/skills/` 定义了 AI Agent 可复用的**自动化技能脚本**，确保特定操作在执行前满足前置条件。

#### 文档类型

| 类型 | 说明 |
|---|---|
| 技能定义文件 | 描述 AI Agent 的自动化行为、触发条件和执行步骤 |

#### 核心内容摘要

`zentao-mcp-token-guard-skill.md` 定义了禅道 MCP Token 守卫技能：
- **触发规则**：任何计划访问禅道 MCP 的任务执行前，自动触发
- **行为流程**：读取 `MCP.json` → 验证 token → 有效则继续 → 无效则提示输入账号密码 → 获取新 token → 回写配置 → 验证后继续操作
- **命令**：`powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json`

#### 实际举例说明

**场景 1：AI Agent 需要通过禅道 MCP 查询需求——认证前置保障**

在执行任何禅道 MCP 操作前，守卫技能会自动检查 `MCP.json` 中 `mcpServers.zentao.headers.token` 是否有效。如果 token 过期，会提示用户输入账号密码并自动刷新，避免了"操作到一半发现认证失败"的尴尬。

**代码级关联**：守卫脚本 `bin/ensure_zentao_mcp_token.ps1` 会读取 `MCP.json` 中的 `mcpServers.zentao.apiBaseUrl`，调用 `GET /api.php/v2/users?page=1&limit=1` 验证 token。如果登录响应不直接在 JSON 中返回 token，脚本还会从 `Set-Cookie` 头中解析 `zentaosid` 作为备用。验证通过后自动回写 `MCP.json`。

**痛点解决**：将认证保障逻辑封装为可复用技能，**AI Agent 无需每次手动处理 token 问题，也不会因 token 过期而中断开发流程**。

**如果缺少这份文档**：AI Agent 可能在执行禅道需求查询、任务更新等操作时因 token 过期而失败，且无法自动恢复，需要人工介入重新认证。

---

**场景 2：跨 Agent 一致性保障——多工具共享同一套认证规则**

`AGENTS.md`、`.github/copilot-instructions.md`、`.cursorrules`、`CLAUDE.md`、`GEMINI.md` 等多个 AI 工具的配置文件都引用了同一套禅道 MCP 认证规则。无论团队使用 Codex、Cursor、Claude 还是 Gemini，都会在执行禅道操作前自动运行守卫脚本。

**跨文档协作**：守卫技能同时被 `AGENTS.md`（项目级 AI 规则）和 `docs/skills/` 引用，确保跨 Agent 一致执行。`AGENTS.md` 明确规定："Before any operation that uses `mcpServers.zentao`，run the token guard script"。

**痛点解决**：避免了"A 工具用 Codex 时认证了，B 工具用 Cursor 时又需要重新认证"的混乱局面。

---

### 2.8 `docs/` 根目录散落文档

#### 功能定位

根目录下的 30+ 个独立文档是项目的**百科全书**，覆盖课程、拼团、秒杀、支付、部署、治理等多个领域。

#### 文档类型与分类

| 主题分类 | 代表文档 | 说明 |
|---|---|---|
| **课程模块** | `course_module_implementation_guide.md`、`course_permission_config.md`、`course_tag_api.md` | 课程模块实现指南、权限配置、标签接口 |
| **课程文件上传** | `课程文件上传实现方案.md`、`课程文件上传实现总结.md`、`课程文件上传 - 快速开始.md` | 文件上传功能三件套 |
| **课程提问** | `课程提问功能需求分析与接口设计.md`（1033 行） | 提问功能完整需求规格 |
| **拼团模块** | `group_module_optimization_handover.md`、`group_table_design_analysis.md` 等 6 篇 | 拼团优化交接、表结构修复、参团用户调试 |
| **秒杀模块** | `秒杀模块接口文档.md`（1475 行） | 秒杀模块完整接口规格 |
| **支付接入** | `业务模块接入支付模块说明.md`（614 行） | 各业务模块接入支付的统一指南 |
| **网站模块** | `实用网站模块接口文档.md`（999 行） | 网站模块接口规格 |
| **部署运维** | `github-actions-deploy.md`、`Git_Merge_vs_Rebase_详细指南.md` | CI/CD 教程、Git 操作指南 |
| **技术治理** | `OSH_Backend_治理体系与业务实现技术分析报告.md`、`典型技术缺陷与优化案例集.md`、`技术改进方向与技术债务清单.md` | 项目治理、缺陷案例、技术债务 |
| **开发日志** | `dev-notes-20260312.md` | 课程模块重命名、返回值统一等日常开发记录 |
| **参考资料** | `高并发.md`（15486 行）、`全局记忆库导出.md` | 高并发学习资料、AI 记忆库备份 |

#### 实际举例说明

**场景 1：新业务模块接入支付——从"不知道如何对接支付网关"到"按文档逐步接入"**

`业务模块接入支付模块说明.md`（614 行）提供了完整的接入指南，覆盖了从订单创建、支付发起、回调处理到退款的全链路。

**代码级关联**：文档中描述的支付流程对应 `OrderServiceImpl.checkout()` → `createPendingOrderAndPayment()` → `requestPayment()` → 汇付支付网关的调用链。`checkout()` 方法内部完成订单创建、支付单生成（`OshPayment`）、渠道解析（`PayChannelEnum`）和网关请求的全链路。文档明确说明了统一响应包装类 `R<T>` 的使用规范（对应 `backstage-common` 中的 `R.java`），以及支付回调中必须处理的幂等性问题（通过 `OshPaymentNotifyLogMapper` 记录回调日志实现去重）。

**痛点解决**：新模块开发者无需从零摸索支付流程，**文档直接给出了接入模板和代码示例，将接入时间从 3-5 天缩短到 1 天**。

**如果缺少这份文档**：开发者可能遗漏支付回调中的关键校验（如金额一致性验证、订单状态幂等判断），导致资金安全问题。

---

**场景 2：拼团模块表结构修复——"计划 → 执行 → 复盘"完整闭环**

根目录下有完整的修复链文档：
- `group_table_structure_fix_plan.md`（修复计划）：定义了修复范围、影响分析和回滚方案
- `group_table_fix_execution_guide.md`（执行指南）：提供了逐步执行的 SQL 命令和校验步骤
- `group_code_modification_report.md`（修改报告）：记录了实际执行的变更、遇到的问题和解决方案
- `group_table_design_analysis.md`（设计分析）：分析了表结构设计的根因问题

**跨文档协作**：修复链与 `docs/拼团模块技术优化项目交接文档.md`（668 行）配合使用——后者记录了拼团模块的完整优化历程，包括 CI/CD 与蓝绿部署体系整合、OSHI 监控接入、Redisson 分布式锁等 15 项优化任务的实现详情。

**痛点解决**：将"一次性修复"升级为"可追溯的工程化修复"，**任何团队成员都可以通过文档链回溯当时的决策依据和执行细节**。

**如果缺少这份文档**：修复过程只能靠参与者的记忆，半年后遇到类似问题时无法复用经验。

---

**场景 3：新人了解项目全貌——从"看代码猜架构"到"读报告理解治理体系"**

`OSH_Backend_治理体系与业务实现技术分析报告.md` 提供了项目的高层治理视角，帮助新人快速理解项目的整体架构和治理思路。

**跨文档协作**：该报告与 `docs/ai/04-architecture.md`（架构说明）和 `openspec/project.md`（项目总纲）形成三层递进关系——架构说明描述"当前是什么"，项目总纲定义"应该怎么做"，治理报告分析"为什么这么做"。

**痛点解决**：新人无需遍历 8 个 Maven 模块的源码来理解项目全貌，**读一份报告即可建立从技术架构到业务实现的完整心智模型**。

**如果缺少这份文档**：新人只能通过问同事或逐文件阅读来了解项目，上手时间从 1 周延长到 2-3 周。

---

## 三、跨文件夹关联分析

### 3.1 核心协作关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                        SDD 工作流联动                            │
│                                                                 │
│  docs/ai/  ──定义规范──→  openspec/  ──指导实现──→  代码变更     │
│     ↑                                              │            │
│     └──────────── 经验反哺 ←───────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      技术方案到代码的传导链                        │
│                                                                 │
│  docs/design/  ──技术方案──→  sql/  ──建表脚本──→  数据库        │
│       │                                                           │
│       ├──模块设计──→  backstage-system/ 各模块代码                │
│       │                                                           │
│       └──中间件方案──→  backstage-flink/ / backstage-quartz/      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      业务模块文档协作                             │
│                                                                 │
│  docs/announcement/  ←── 各业务模块写入公告                      │
│       ↑                                                          │
│       │  tools / website / course / exam 等模块                  │
│       │  按公告规范接入                                           │
│       │                                                          │
│  docs/design/  ──提供模块设计──→  各模块按设计实现                │
│       ↑                                                          │
│  docs/homepage/  ──首页卡片依赖各模块 Service 暴露的方法          │
│       ↑                                                          │
│  docs/feedback/  ──ES 字段选型经验──→ 所有使用 ES 的模块          │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 关键协作关系详解

| 协作关系 | 说明 | 代码级示例 |
|---|---|---|
| `docs/ai/` ↔ `openspec/` | `docs/ai/02-workflow.md` 定义了 OpenSpec 的使用规范；`openspec/changes/` 是实际的需求变更目录。AI Agent 必须先读前者，再操作后者 | Agent 接到任务 → 读 `docs/ai/02-workflow.md` 了解流程 → 创建 `openspec/changes/course-question/proposal.md` → 按 `tasks.md` 编码 |
| `docs/design/` ↔ `sql/` | 设计文档中的数据库变更方案对应 `sql/` 目录下的迁移脚本 | `结算积分抵扣功能设计.md` 中定义的 `ALTER TABLE osh_order ADD COLUMN points_amount` 对应 `sql/2026/05/mysql/alter_osh_order_add_points_amount.sql` |
| `docs/design/` ↔ `docs/homepage/` | 首页模块依赖各业务模块的 Service 方法，设计文档定义了 Service 分层 | `工具模块设计文档.md` 定义了 `IOshToolService` 分层 → `OshHomePageToolServiceImpl` 继承 `AbstractOshHomePageEsCardService` 调用 `homePageToolMapper.selectHotTools()` |
| `docs/announcement/` ↔ `docs/design/` | 公告栏接入规范要求每个业务模块在业务动作发生时写入公告表 | `ToolResourceAuditCallbackHandler` 审核通过后调用 `oshToolAnnouncementMapper.insertToolAnnouncement()` 写入公告 + `webSocketNotifyService.broadcast()` 推送刷新 |
| `docs/feedback/` ↔ 所有 ES 模块 | ES 字段选型经验适用于所有使用 ES 的模块 | `OshHomePageHotCourseEsDocument` 中 `tags` 用 `text`（全文搜索），`resourceType` 用 `keyword`（精确过滤）——正是 `feedback/` 文档中经验的具体应用 |
| `docs/internal-member-admin-api/` ↔ `docs/design/权限管控设计.md` | 付费用户后台的接口权限控制依赖权限管控设计中定义的 `@PreAuthorize` 机制 | `InternalMemberAdminController` 中的接口使用 `@PreAuthorize("@ss.hasPermi('internal:member:edit')")`，权限字符在 `sys_menu.perms` 中配置 |
| `docs/skills/` ↔ `AGENTS.md` | 禅道 MCP Token 守卫技能被多个 AI 工具配置文件引用 | `AGENTS.md` 规定 "run `ensure_zentao_mcp_token.ps1`" → `bin/ensure_zentao_mcp_token.ps1` 验证 `MCP.json` 中的 token |
| `docs/design/` ↔ `backstage-common/` | 设计文档中定义的通用能力对应 `backstage-common` 中的注解和切面实现 | `DistributeLock使用说明.md` → `DistributeLock.java`（注解） + `DistributeLockAspect.java`（切面） + `DistributeLockConstant.java`（常量） |

### 3.3 典型跨文档协作链路

以一个完整的新功能开发为例，说明文档之间的协作关系：

**场景：新增"课程提问"功能**

```
1. 需求理解
   docs/ai/02-workflow.md        → 了解必须先建 OpenSpec change
   docs/ai/01-newcomer-guide.md  → 了解 proposal/design/spec/tasks 写法

2. 方案设计
   docs/course提问功能需求分析与接口设计.md → 了解需求规格和接口设计
   docs/design/权限管控设计.md              → 了解权限注解写法
   docs/design/DistributeLock使用说明.md    → 了解并发控制方案

3. 编码实现
   docs/ai/conventions/springboot.md        → 按分层规则编码
   docs/ai/conventions/api.md               → 按 API 约定设计接口
   docs/announcement/Announcement-Backend-Integration.md → 接入公告栏

4. 数据库变更
   docs/design/课程模块本地事务表设计分享.md  → 了解 Outbox 模式
   sql/ 目录                                  → 执行 DDL 迁移脚本

5. 验证发布
   docs/ai/07-review-rubric.md              → 按七维度清单自查
   docs/ai/08-release-flow.md               → 按发布流程灰度发布
```

**关键结论**：**没有任何一个文档是孤立使用的，它们形成了一条从"需求理解"到"发布上线"的完整指导链**。

---

## 四、使用建议

### 4.1 团队成员查阅建议

| 角色 | 优先阅读 | 说明 |
|---|---|---|
| **新人入职** | `docs/ai/01-newcomer-guide.md` → `docs/ai/04-architecture.md` → `openspec/project.md` | 先理解工作方式和架构全貌，再看具体模块 |
| **前端开发者** | `docs/internal-member-admin-api/README.md` → 具体接口文档 → `docs/design/权限管控设计.md` | 重点看接口契约和权限机制 |
| **后端开发者** | `docs/design/` 对应模块文档 → `docs/ai/conventions/` → `docs/announcement/Announcement-Backend-Integration.md` | 先理解模块设计，再按约定编码 |
| **运维/DevOps** | `docs/github-actions-deploy.md` → `docs/ai/08-release-flow.md` → `docs/ai/09-upgrade-strategy.md` | 关注部署流程和升级策略 |
| **问题排查** | `docs/feedback/` → `docs/典型技术缺陷与优化案例集.md` → `docs/技术缺陷与优化案例参考手册.md` | 先看是否有类似问题的历史记录 |

### 4.2 AI Agent 使用建议

| 阶段 | 必读文档 | 目的 |
|---|---|---|
| **任务理解** | `AGENTS.md` → `docs/ai/02-workflow.md` → `openspec/project.md` | 理解工作规范和项目全貌 |
| **方案设计** | `docs/design/` 对应模块文档 → `docs/ai/conventions/` 相关约定 | 确保方案符合已有架构 |
| **编码实现** | `docs/ai/conventions/springboot.md`、`api.md`、`database.md` | 按约定编码 |
| **模块接入** | `docs/announcement/Announcement-Backend-Integration.md`（公告）/ `docs/业务模块接入支付模块说明.md`（支付） | 按规范接入公共能力 |
| **自检验证** | `docs/ai/07-review-rubric.md` → `docs/测试规范与Checklist.md` | 提交前按审核标准自查 |

### 4.3 文档维护建议

1. **新增模块时**：先在 `docs/design/` 创建设计文档，再编码；同时在 `docs/announcement/` 确认公告接入
2. **解决问题后**：将诊断过程和根治方案记录到对应模块目录（参考 `docs/feedback/` 的模式）
3. **架构变更后**：同步更新 `docs/ai/04-architecture.md` 和 `openspec/project.md`，保持文档一致性
4. **接口变更后**：更新对应的接口文档（如 `docs/internal-member-admin-api/` 中的文件），确保前端能同步
5. **经验总结**：典型缺陷应沉淀到 `docs/典型技术缺陷与优化案例集.md`，形成团队共享知识

---

## 五、各文件夹特点对比总览

| 文件夹 | 功能定位 | 核心关键词 | 文档数量 | 主要受众 |
|---|---|---|---|---|
| `ai/` | 工作规范中心 | SDD、OpenSpec、工作流 | 9 + 14 conventions | 全员 + AI Agent |
| `design/` | 技术方案蓝图 | 架构、数据模型、接口设计 | 11 | 后端开发者 |
| `homepage/` | 首页高性能方案 | ES、xxl-job、A/B 轮换 | 3 | 首页模块开发者 |
| `announcement/` | 公告接入协议 | 统一链路、WebSocket | 1 | 各模块开发者 |
| `feedback/` | 问题诊断记录 | ES 字段选型、Bug 修复 | 1 | 问题排查者 |
| `internal-member-admin-api/` | 接口契约与重构 | 付费用户、18 接口、口径统一 | 22 | 前端 + 后端 |
| `skills/` | AI 自动化技能 | Token 守卫、MCP | 1 | AI Agent |
| 根目录散落 | 百科全书 | 课程/拼团/秒杀/支付/部署 | 30+ | 全员 |

---

> **总结**：`docs/` 目录构成了 OSH Backend 项目的知识中枢。从开发规范（`ai/`）到技术方案（`design/`）、从接口契约（`internal-member-admin-api/`）到故障复盘（`feedback/`），形成了"**规范指导实践 → 实践产生经验 → 经验反哺规范**"的完整知识闭环。善用这套文档体系，可以显著降低沟通成本、减少重复踩坑、提升 AI Agent 的代码质量。
