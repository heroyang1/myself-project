# Spec: dependency-gates

> 关联 change：`close-r4-gate-gap`。本能力覆盖宪法 `CLAUDE.md` 红线 R4③/④ 的"抓手与声明一致"。
> 关联既有能力基线：`enhance-harness-closure`（R4①② 分层冻结门禁），本 change 为增量。

## ADDED Requirements

### Requirement: 静态取 Bean 须经 ArchUnit 冻结门禁强制（R4④）

系统 SHALL 以 ArchUnit `CodingConventionArchitectureTest` 拦截"业务代码运行时经
`com.backstage.common.utils.spring.SpringUtils.getBean(...)` 取 Bean"的行为，
并按"零违规强制 + 存量冻结分级"策略：现存调用点入 FreezingArchRule 基线只拦新增。
`CLAUDE.md` R4④ 检查列 MUST 引用真实类名 `SpringUtils`（非 `SpringUtil`），使声明与抓手一致。

#### Scenario: 新增代码调用 SpringUtils.getBean 被拦截

- GIVEN CI 已提交 `archunit-store/` 冻结基线（含现存 9 处 infra 调用）
- WHEN 一次 PR 在业务包新增 `SpringUtils.getBean(XxxService.class)` 调用
- THEN `mvn test -pl backstage-admin -Dtest=CodingConventionArchitectureTest` 判为新增违例并 fail，PR 被拦截

#### Scenario: 存量 infra 调用不因规则上线而砸构建

- GIVEN 规则首次启用、现存 `SpringUtils.getBean` 调用点位于 framework/quartz/common/system utils
- WHEN 本地/CI 运行该 ArchUnit 测试
- THEN 首跑 `allowStoreCreation=true` 生成基线并放行全部存量，测试为绿（冻结语义：只拦新增，非未拦截）

#### Scenario: 宪法声明与实跑抓手一致（消除假合规）

- GIVEN 审计发现 `CLAUDE.md` R4④ 原文 grep 为 `SpringUtil\.getBean` 且恒 0 命中
- WHEN 本 change 落盘后复核 R4④ 检查列
- THEN 检查列指向真实类名 `SpringUtils.getBean`，且声明的强制方式为"ArchUnit 冻结门禁"，与 CI 实跑一致

### Requirement: 跨域 Mapper 禁令如实定级并登记机器化前置条件（R4③）

系统 SHALL 在 `CLAUDE.md` R4③ 检查列如实登记其当前强制等级为"import 台账 + 评审拦截"，
MUST NOT 将"无稳定域包锚点"的跨域禁令伪装为已被 ArchUnit 强制；
并 SHALL 记录将其机器化的解锁条件（前置"按域纵切包规整化"）。

#### Scenario: 检查列诚实标注非自动拦截

- GIVEN 当前 `system.mapper` 含 16 个扁平 Mapper、controller 存在 `flashsale`/`seckill` 双名域
- WHEN 查看 `CLAUDE.md` R4③ 检查列
- THEN 其强制等级写为"评审拦截 + ≥207 条 import 台账"，并指向 `docs/architecture/module-dependency-rules.md` §4.2，
  不声称由 CI/ArchUnit 拦截

#### Scenario: 包规整化完成后方可升级为硬门禁

- GIVEN 前置 change `regularize-domain-package-layout` 使每域有唯一、稳定的 service/mapper 包锚点
- WHEN 该前置完成并入基线
- THEN 允许另立 change 以 ArchUnit `noClasses().that().resideInAPackage("..service.<域A>..").should().dependOnClassesThat().resideInAPackage("..mapper.<域B>..")`
  （选择器方法名用 `resideInAPackage`/`resideInAnyPackage`）把 R4③ 升级为冻结门禁；在此之前 R4③ 维持评审级

## MODIFIED Requirements

### Requirement: 红线区 R4 检查列表述（继承自 enhance-harness-closure 基线）

系统 SHALL 保持 R4①② 由 `LayeredArchitectureTest` 冻结强制不变，
仅修订 R4③④ 两列文本，使四条依赖红线的"规则—抓手—强制等级"三者对齐、无假合规。

#### Scenario: 修订不影响既有分层门禁

- GIVEN `LayeredArchitectureTest` 的 `层间依赖方向`/`controller_不得直接使用_mapper` 已冻结上线
- WHEN 本 change 仅改 R4③④ 检查列文本并新增 `SpringUtils` 取 Bean 规则
- THEN 既有 R4①② 规则与基线不变、仍绿，修订不引入双源（R4 正文唯一住所仍是 `CLAUDE.md` 第 2 层）

## REMOVED Requirements

无。本 change 不移除任何既有要求。
