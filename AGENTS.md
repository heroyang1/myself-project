---
title: Agent Instructions
path: AGENTS.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-10
openspec: none          # 关联的 change-id；无则 none
---

# Agent Instructions

AI agents 在本仓库的共享规则。本文件是策略源，`.github/copilot-instructions.md` 须与之对齐。

## 项目简介

`osh-backend`：OSH 平台后端，Spring Boot 2.5 单体多模块（主站/课程/订单/拼团/秒杀/工具/电子书/后台等），按业务边界演进到微服务。

## 代码地图（想做什么 → 去哪里看）

> 本表只做导航，具体规则以目标文档为单一事实源，**勿在本文复制正文**（避免双源漂移）。

| 想做什么 | 去看 |
|---|---|
| 了解当前迭代任务（进行中/待开始/已知问题） | `docs/plans/current-sprint.md` |
| 上手 / 工作流 / 分支规则 | `docs/ai/01-newcomer-guide.md`、`docs/ai/02-workflow.md` |
| 架构与分层（Controller→Service→Mapper，外部调用走 client 层） | `docs/architecture/boundaries.md`、`docs/ai/04-architecture.md` |
| 各 Maven 模块职责与依赖链、backstage-ui 定位 | `docs/architecture/overview.md` |
| 模块边界与依赖规则（包含矩阵/调用方向/数据权限/配置关系） | `docs/architecture/module-dependency-rules.md` |
| 关键包路径 / 新模块落位 / 分层命名规约 | `docs/architecture/package-structure.md` |
| 响应格式（AjaxResult/TableDataInfo/R、HTTP 恒 200、startPage）与前端对接（/dev-api、Bearer、/notify） | `docs/reference/api-spec.md` |
| 核心基类（BaseController/BaseEntity…）与自定义注解速查 | `docs/ai/conventions/backend.md` |
| 异常体系与错误码 | `docs/reference/error-codes.md`、`docs/ai/conventions/logging.md` |
| 数据库表约定（osh_/sys_ 前缀、必备字段、delete_flag） | `docs/ai/conventions/database.md` |
| 配置文件位置（Nacos/profile/logback/前端 .env） | `docs/architecture/module-dependency-rules.md` §8 |
| 编码规范（命名/Controller/Service/Mapper/实体/响应/异常/注解/SQL 含真实示例，AI 新增代码必读） | `docs/ai/conventions/coding-standards.md` |
| 编码风格 / 日志 | `docs/ai/05-code-style.md`、`docs/ai/conventions/logging.md` |
| 测试与验证要求 | `docs/ai/06-testing.md` |
| Codex/Harness 开发前清单与禁止事项 | `docs/ai/03-codex-harness.md`、`.codex/harness/README.md` |
| 会话起点自检（分支/脏区/change） | `powershell -File scripts/preflight.ps1` |
| 代码审核标准 | `docs/ai/07-review-rubric.md`、`CODE_REVIEW.md` |
| 中间件 / API / 安全约定 | `docs/ai/conventions/<topic>.md` |
| 开始中大型改动前 | 建 `openspec/changes/<change-id>/` 四件套 |

## 技术栈基线（不允许擅自升级；升级须先建 OpenSpec change）

- Java 8 运行语义（禁 `var` / `record` / text blocks），CI 用 JDK 17 构建。
- Spring Boot 2.5.15（不可升 3.x）+ Spring Cloud 2020.0.6 / Alibaba 2021.1。
- MyBatis-Plus 3.5.3.1 + PageHelper + Druid + MySQL；无 Flyway，DDL 走 `sql/` 脚本。
- Redis / Redisson、Elasticsearch、Kafka、Nacos、Cloudflare R2/S3、XXL-JOB。

## 硬性规则（以下 CI 会强制拦截，不是建议）

1. **OpenSpec 门禁**（`scripts/verify-ai-process.sh`）：中大型改动，或影响行为、接口、权限、数据、
   中间件、部署、存储的改动，PR 必须含 `openspec/changes/<change-id>/`，且 `proposal.md`、
   `design.md`、`tasks.md`、`specs/**/spec.md` 齐全非空，否则失败。豁免须在 PR 正文写
   `OpenSpec-Exempt: true` 且 `Reason: <理由>`。**保护域不可豁免**：触碰 `pom.xml`、`sql/`、
   `application*.yml|properties` 的 PR 必须携带 change。归档区 `openspec/changes/archive/` 只读，
   修改/删除已归档内容会被拦截（修正历史须新建 change）。
2. **DDL↔代码一致**（`scripts/code-quality-guard.sh`）：改表须同步实体/VO；必填字段四层一致、
   列类型 Java↔SQL 匹配、索引无幽灵列、ALTER 与初始 DDL 同步。
3. **构建与核心测试**（`pr-check.yml` build job）：`mvn clean install -DskipTests -pl '!backstage-flink'`
   （JDK 17）须成功，核心测试套件须运行。
4. **架构护栏**（`pr-check.yml` build job → ArchUnit）：`LayeredArchitectureTest`（分层方向
   Controller→Service→Mapper、common 不反向依赖）与 `CodingConventionArchitectureTest`
   （禁 `e.printStackTrace()`；`System.out.println`/裸 HTTP 经 FreezingArchRule 只拦新增）须通过。
   **落地前置**：`backstage-admin/src/test/java/com/backstage/architecture/*.java` 与冻结基线目录
   `backstage-admin/archunit-store/` 均须已入 git 索引；CI 会先跑 `Assert ArchUnit test files present` 断言，
   任一缺失即 fail（拒绝因 `failIfNoSpecifiedTests=false` 导致“测试失踪→步骤绿灯”的静默跳过）。
   存量违规由 `FreezingArchRule` 入档只拦新增；存量清完后须将对应规则拆下 freeze 升回普通
   `ArchRule` 并删基线目录内对应 `.properties`。详见 `openspec/changes/enhance-harness-closure/`。
5. **密钥扫描**（`pr-check.yml` → gitleaks）：PR 引入的 commit 含 token/密码/私钥即失败。
   本地凭据约定：`mcp.json` 已列入 `.gitignore`，新成员首次 clone 后直接跑
   `bin/ensure_zentao_mcp_token.ps1` 会从 `mcp.json.example` 自愈生成，再交互式刷新 token。
   历史 commit 中旧 token 仍在 → 如怀疑泄漏先在禅道侧刷新口令，再评估是否重写历史。
6. **PR 标题**（`pr-check.yml`）：须满足 Conventional Commits `<type>(<scope>)?: <subject>`，CI 拦截。

> 非阻断告警（仅提示、不 fail）：文件 >300 行、`docs/design` 文档 >60 天未更新。

## 提交规范（Conventional Commits，团队约定）

`feat:` 新功能 · `fix:` 修复 · `refactor:` 重构 · `docs:` 文档 · `test:` 测试 · `chore:` 构建杂项。
建议带 scope，如 `feat(group): ...`。**PR 标题由 CI 强制校验（见硬性规则 6）**。改动只限当前任务范围，保留用户本地修改，不提交密钥/token/日志/构建产物（密钥入仓由 gitleaks 拦截）。

## ZenTao MCP token guard（强制）

用 `mcpServers.zentao`（见 `MCP.json`）前必须先跑；令牌失效先刷新再操作，不得跳过：

```powershell
powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json
```
