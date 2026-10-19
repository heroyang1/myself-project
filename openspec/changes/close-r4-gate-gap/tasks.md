# Tasks: close-r4-gate-gap

> 本清单为**实现阶段**待办。契约（proposal/design/spec）先行经人拍板后才动手；
> 其中改 `CLAUDE.md` 红线区、加 CI 门禁规则属"影响行为/架构边界"，须随本 change 四件套一并提交。

## 准备

- [ ] 阅读 `openspec/project.md`
- [ ] 阅读 `docs/architecture/module-dependency-rules.md` §4.2、`docs/architecture/package-structure.md`
- [ ] 阅读 `CLAUDE.md` R4 四条款与第 2 层缺口说明段
- [ ] 确认 proposal/design 已通过评审；**决策点 A/B/C 由人拍板**（冻结基线 / infra 白名单 / 仅软 grep）

## 实现（R4④ · 升级硬门禁）

- [ ] 改 `CLAUDE.md` R4④ 检查列：grep 正则 `SpringUtil\.getBean` → 真实类名 `SpringUtils\.getBean`，
      并把"须 0 命中"更新为"由 ArchUnit `CodingConventionArchitectureTest` 冻结强制（存量 9 处入基线，只拦新增）"
- [ ] `CodingConventionArchitectureTest` 增 `禁止经SpringUtils运行时取Bean` 规则：
      `noClasses().should().callMethod(SpringUtils.class,"getBean",String.class).orShould().callMethod(...,Class.class)`
      套 `FreezingArchRule.freeze(...)`；失败信息用 [违规]/[修复]/[参考] 三要素
- [ ] 核对 ArchUnit 1.x API：`callMethod` 显式重载可编译；若用选择器注意 `resideInAPackage`（带 A）
- [x] 本地首跑生成基线：`C:\Users\Admin1\tools\apache-maven-3.9.6\bin\mvn.cmd -pl backstage-admin test "-Dtest=CodingConventionArchitectureTest"`（JDK21 即可，见下实况）

## 实现（R4③ · 如实定级 + 立前置）

- [ ] 改 `CLAUDE.md` R4③ 检查列：由"暂无机器抓手"如实改为
      "import 台账（≥207 条）+ 评审拦截；ArchUnit 化待前置『按域纵切包规整化』change 解锁"
- [ ] 在 `docs/plans/current-sprint.md` 或待办登记前置 change 意图：`regularize-domain-package-layout`（占位，未开工）

## 验证

- [x] 本地全绿：8/8 `BUILD SUCCESS`（Layered 4 + CodingConvention 4），命令见文末实况段
- [x] 自证新增拦截（2026-10-19 已执行）：临时类 `com.backstage.architecture.probe.TempGateProbe` 调 `getBean(String)`/`getBean(Class)` 两重载 → `mvn -pl backstage-admin clean test` 判 **BUILD FAILURE，was violated (2 times)**（仅 2 处新增，非 14+2）；删探针复跑 → **BUILD SUCCESS**、基线仍 14 未污染。证明 R4④ 冻结门禁"存量放行/新增必拦"真生效。
- [ ] `git add` 测试文件与 `backstage-admin/archunit-store/` 新基线 `.properties`（缺一 CI 静默失效）
- [ ] 跑 `powershell -File scripts/preflight.ps1` 确认四件套齐全、分支/脏区正常

## PR

- [ ] PR 标题满足 Conventional Commits：`feat(arch): gate SpringUtils.getBean (R4④) and honestly classify R4③`
- [ ] PR 描述关联本 change `close-r4-gate-gap`
- [ ] 填写测试结果（首跑绿 + 新增自证 fail）与回滚说明
- [ ] 指派 Reviewer（红线区文本改动须 @heroyang 拍板）
- [ ] 触碰行为不申请豁免（本 change 携四件套满足 R3）

## 落地收尾实况（2026-10-19 · 环境已打通）

### 已完成（本会话，物理验证）
- [x] 环境：`mvn` 不在 PATH，实测用 `C:\Users\Admin1\tools\apache-maven-3.9.6\bin\mvn.cmd` + JDK21（`JAVA_HOME=Amazon Corretto 21`）即可 `install -DskipTests` **BUILD SUCCESS**，无需另装 JDK17（`java.version=1.8` 在 21 下仅告警、产物 v52；Lombok 1.18.32 支持 21）。
- [x] R4④：`CodingConventionArchitectureTest` 新增 `禁止经SpringUtils运行时取Bean`（FreezingArchRule），4/4 绿；存量 9 处入基线 `bddffb30…`。
- [x] **第二处假合规（本次新发现并修）**：`LayeredArchitectureTest.mapper_不得依赖_controller_或_service` 原注释断言"实测零违规"从未被 CI 验证，实跑 **53 处**（`system.mapper.book.BookEsMapper`/`BookMapper`/`system.mapper.tool.OshToolEsMapper` 依赖**放错包**的 `controller.book.BookListReqVO`、`service.tool.ToolIndexMessage`）。按本文件既有模式改套 `FreezingArchRule`，53 处入基线 `22c98b60…` 只拦新增；根因是 VO/Message 归位，须另立 change 迁到 domain/dto 后再升回硬断言。
- [x] 合跑 `8/8 BUILD SUCCESS`；`backstage-admin/archunit-store/` 生成 6 条基线 + `stored.rules`，`git check-ignore` 确认未被忽略、可提交。

### 待你执行（push/PR 需你的远端凭据；git 写操作不代跑）

> 硬约束：`feature/course_hero` 领先 `origin/master` **207 commit**，工作区另混 **60+ 未提交业务文件**（backstage-system/sql/pom/framework…）。
> **严禁 `git add -A`/`git add .`**；只按下列精确路径分域提交，业务 WIP 保持原样浮动。
> 两 PR 均以 `feature/course_hero` 为 base（当前主力线）；若要并到 master/qa 须先 rebase，不在本步。

**PR#2 · R4 门禁**（测试+基线原属 enhance-harness-closure，随本 change 首次落地，PR 描述点名该依赖）：
```powershell
git switch -c feature/r4-arch-gate
git add CLAUDE.md `
        openspec/changes/close-r4-gate-gap `
        backstage-admin/src/test/java/com/backstage/architecture `
        backstage-admin/archunit-store
git commit -m "feat(arch): gate SpringUtils.getBean via ArchUnit freeze (R4-4); freeze 53 legacy mapper deps; honest-classify R4-3"
git push -u origin feature/r4-arch-gate
git switch feature/course_hero
```

**PR#1 · 文档/治理落地**（docs 重组 + .harness 知识库，纯文档无行为变更走豁免）：
```powershell
git switch -c feature/docs-governance
git add docs .harness `
        openspec/changes/enhance-harness-closure `
        openspec/changes/add-harness-wiki-knowledge-base
git commit -m "docs: reorganize docs into topical modules; land .harness wiki/routing/candidate; fix broken links"
git push -u origin feature/docs-governance
git switch feature/course_hero
```
> 回切 course_hero 时新支已提交的治理文件会从工作区消失（内容安全存于新支提交），业务未提交件不受影响——属隔离的预期行为，非丢失。
> 提交前自检：`git diff --cached --stat` 逐一确认**只含治理路径、无业务/保护域文件**。
> 落地后门禁闭环自证：临时业务类调用 `SpringUtils.getBean(...)` → 复跑两测试须 fail → 删除该类。
