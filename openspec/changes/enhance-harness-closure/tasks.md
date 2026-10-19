# Tasks: enhance-harness-closure

## 准备

- [x] 阅读 `openspec/project.md`
- [x] 阅读 `AGENTS.md`、`.codex/harness/README.md`、`docs/ai/03-codex-harness.md`
- [x] 阅读 `docs/architecture/module-dependency-rules.md` §2 §4 §6
- [x] proposal/design 自审通过（本会话）

## 实现

### ArchUnit 落地

- [x] `LayeredArchitectureTest.java`：`层间依赖方向`、`controller_不得直接使用_mapper` 套 `FreezingArchRule.freeze(...)`
- [x] 新建 `backstage-admin/archunit-store/.gitkeep` 与 `README.md`（占位 + 生成/更新指引）
- [ ] 本地首跑 `mvn -B test -pl backstage-admin -am -Dtest='LayeredArchitectureTest,CodingConventionArchitectureTest'` 生成实际基线文件
- [ ] `git add backstage-admin/archunit-store/` 提交基线
- [ ] `git add backstage-admin/src/test/java/com/backstage/architecture/` 提交两个测试文件（当前工作区已有但未入索引）

### CI 断言

- [x] `.github/workflows/pr-check.yml`：在 ArchUnit 步骤前新增 `Assert ArchUnit test files present`

### MCP 凭据外置

- [x] 新建 `mcp.json.example`（占位字段，不含真实凭据）
- [x] `.gitignore` 追加 `mcp.json`
- [x] `bin/ensure_zentao_mcp_token.ps1`：`Test-Path` 失败时从 `mcp.json.example` 自愈
- [ ] 维护者手动执行 `git rm --cached mcp.json && git commit -m "chore(harness): untrack mcp.json (externalize token)"`（本 change 不代执行）
- [ ] 若历史 commit 的 token 疑似泄漏：在禅道侧刷新口令使旧 token 失效

### 本地 hook 前移

- [x] `scripts/preflight.ps1`：新增 `[hooks]` 检查段
- [ ] 团队成员各自跑 `bash scripts/install-hooks.sh` 落地 `.git/hooks/pre-commit`

### 文档 / 导航

- [x] `AGENTS.md` 硬性规则 4 补"落地前置"段
- [x] `.codex/harness/README.md` 执行顺序第 6 步补"ArchUnit 是否真实运行"汇报要求

## 验证

- [ ] 本地 `mvn -B test -pl backstage-admin -am -Dtest='*ArchitectureTest'` 通过且 archunit-store 有内容
- [ ] 本地 `powershell -File scripts/preflight.ps1` 输出 `[hooks]` 段
- [ ] 删除 `mcp.json` 后 `bin/ensure_zentao_mcp_token.ps1` 能从 example 自愈
- [ ] PR 打开后 CI `Assert ArchUnit test files present` 步骤运行
- [ ] ArchUnit 步骤真实出现在 build job 日志，非 `No tests to run`

## PR

- [ ] PR 标题：`chore(harness): close ArchUnit/MCP/hook gaps with freezing baseline`
- [ ] PR 描述关联本 change（enhance-harness-closure）
- [ ] 填写测试结果：CI 首红→提交基线→转绿的时间线
- [ ] 风险与回滚：仅 revert；无 schema/接口
- [ ] Reviewer：@heroyang
