# Agent Instructions

本仓库 AI agents 的共享规则（策略源）。`CLAUDE.md`（项目宪法）、`.github/copilot-instructions.md` 须与之对齐。

## 仓库现状：个人维护模式

- osh-backend 原为团队协作仓库，团队已解散，现由个人在私有仓 `heroyang1/myself-project` 单独维护。
- 迁移采用「全量保留历史 + 仅切远程」方案，未做历史重写。
- **本仓库必须保持 Private**：旧提交历史中的 `mcp.json` 含禅道 token；如需转 Public，必须先 `git filter-repo` 清洗历史并吊销该 token。

## 已退役的团队机制（个人仓不再强制，仅作历史参考）

以下原为团队协作治理，迁移后已从 CI（`pr-check.yml`）移除，不再是门禁：

- **SDD / OpenSpec 四件套强制**：`scripts/verify-ai-process.sh` 不再由 PR 触发；中大型改动是否建 change 由个人自行判断。
- **禅道 MCP token guard**：依赖团队禅道实例，个人维护一般无需；脚本 `bin/ensure_zentao_mcp_token.ps1` 保留备用。
- **团队服务器部署**：`deploy-qa.yml` / `deploy-prod.yml` 已删除（其中硬编码团队服务器 IP 与 Secrets，个人仓不可用）。

## 个人开发工作原则（保留的良好习惯）

- 改动前先读相关文档与目标代码，理解后再动手；不做无关重构。
- 保留用户本地修改，除非明确要求，绝不丢弃。
- 编辑范围只限当前任务。
- 完成后运行相关验证并如实汇报结果。
- 提交遵循 Conventional Commits（`feat` / `fix` / `refactor` / `docs` / `test` / `chore` 等，建议带 scope）。

## 个人仓仍生效的 CI 门禁（`pr-check.yml`，PR 时触发）

1. **密钥扫描**：gitleaks（个人账户无需 `GITLEAKS_LICENSE`）。
2. **架构护栏**：ArchUnit `LayeredArchitectureTest` + `CodingConventionArchitectureTest`，含「测试文件存在性断言」步骤防静默跳过。
3. **构建 + 核心测试**：JDK 17 `mvn clean install -DskipTests -pl '!backstage-flink'` + 指定核心测试套件 + jar 存在性校验。
4. **PR 标题**：Conventional Commits 校验。

> 本地仍有 `code-quality-guard` pre-commit 钩子（改表 DDL/Entity/VO 四层一致检查）。因仓库存在历史遗留技术债（若干 `setNo()` 缺失、ALTER 与初始 DDL 不同步），提交被拦时，可在确认问题与本次改动无关后用 `git commit --no-verify` 跳过。

## 技术栈基线（不允许擅自升级）

Java 8 运行语义（禁 `var` / `record` / 文本块），CI 用 JDK 17 构建；Spring Boot 2.5.15（不可升 3.x）+ Spring Cloud 2020.0.6 / Alibaba 2021.1；MyBatis-Plus 3.5.3.1 + PageHelper + Druid + MySQL；无 Flyway，DDL 走 `sql/` 脚本；Redis / Redisson、Elasticsearch、Kafka、Nacos、Cloudflare R2/S3、XXL-JOB。

## 代码地图

模块职责、分层边界、编码规范、测试与审核标准等详见 `CLAUDE.md`（五层宪法）与 `docs/` 目录。
