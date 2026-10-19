---
title: CLAUDE.md（项目宪法）
path: CLAUDE.md
version: v1.0
status: active           # 人工拍板后由 draft 升 active
owner: @heroyang
created: 2026-10-18
---

# CLAUDE.md — osh-backend 项目宪法（五层 · 100 行内）

> 本文件自动常驻，只放"每轮都必须遵守"的结论，细则进索引（第 3/4 层）；超 100 行 = 红线被略读，超了先删。

## 第 1 层 · 项目身份

- **是什么**：OSH 平台后端（主站/课程/订单/拼团/秒杀/工具/电子书/后台），
  Spring Boot 2.5.15 单体多模块 Maven，包根 `com.backstage`。
- **不是什么**：不是微服务；不是 Spring Boot 3；不是 Java 17 语法
  （运行语义 Java 8：禁 var / record / 文本块，CI 用 JDK 17 构建）。
- **栈**：MyBatis-Plus 3.5 + PageHelper + Druid；Redis/Redisson、ES、Kafka、
  Nacos、XXL-JOB、Cloudflare R2。无 Flyway——DDL 走 `sql/` 脚本，人审后执行。
- **架构**：模块化单体。跨模块调用只走 `backstage-service-contract`；
  依赖方向纪律由 ArchUnit 门禁执法（见 R4），不靠自觉。
- **命名口径**三铁律：C 端 `userId`(OshUser) ≠ B 端 `SysUser` 不许混用；秒杀正名 `seckill`
  （`flashsale`=别名禁用）；`group`=拼团 ≠ `servergroup`=服务人员分组——全量词表见 `.harness/wiki/领域术语.md`。
- 当前迭代任务看 `docs/plans/current-sprint.md`（状态单一事实源，不抄进本文）。

## 第 2 层 · 核心约束（critical 红线 · 命中即停先问人，违例 = PR fail）

> 只收可判定且有机器抓手的 critical；红线写进文档 ≠ 被拦截，生效以 CI 实跑为准。
>
> ⚠️ **个人维护模式（迁移后现状）**：R2/R3/R7（OpenSpec 门禁）与 R6（DDL 守卫）已从 `pr-check.yml` 移除，**不再是 CI 门禁**——R2/R3/R7 仅 `bash scripts/verify-ai-process.sh` 手动可跑，R6 仅本地 pre-commit 钩子。真正仍由 CI 强制的是 R1（gitleaks）、R4/R5/R9（ArchUnit）、R8（PR 标题）与构建+核心测试。

| # | 红线 | 检查语句（CI 抓手） |
|---|---|---|
| R1 | token/密码/私钥/日志产物不许进仓 | pr-check.yml 的 gitleaks job |
| R2 | 触碰 `pom.xml`、`sql/`、`application*.yml\|properties` 建议带非空 `openspec/changes/<id>/` 四件套（**个人仓已从 CI 移除，不再强制**） | ~~pr-check.yml~~ 仅手动 `bash scripts/verify-ai-process.sh` |
| R3 | 中大型或影响行为/接口/权限/数据的改动建议带四件套（个人仓已非强制；如需恢复门禁，重跑 verify-ai-process.sh） | 同上（已移出 CI） |
| R4 | **依赖方向红线**四条：① 分层只许 Controller→Service→Mapper 单向，common 不许反向；② 禁反向依赖三形态——Mapper 调 Service、Entity 注入 Service、Service/Entity 反向调用 Controller；③ 跨域取数只走对方 Service 接口，不得注入其他域的 Mapper（Controller 直连 Mapper 同禁），跨域反向通知走领域事件/outbox、禁反向回写；④ 禁经 static 工具类 `SpringUtils.getBean` 运行时取 Bean（编译期看不见、静态扫描最难抓，最危险形态） | ①②：`mvn test -pl backstage-admin -Dtest=LayeredArchitectureTest`；④：`-Dtest=CodingConventionArchitectureTest` 冻结强制（存量 14 处入基线只拦新增）；③：无稳定域包锚点，暂评审拦截+import 台账(≥207)，ArchUnit 化待前置包规整化 change |
| R5 | 禁 `e.printStackTrace()`；裸 `System.out.println` / 裸 HTTP 禁新增（存量冻结） | `mvn test -pl backstage-admin -Dtest=CodingConventionArchitectureTest` |
| R6 | 改表必须同步 DDL/ALTER/Entity/VO 四层一致 | 个人仓已移出 CI；仅本地 pre-commit 钩子 `code-quality-guard.sh` |
| R7 | `openspec/changes/archive/` 只读——修正历史须新建 change（个人仓已移出 CI） | ~~pr-check.yml~~ 仅手动 verify-ai-process.sh 归档守卫 |
| R8 | PR 标题必须满足 Conventional Commits `<type>(<scope>)?: <subject>` | pr-check.yml 标题 lint |
| R9 | ArchUnit 两测试文件与 `backstage-admin/archunit-store/` 必须在 git 索引内，缺一即门禁静默失效 | "Assert ArchUnit test files present" 步骤 |

