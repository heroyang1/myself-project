# Proposal: close-r4-gate-gap

## 背景

`CLAUDE.md` 第 2 层红线 R4「依赖方向红线」有四条款，其中：

- **R4①②（分层单向 / 禁反向三形态）**：已由 `backstage-admin` 的 `LayeredArchitectureTest`
  以 FreezingArchRule 上线（见 change `enhance-harness-closure`），有 CI 抓手。
- **R4③（跨域取数只走对方 Service，禁注入他域 Mapper）**：宪法检查列自认"**暂无机器抓手**"，
  靠评审拦截。实测 `Service→跨域 Mapper import ≥207 条`（`docs/architecture/module-dependency-rules.md` §4.2）。
- **R4④（禁经 SpringUtil/static 工具类运行时取 Bean）**：宪法给出的软抓手是
  `grep SpringUtil\.getBean 在 src/main 须 0 命中`。

本轮治理审计（对照 `.harness/../docs/harness/Harness工程方法论_学习笔记.md` 验收清单）实测发现两处缺口，
需要补齐或如实修正，否则红线区存在"声明与抓手不符"的**假合规**：

1. **R4④ 的 grep 抓手写错了类名**。本仓库真实的静态 Bean 持有类是
   `com.backstage.common.utils.spring.SpringUtils`（复数 s），**不存在**名为 `SpringUtil` 的类。
   文档正则 `SpringUtil\.getBean` 匹配不到任何真实调用（真实形态是 `SpringUtils.getBean`），
   因此它恒返回 0 命中——**看着合规，实则空转**。实测 `src/main` 下 `SpringUtils.getBean` 调用点约 9 处：
   `common/utils/DictUtils`、`common/utils/MessageUtils`、`framework/config/DruidConfig`、
   `framework/manager/AsyncManager`、`framework/manager/factory/AsyncFactory`、
   `quartz/util/ScheduleUtils`、`quartz/util/JobInvokeUtil`、`quartz/util/AbstractQuartzJob`、
   `system/utils/ResourcePermissionUtil`。
2. **R4③ 无法在当前包树上被 ArchUnit 可靠表达**。实测 `system` 模块包结构为"技术层优先 + 二级域切分"，
   但域轴不完整且命名漂移：`system.mapper` 下既有 39 个域子包，又有 **16 个扁平 Mapper（无域归属）**；
   `system.controller` 同时存在 `flashsale` 与 `seckill`、`internal` 与 `internalmember`
   （同一逻辑域裂成两个包名）；`system.service` 混有扁平 `impl` 包。按域写 ArchUnit 规则会**漏判扁平 Mapper、
   误判双名域**，其结构性根源即方法论 §3.2 所述"层优先布局 = 跨域直连 Mapper 积 207 条的根源"。

## 目标

- **修正 R4④ 假合规**：把宪法红线 R4④ 的 grep 抓手更正为匹配真实类名 `SpringUtils`，
  并将其从"软 grep 抓手"升级为 **ArchUnit 机器门禁**（`CodingConventionArchitectureTest` 增一条规则），
  按仓库既有"零违规强制 + 存量冻结分级"策略：现存 9 处入 FreezingArchRule 基线只拦新增。
- **如实为 R4③ 定级与拆分**：本 change **不**假装能用 ArchUnit 干净拦截 R4③；而是
  （a）在宪法检查列把 R4③ 的抓手由"无"如实登记为"207 条 import 台账 + 评审 + `module-dependency-rules §4.2`"，
  （b）把"按域纵切包规整化"确立为 R4③ 可机器化的**前置 change**，在其 specs 内留占位要求与解锁条件。

## 非目标

- 本 change **不改任何业务代码**、不做包结构重构、不清理 207 条跨域 import 存量。
- 本 change **不**新增/晋升宪法第 2 层新红线编号（R10/R11 属另议的"三解法晋升"决策，不在本范围）。
- 本 change **不**追求 R4④ 一次性零存量硬门禁（9 处 infra 调用多为 RuoYi 框架装配，强行硬拦会砸构建）。

## 用户影响

- 纯工程治理/门禁改动，**不影响任何线上接口、页面、数据、权限、缓存**。
- 影响开发者：新增 `SpringUtils.getBean` 的业务代码会在 CI `ArchUnit` 步骤被判新增违例（存量 9 处不受影响）。

## 风险

- 兼容性风险：无 API 变更。
- 数据风险：无。
- 性能风险：ArchUnit 扫描在既有 test 作用域内运行，构建时长增量可忽略（复用 pr-check build job）。
- 安全风险：无（不涉及凭据）。
- 回滚风险：低。新增 ArchRule 为纯测试代码；FreezingArchRule 基线为数据文件。
  回滚 = 撤规则 + 撤基线，不触生产。
- **门禁静默失效风险**（须防）：基线目录 `backstage-admin/archunit-store/` 必须提交入 git 索引，
  否则 CI `Assert ArchUnit test files present` 步骤会 fail（沿用 `enhance-harness-closure` 已建立的断言）。

## 验收标准

- [ ] `CLAUDE.md` R4④ 检查列的 grep 更正为 `SpringUtils\.getBean`（真实类名），并如实标注"存量冻结"。
- [ ] `CLAUDE.md` R4③ 检查列由"暂无机器抓手"如实更新为"import 台账 + 评审；ArchUnit 化待前置包规整化 change"。
- [ ] `CodingConventionArchitectureTest` 新增规则：非测试类调用 `SpringUtils.getBean(...)` 即违规，套 `FreezingArchRule`。
- [ ] 本地 `mvn test -pl backstage-admin -Dtest=CodingConventionArchitectureTest` 通过（9 处存量入基线）。
- [ ] `backstage-admin/archunit-store/` 内对应 `.properties` 基线生成并提交入 git 索引。
- [ ] 新增 `SpringUtils.getBean` 调用能被 CI 判为新增违例（可用一次性临时类自证后删除）。

## 关联信息

- 禅道任务：（待挂）
- PR：（待建）
- 设计稿：见同目录 `design.md`
- 关联既有 change：`enhance-harness-closure`（ArchUnit 分层/冻结机制、CI 存在性断言由此建立，本 change 复用其基础设施）
- 上游事实源：`docs/architecture/module-dependency-rules.md` §4.2（跨域禁令与 207 条实测）、
  `CLAUDE.md` R4、`.harness/wiki/领域术语.md` §2（flashsale/seckill 双名漂移登记）
