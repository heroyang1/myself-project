# Design: enhance-harness-closure

## 方案概览

沿用项目既有的"零违规强制 + 存量冻结"策略，把 AGENTS.md 声明的 ArchUnit/gitleaks/hook 三道抓手补完最后一公里：**文件必须入索引、CI 必须断言存在、存量违规必须显式冻结、凭据必须外置**。全部改动限于工程治理与 CI 配置，无业务/接口/schema 变更。

## 现状分析

- **相关模块**：`backstage-admin`（ArchUnit 测试落位）、`.github/workflows`、`scripts/`、`.gitignore`、`bin/ensure_zentao_mcp_token.ps1`、`AGENTS.md`、`.codex/harness/README.md`。
- **相关类**：`LayeredArchitectureTest`（4 条规则）、`CodingConventionArchitectureTest`（3 条规则，已用 FreezingArchRule 冻结 println/裸 HTTP）。
- **存量违规实测**：Controller→Mapper 12 处；Service→跨域 Mapper ≥207 条 import；OshGroupOrder 双实体；IpUtils 双份。
- **配置**：`mcp.json` 含明文凭据字段已被 git 追踪；`backstage-admin/archunit-store/` 不存在。

## 详细设计

### CI 步骤改造（pr-check.yml）

在 `build` job 内、ArchUnit 执行前，新增独立"存在性断言"步骤：

```yaml
- name: Assert ArchUnit test files present
  run: |
    for f in \
      backstage-admin/src/test/java/com/backstage/architecture/LayeredArchitectureTest.java \
      backstage-admin/src/test/java/com/backstage/architecture/CodingConventionArchitectureTest.java; do
      if [ ! -f "$f" ]; then
        echo "::error file=$f::$f 缺失；ArchUnit 护栏失效，禁止静默跳过（failIfNoSpecifiedTests=false 的副作用）"
        exit 1
      fi
    done
    if [ ! -d backstage-admin/archunit-store ]; then
      echo "::error FreezingArchRule 基线目录 backstage-admin/archunit-store/ 缺失；" \
           "首次运行会现建基线把当时全部存量洗白。请本地跑 mvn test -Dtest='*ArchitectureTest' 生成后 git add 提交。"
      exit 1
    fi
```

**保留** `-Dsurefire.failIfNoSpecifiedTests=false`（多模块聚合下其他模块无匹配类不 fail 是必要容错），把"缺件即红"的责任前移到断言步骤，避免误伤。

### LayeredArchitectureTest 分级

按存量违规数量分档（对应记忆 c0b3648a 策略）：

| 规则 | 存量 | 处理 |
|---|---|---|
| `层间依赖方向` | Service 被非 Controller 访问（quartz 消费者等）与 Mapper 被 Controller 访问均违规 | 套 `FreezingArchRule.freeze(...)` |
| `controller_不得直接使用_mapper` | 12 Controller | 套 `FreezingArchRule.freeze(...)` |
| `mapper_不得依赖_controller_或_service` | 实测零 | 保持普通 `ArchRule` |
| `common_不得反向依赖上层模块` | 实测零 | 保持普通 `ArchRule` |

`consideringOnlyDependenciesInLayers()` 保留，Kafka 消费者/task 已豁免。基线目录 `backstage-admin/archunit-store/` 用 `.gitkeep` 与 README.md 双占位。

### MCP 凭据外置

- `.gitignore` 增 `mcp.json`（对未来新贡献者 clone 生效）。
- 新增 `mcp.json.example`：字段结构与现 mcp.json 对齐，`headers.token` 与 `Authorization` 留空字符串，`url`/`apiBaseUrl` 用 `<your-zentao-host>` 占位。
- `bin/ensure_zentao_mcp_token.ps1`：`Test-Path` 失败时先看 `mcp.json.example`，存在则 Copy 到 ConfigPath 继续刷新；不存在仍 throw 现错。
- **人工动作**（列在 tasks.md）：一次性 `git rm --cached mcp.json && git commit`。本 change 不执行破坏性 git 命令，避免误伤其他未提交工作区。
- **历史 token 处理**：写入 AGENTS.md 提醒"若历史泄漏疑存，先刷新禅道口令使旧 token 失效"。

### preflight hook 检查

`preflight.ps1` 在提醒段前追加：

```powershell
Write-Host '[hooks] 检测 pre-commit hook ...'
if (-not (Test-Path '.git/hooks/pre-commit')) {
    Write-Host '        未安装 —— 保护域拦截仍只在 CI 发生（滞后）。建议：bash scripts/install-hooks.sh' -ForegroundColor Yellow
} else {
    Write-Host '        已安装 .git/hooks/pre-commit'
}
```

### AGENTS.md 硬性规则 4 补充

在 ArchUnit 条目末尾追加"落地前置条件"段：

> **落地前置**：`backstage-admin/src/test/java/com/backstage/architecture/*.java` 与 `backstage-admin/archunit-store/` 必须均已入 git 索引；CI 的 `Assert ArchUnit test files present` 步骤会校验两者存在，缺失即 fail。存量违规用 `FreezingArchRule` 冻结、只拦新增；存量清完后须把对应规则升级回普通 `ArchRule` 并删基线目录。

### .codex/harness/README.md 执行顺序补

第 6 步"汇报结果和风险"补一句：必须报告 ArchUnit 是否真实运行、`git ls-files backstage-admin/archunit-store/` 是否非空。

## 兼容性

- 老接口/数据/前端：无影响。
- 已有开发者工作流：首次 clone 后需要 `copy mcp.json.example mcp.json` + 运行 token 脚本刷新 + 跑 `install-hooks.sh`；preflight 会提示。
- CI：ArchUnit 步骤第一次跑会生成基线；基线未提交前会 fail 该步骤——这**正是**目标（拒绝静默跳过）。开发者本地首次 `mvn test -Dtest='*ArchitectureTest'` 生成基线 → `git add backstage-admin/archunit-store/` → 提交后 CI 通过。

## 测试计划

- **单元**：无（改动全在治理/CI/脚本层）。
- **CI smoke**：本 change 提交后首次 CI 预期红一次（archunit-store 缺失），生成基线补提交后转绿；观察 pr-check build job 日志。
- **本地验证**：
  - `powershell -File scripts/preflight.ps1` 应输出 `[hooks]` 段。
  - `bash scripts/install-hooks.sh` 后 `.git/hooks/pre-commit` 存在。
  - 删除 `mcp.json` 跑 `bin/ensure_zentao_mcp_token.ps1` 应自动从 example 生成后进入正常刷新流程。
- **回归范围**：仅工程配置，业务代码零改动。

## 发布与回滚

- 发布：正常 PR，qa/** → main。
- 验证命令：
  - `git ls-files backstage-admin/archunit-store/`（非空）
  - `git ls-files backstage-admin/src/test/java/com/backstage/architecture/`（两个测试）
  - CI build job 日志出现"Architecture guardrail (ArchUnit)"步骤真实运行
- 回滚：revert PR；`LayeredArchitectureTest` 与 pr-check.yml 回到原状；mcp.json 保持工作区（未 untrack 前不影响）。