R4 的 ④ 已由 ArchUnit 冻结门禁强制（见 `openspec/changes/close-r4-gate-gap`）；③ 因扁平 Mapper/双名域无稳定包锚点，
暂为评审级——补齐须先做“按域纵切包规整化”前置 change 再上 ArchUnit。AI 违例 ③④ 前先停记 `.harness/candidate/`。

## 第 3 层 · 规范索引（内容 → 位置 → 何时装载）

> 只登记"有这事、在哪、何时读"，不抄正文；暂无 paths 自动通道，全部手动命中即读。

| 内容 | 位置 | 何时装载 |
|---|---|---|
| 模块职责与依赖链 | `docs/architecture/overview.md` | 建/挪 Maven 模块 |
| 模块边界与调用方向矩阵 | `docs/architecture/module-dependency-rules.md` | 跨模块调用 |
| 层内职责七层表 | `docs/architecture/boundaries.md` | 类落位争议 |
| 包路径 / 命名规约 | `docs/architecture/package-structure.md` | 新类落位 |
| 数据流与一致性链路 | `docs/architecture/data-flow.md` | 缓存/ES/Kafka 一致性 |
| 响应格式与前端对接 | `docs/reference/api-spec.md` | 写接口 |
| 异常体系与错误码 | `docs/reference/error-codes.md` | 抛错、定义异常 |
| 编码规范（含真实示例） | `docs/ai/conventions/coding-standards.md` | AI 新增代码必读 |
| 数据库表约定 | `docs/ai/conventions/database.md` | 涉表 |
| 日志规范 | `docs/ai/conventions/logging.md` | 写日志 |
| 安全/认证约定 | `docs/ai/conventions/security.md` | 涉认证鉴权 |
| 中间件约定 | `docs/ai/conventions/` 下 redis/kafka/elasticsearch/nacos/object-storage/middleware.md | 碰对应组件 |
| 工作流与分支策略 | `docs/ai/02-workflow.md` | 开始改动前 |
| 测试规范与 Checklist | `docs/ai/06-testing.md` | 写测试 |
| 审核标准 | `docs/ai/07-review-rubric.md`、`CODE_REVIEW.md` | 提 PR 前 |
| candidate 记录格式与晋升规则 | `.harness/candidate/README.md` | 登记/晋升暂存区条目时 |
| 外部调用规范（线程池/超时/熔断降级） | `.harness/外部依赖规范.md` | 写/改任何外部调用（支付/物流/短信/HTTP 客户端）时 |
| wiki 五层知识库（业务/接口/数据/术语/复盘） | `.harness/wiki/` | 按 ROUTING 表 1 任务类型对应装载 |

## 第 4 层 · 技能索引（编号 / 能力 / 何时用）

> 只登记机器能力，步骤与失败回退在各文件里；禁止现编流程，先查此表。

| # | 能力 | 何时用 |
|---|---|---|
| 01 | 会话自检 `scripts/preflight.ps1` | 任何任务第一步（分支/脏区/基线/active change 缺件） |
| 02 | 变更四件套模板 `openspec/templates/` | 中大型改动动手前 |
| 03 | 流程门禁 `scripts/verify-ai-process.sh` | 提 PR 前 |
| 04 | DDL 一致性守卫 `scripts/code-quality-guard.sh` | 每次动表前后 |
| 05 | 本地钩子 `scripts/install-hooks.sh` | 环境初始化 / 钩子缺失 |
| 06 | 并行工作区 `scripts/worktree.ps1` | 多任务并行开发 |
| 07 | 事故案例集 `docs/analysis/典型技术缺陷与优化案例集.md` | 修 bug 前先读 |
| 08 | 文档清理 Agent `docs/ai/10-cleanup-agent.md` | 文档过期/并库时 |

## 第 5 层 · 沟通原则

1. **大任务前先认知对齐**：读相关 change 四件套与本表索引到的规范，复述
   理解、确认后再动手——不许边猜边写。
2. **单次变更 ≤40%**（写上限；与上下文"读预算"是两个概念，勿混）：
   改太多你审不动，审不动等于没审——超了先拆任务。
3. **不确定就停下来问**：禁止自行假设后推进；假设是错误里最难被发现的一种。
4. **风险定级不能外包**：命中第 2 层红线、或新规矩想进本文件，必须人拍板；
   AI 的发现先写 `.harness/candidate/`（格式见其 README），确认前不许当事实引用。
5. **跨会话传承**：决策进 `openspec/changes/<id>/proposal.md` 或对应文档，
   不进聊天记录。
6. 任务开始时先读 `.harness/ROUTING.md`（三张路由表：任务→文档、路径→规则、需求→管道），按路由表确定加载项目
7. **上下文压缩**（压的是有效注意力、非存储空间——不是因为装不下）：引用不复述（说"见 `.harness/wiki/数据模型.md 第 3 节`"，不抄进对话）；
   摘要非全文（贴关键片段和函数签名，不贴整文件）；分段非一次（大任务拆小步，每步做完清一次上下文）；交棒给文件（长会话状态写 `openspec/changes/<id>/progress.md`，下会话读文件接着干）。
