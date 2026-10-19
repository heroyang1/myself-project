# Spec: harness-gates

## ADDED Requirements

### Requirement: ArchUnit 护栏文件与基线目录必须入 git 索引

系统 SHALL 保证 `backstage-admin/src/test/java/com/backstage/architecture/*.java` 与 `backstage-admin/archunit-store/` 均被 git 追踪，CI 在跑 ArchUnit 前必须显式断言二者存在，任一缺失即 fail；`-Dsurefire.failIfNoSpecifiedTests=false` 的容错不得掩盖"测试类失踪"的场景。

#### Scenario: 测试文件被误删

- GIVEN 开发者在本地删除或从未 `git add` `LayeredArchitectureTest.java`
- WHEN PR 触发 pr-check.yml build job
- THEN `Assert ArchUnit test files present` 步骤以 exit 1 fail，PR 阻断
- AND 错误信息含文件路径与"禁止静默跳过"提示

#### Scenario: 冻结基线未提交

- GIVEN 测试文件已入索引但 `backstage-admin/archunit-store/` 目录不存在
- WHEN CI 运行到断言步骤
- THEN 步骤 fail 并提示"本地跑 mvn test 生成基线后 git add 提交"

### Requirement: 存量架构违规采用 FreezingArchRule 冻结、只拦新增

系统 SHALL 对存在存量违规的 ArchUnit 规则套用 `FreezingArchRule.freeze(...)`，首次运行时把当时全部违规写入基线；后续任何新增违规使 CI 变红；存量清完后 SHALL 把规则升级回普通 `ArchRule` 并删除基线条目。

#### Scenario: 第 13 个 Controller 引入直连 Mapper

- GIVEN `controller_不得直接使用_mapper` 已套 FreezingArchRule，基线含 12 处存量
- WHEN 开发者新增一个 Controller 直接 `@Resource XxxMapper`
- THEN 该文件不在基线内，ArchUnit 步骤 fail，输出违规清单仅含新类

#### Scenario: 存量清理完毕

- GIVEN 12 处 Controller 违规已改完、Service 层跨域 Mapper 引用已消
- WHEN 维护者删除 `archunit-store/` 内相应文件并把规则改回普通 `ArchRule`
- THEN 后续 CI 用严格断言，任何违规即刻 fail

## MODIFIED Requirements

### Requirement: MCP 凭据不入 git 索引

系统 SHALL 提供 `mcp.json.example` 占位文件、将 `mcp.json` 列入 `.gitignore`；`bin/ensure_zentao_mcp_token.ps1` SHALL 在本地 `mcp.json` 缺失时从 example 自愈复制，避免首次 clone 用户跳过 token guard。

#### Scenario: 新成员首次 clone

- GIVEN 新成员 clone 仓库，工作区无 `mcp.json`
- WHEN 直接跑 `bin/ensure_zentao_mcp_token.ps1 -ConfigPath .\mcp.json`
- THEN 脚本从 `mcp.json.example` 生成 `mcp.json` 后进入正常 token 校验/刷新流程
- AND 不会因 `MCP config not found` 抛错退出

### Requirement: 会话起点自检覆盖本地 hook 状态

`scripts/preflight.ps1` SHALL 在输出中报告 `.git/hooks/pre-commit` 是否安装；缺失时以 Yellow 提示给出 `bash scripts/install-hooks.sh` 一键修复命令。

#### Scenario: 开发者未装 hook

- GIVEN 本地 clone 后未运行 install-hooks.sh
- WHEN 跑 preflight.ps1
- THEN `[hooks]` 段以 Yellow 输出"未安装"与安装命令

## REMOVED Requirements

无。
