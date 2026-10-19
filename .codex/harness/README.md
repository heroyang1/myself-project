# Codex Harness

本目录记录 Codex 在本仓库的固定执行约束。

## 固定入口

- 根规则：`AGENTS.md`
- 执行约束与开发前清单：`docs/ai/03-codex-harness.md`
- 新人教程：`docs/ai/01-newcomer-guide.md`
- 工作流：`docs/ai/02-workflow.md`
- OpenSpec 项目说明：`openspec/project.md`

## 执行顺序

0. 跑起点自检（只读，输出分支/脏区/active change/保护域/hook 安装状态）：
   `powershell -ExecutionPolicy Bypass -File .\scripts\preflight.ps1`
1. 识别当前任务和 change-id。
2. 读取 OpenSpec。
3. 制定计划。
4. 小步修改。
5. 运行验证。
6. 汇报结果和风险。**汇报必须包含**：
   - ArchUnit 护栏本次是否真实运行（`git ls-files backstage-admin/archunit-store/` 非空、且 CI build job 日志能看到 `Architecture guardrail (ArchUnit)` 步骤产出测试结果，而非 `No tests to run`）。
   - `.git/hooks/pre-commit` 是否已安装；若否，附 `bash scripts/install-hooks.sh` 一键指引。
   - 本地 `mcp.json` 是否已从 `mcp.json.example` 生成；若否，跑 `bin/ensure_zentao_mcp_token.ps1` 自愈。
