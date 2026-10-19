# Proposal: enhance-harness-closure

## 背景

上一轮 Harness 达标核查（本会话）发现：AGENTS.md 声明的"六项 CI 阻断门禁"在**边界与验收两层存在纸面执行分离**——

- `backstage-admin/src/test/java/com/backstage/architecture/LayeredArchitectureTest.java` 与 `CodingConventionArchitectureTest.java` 起草完毕但**未 `git add`**，`git ls-files` 返回空。CI 里的 ArchUnit 步骤因 `-Dsurefire.failIfNoSpecifiedTests=false` 静默跳过，永远绿。
- `backstage-admin/archunit-store/` FreezingArchRule 基线目录不存在。即使测试文件补提交，首次 CI 会现建基线，把当时的所有存量违规"洗白"入档，冻结失效。
- `LayeredArchitectureTest` 4 条规则均为普通 `ArchRule`，实测仓内 `Controller 直接 import Mapper` 存量违规 **12 处**（UserBlock/FriendLink/BehaviorStats/CoursePay/OshOpenProject/UserOrder/OshQAQuestion/SeckillItemEsIndex/OshGroupServer/AdminInvite/UserBlock/UserInvitation/UserManage），补提交即刻打红 CI。
- `mcp.json` 已被 git 追踪且包含明文凭据字段，AGENTS.md 规则 5（gitleaks）对已入仓文件不生效。
- `scripts/install-hooks.sh` 存在但 opt-in，`.git/hooks/pre-commit` 未落地；`preflight.ps1` 不检查 hook 状态，"保护域拦截前移到本地 commit"从未闭环。

## 目标

- 让"CI 阻断"从纸面声明变为机器可验证的事实：ArchUnit 测试文件与其基线目录一起入 git，pr-check 增加"测试文件存在性断言"步骤；缺失即红，杜绝静默跳过。
- 对存量违规采用"零违规强制 + 存量冻结分级"策略（对齐项目既有决策记忆）：`LayeredArchitectureTest` 的两条会被 12 Controller 存量打红的规则改套 `FreezingArchRule`，保留零违规规则为普通 `ArchRule`。
- MCP 凭据外置：`mcp.json` 加入 `.gitignore`，新增 `mcp.json.example`，`ensure_zentao_mcp_token.ps1` 支持从 example 自愈生成。
- preflight 与 hook 融合：本地会话起点自检自动报告 hook 安装状态并给出一键安装指引，把保护域拦截前移到 commit 前。
- AGENTS.md 硬性规则 4 补充两条硬约束："测试文件必须入 git"与"基线目录必须入 git"，让下一个 Agent 读到即知。

## 非目标

- 不改动 12 个 Controller 的存量违规（属跨域 Mapper 直连清理，另立 change）。
- 不引入 Checkstyle/SpotBugs/JaCoCo 新构建插件。
- 不重跑 `mvn test` 生成真实基线（基线在首次 CI/本地跑通时由 FreezingArchRule 自动生成，本 change 只提供占位目录与 README 说明）。
- 不删除 `docs/**` 冗余文档（属"上下文层"治理，另行推进）。

## 用户影响

- 内部开发者：首次本地 clone 后需跑 `bash scripts/install-hooks.sh` 与 `copy mcp.json.example mcp.json`；`preflight.ps1` 会给出提示。
- CI：ArchUnit 步骤行为从"永远绿"变为"缺件即红、违规即红"。存量 12 Controller 由基线吸收，不会立刻打红；从此新增 Controller 直连 Mapper 会被拦下。
- AI Agent：AGENTS.md 与 `.codex/harness/README.md` 补充"必须 git add 测试与基线"提示，避免下一个会话重犯。
- 不影响任何业务接口、数据、权限。

## 风险

- **兼容性风险**：mcp.json 已入 git 索引，`.gitignore` 生效需要维护者手动 `git rm --cached mcp.json`；本 change 只在 tasks 列该人工动作，不在文件里执行破坏性 git 命令。
- **数据风险**：无。
- **性能风险**：ArchUnit 步骤在 CI 中真实执行，构建时长 +2~3 min；`-am` 会编译依赖链，与既有构建步骤复用 JDK 缓存。
- **安全风险**：迁 mcp.json 出仓后，历史 commit 中该文件的 token 仍在 git 历史；gitleaks 只扫 PR 新增 commit，历史 token 需人工在禅道侧作废（本 change 不主动执行）。
- **回滚风险**：本 change 仅新增/编辑文件；回滚 = revert PR 即可，无 schema/接口变更。

## 验收标准

- [ ] `git ls-files backstage-admin/src/test/java/com/backstage/architecture/` 返回两个测试文件；`git ls-files backstage-admin/archunit-store/` 返回 README.md 与 .gitkeep。
- [ ] `LayeredArchitectureTest` 中 `层间依赖方向` 与 `controller_不得直接使用_mapper` 均套 `FreezingArchRule.freeze(...)`；`mapper_不得依赖_controller_或_service` 与 `common_不得反向依赖上层模块` 保持普通 `ArchRule`。
- [ ] `.github/workflows/pr-check.yml` 存在独立步骤 `Assert ArchUnit test files present`，任一测试文件缺失即 fail。
- [ ] `.gitignore` 新增 `mcp.json`；`mcp.json.example` 存在于仓库根且不含真实凭据；`ensure_zentao_mcp_token.ps1` 在 mcp.json 缺失时从 example 生成。
- [ ] `scripts/preflight.ps1` 输出 `[hooks]` 段：缺失 hook 时给出 Yellow 提示 + 安装命令。
- [ ] `AGENTS.md` 硬性规则 4 增补"测试文件与 archunit-store 基线目录必须入 git"；`.codex/harness/README.md` 执行顺序第 6 步补"汇报里必须包含 ArchUnit 是否运行"。

## 关联信息

- 禅道任务：无（内部 Harness 治理）
- PR：待建
- 设计稿：docs/architecture/module-dependency-rules.md §2 §4 §6，docs/ai/03-codex-harness.md
- 线上日志：无
