# OSH Backend CI/CD 自动部署配置说明分析报告

> 生成时间：2026-09-27  
> 分析范围：`.github/`、`scripts/`、`bin/`、根目录脚本  
> 分析方法：基于代码库实际文件的静态审计

---

## 目录

1. [总览与结论](#1-总览与结论)
2. [GitHub Actions Workflows 分析](#2-github-actions-workflows-分析)
   - 2.1 PR 质量门禁 — pr-check.yml
   - 2.2 QA 自动部署 — deploy-qa.yml
   - 2.3 生产蓝绿部署 — deploy-prod.yml
   - 2.4 前端部署模板 — deploy-149.yml（非活跃）
   - 2.5 变量配置示例 — deploy-release.variables.example
3. [构建与部署脚本分析](#3-构建与部署脚本分析)
   - 3.1 CI 辅助脚本（scripts/）
   - 3.2 本地开发脚本（bin/ 与根目录）
4. [其他 CI 工具痕迹排查](#4-其他-ci-工具痕迹排查)
5. [CI/CD 分层架构总结](#5-cicd-分层架构总结)
6. [GitHub Secrets 与 Variables 完整清单](#6-github-secrets-与-variables-完整清单)
7. [硬编码值与可配置性分析](#7-硬编码值与可配置性分析)
8. [改进建议](#8-改进建议)

---

## 1. 总览与结论

**项目已具备完整的、分层级的 CI/CD 自动部署能力**，完全基于 **GitHub Actions** 驱动，不依赖任何第三方 CI 平台。

整体架构分为三层：

| 层级 | Workflow | 触发方式 | 目标环境 |
|------|----------|----------|----------|
| PR 质量门禁 | `pr-check.yml` | PR 发起到受保护分支（自动） | CI Runner（无部署） |
| QA 自动部署 | `deploy-qa.yml` | Push 到 `qa/**`（自动） | 25 测试机 |
| 生产蓝绿部署 | `deploy-prod.yml` | `workflow_dispatch` / `repository_dispatch`（手动审批） | 149 生产机 |

**分支流转模型**：`feature/*` → `qa/YYYYMMDD` → `release/YYYYMMDD` → `master`

---

## 2. GitHub Actions Workflows 分析

### 2.1 PR 质量门禁 — pr-check.yml

**文件路径**：`.github/workflows/pr-check.yml`（83 行）

#### 触发条件

```yaml
on:
  pull_request:
    branches:
      - 'qa/**'
      - 'release/**'
      - 'main'
      - 'master'
```

向 `qa/**`、`release/**`、`main`、`master` 发起 Pull Request 时**自动触发**。
`feature` 分支直接 push **不会**运行此门禁，强制校验发生在 PR 合并环节。

#### 并发控制

```yaml
concurrency:
  group: pr-check-${{ github.head_ref }}
  cancel-in-progress: true
```

按 PR 源分支分组，新 push 自动取消旧的运行——避免资源浪费。

#### Job 1: process-guard（Spec & Process Guard）

| 步骤 | 作用 |
|------|------|
| Checkout source | 拉取代码（`fetch-depth: 0`，完整历史用于 diff） |
| Detect OpenSpec exemption | 使用 `actions/github-script@v7` 正则检测 PR body 中的 `OpenSpec-Exempt: true` 和 `Reason:` |
| Verify AI process files | 调用 `scripts/verify-ai-process.sh`，校验 14 个必需文件存在且非空；若 PR 含 `openspec/changes/` 变更，强制要求 proposal/design/tasks/spec 齐全 |
| Code quality guard | 调用 `scripts/code-quality-guard.sh`，执行 6 项静态检查（详见 [3.1 节](#31-ci-辅助脚本scripts)） |

#### Job 2: build（Build & Verify）

| 步骤 | 作用 |
|------|------|
| Setup JDK | Temurin JDK 17 + Maven 缓存 |
| Run core test suites | 运行 7 个核心测试套件：`PaymentScenarioTest`、`GroupBuyingScenarioTest`、`SecurityBoundaryTest`、`BoundaryValueTest`、`MiddlewareFailoverTest`、`EquivalenceClassTest`、`ConcurrencySafetyTest` |
| Upload test reports | 上传 Surefire 报告（`always()` 条件下，即使测试失败也上传） |
| Build backend | `mvn clean install -DskipTests -pl '!backstage-flink'`（排除 Flink 模块） |
| Verify jar exists | `test -f backstage-admin/target/backstage-admin.jar` |

---

### 2.2 QA 自动部署 — deploy-qa.yml

**文件路径**：`.github/workflows/deploy-qa.yml`（178 行）

#### 触发条件

```yaml
on:
  push:
    branches:
      - 'qa/**'
  workflow_dispatch:
```

- **Push 触发**：代码推送到任意 `qa/**` 分支时自动运行。
- **手动触发**：支持 `workflow_dispatch`，可在 GitHub Actions 页面手动运行。

#### 并发控制

```yaml
concurrency:
  group: deploy-backend-release
  cancel-in-progress: false
```

全局单一部署组，`cancel-in-progress: false` 确保部署不会被中途取消（避免半部署状态）。

#### 智能分支过滤机制

此 workflow 包含一个**关键设计**——仅部署最新 qa 分支：

1. **Detect latest qa branch**：`git branch -r --list 'origin/qa/*'` + `sort -V | tail -n 1`，按版本号排序找到最新 qa 分支。
2. **Skip non-latest qa branch**：若当前 push 的分支不是最新 qa 分支，直接 `exit 0` 跳过。

这确保了只有最新的 qa 分支（如 `qa/20260708`）才会触发实际部署，旧分支的 push 不会误部署。

#### 部署步骤详解

```
Checkout → 检测最新 qa 分支 → [跳过非最新分支] →
Setup JDK 17 → Maven 构建 → jar 校验 + SHA256 →
SSH agent 建立 → known_hosts 添加（3 次重试） →
Docker Compose stop（停止后端，避免读取半上传的 jar）→
rsync 上传 jar（--inplace 增量传输）→
SHA256 双重校验（本地 vs 远程）→
Docker Compose up -d（重启后端）→
输出容器状态和日志 →
邮件通知（成功/失败）
```

#### 通知机制

使用 `dawidd6/action-send-mail@v3`，通过 SMTP（QQ 邮箱 `smtp.qq.com:465`）发送邮件：
- 成功邮件：包含仓库名、分支、提交 SHA、提交信息、触发人、时间、访问地址。
- 失败邮件：包含同样信息及 Actions 运行链接。

---

### 2.3 生产蓝绿部署 — deploy-prod.yml

**文件路径**：`.github/workflows/deploy-prod.yml`（179 行）

#### 触发条件

```yaml
on:
  workflow_dispatch:
    inputs:
      git_ref:
        description: 'Commit SHA or branch to build and deploy'
        required: true
        default: 'release/20260628'
      slot:
        description: 'green=待命槽位 | blue=切流后同步蓝'
        required: true
        type: choice
        default: 'green'
        options:
          - green
          - blue
      release_id:
        description: 'Release ticket ID from osh-prod-release (audit)'
        required: false
  repository_dispatch:
    types: [deploy-149-approved]
```

- **手动触发（`workflow_dispatch`）**：需传入 `git_ref`（构建目标）、`slot`（green/blue）、`release_id`（审计追踪）。
- **API 触发（`repository_dispatch`）**：外部系统发送 `deploy-149-approved` 事件触发。
- **不允许 push 自动触发**——生产部署必须人工审批。

#### 并发控制

```yaml
concurrency:
  group: deploy-backend-149-${{ github.event.inputs.slot || github.event.client_payload.slot || 'green' }}
  cancel-in-progress: false
```

按槽位（green/blue）分组，同一槽位同时只能有一个部署运行。

#### 蓝绿槽位架构

| 属性 | Green 槽位 | Blue 槽位 |
|------|-----------|-----------|
| 后端目录 | `/opt/osh-green/app/osh/osh-backend` | `/opt/osh/app/osh/osh-backend` |
| 容器名 | `osh-g-backend` | `osh-backend` |
| Nginx 端口 | `28080` | `58080` |
| 后端端口 | `28081` | `58081` |
| Nacos 地址 | `osh-g-nacos:8848` | `osh-nacos:8848` |
| Secret Manager | `http://osh-g-secret-manager:59100` | `http://osh-secret-manager:59100` |

#### 关键步骤：热补丁（Patch）

```yaml
- name: Patch jar bootstrap for 149 slot
  run: python3 .github/scripts/patch-149-config.py --jar backstage-admin/target/backstage-admin.jar --slot "$SLOT" --src-host "${{ vars.TEST25_DEPLOY_HOST || '43.242.200.25' }}"
```

`patch-149-config.py`（177 行）是部署的核心组件，功能：
1. 解压 jar 中的 `BOOT-INF/classes/bootstrap.yml`。
2. 将测试机地址 `43.242.200.25` 替换为生产机地址 `149.88.92.159`。
3. 替换 Nacos 服务发现地址（green 用 `osh-g-nacos:8848`，blue 用 `osh-nacos:8848`）。
4. 替换 Secret Manager 地址。
5. 重新打包 jar 并验证产物中不再包含测试机 IP。

还有一层**远程 fallback patch**：若本地 patch 遗漏，服务器上的 `/opt/osh-green/005-scripts/patch-149-config.py` 会再次执行（同样传入 `--src-host` 参数）。

#### 关键步骤：槽位路径安全校验（Guard）

```bash
if [ "$SLOT" = "green" ]; then
  case "$BACKEND_DIR" in /opt/osh-green/*) ;; *) exit 1 ;; esac
  case "$CONTAINER" in osh-g-*) ;; *) exit 1 ;; esac
else
  case "$BACKEND_DIR" in /opt/osh/app/*) ;; *) exit 1 ;; esac
  case "$CONTAINER" in osh-backend) ;; *) exit 1 ;; esac
  case "$BACKEND_DIR" in /opt/osh-green/*) exit 1 ;; esac  # 交叉污染防护
fi
```

这是部署安全的**最后一道防线**：
- Green 槽位所有资源必须在 `/opt/osh-green/` 命名空间下。
- Blue 槽位必须在 `/opt/osh/app/` 下，且**明确禁止**部署到 green 路径。
- 即使上游配置解析错误，此步骤也能阻止错误部署。

#### 健康检查

```yaml
for i in $(seq 1 12); do
  code="$(ssh ... "curl -s -o /dev/null -w '%{http_code}' --max-time 10 http://127.0.0.1:${NGINX_PORT}/api/ || echo 000")"
  if [ "$code" = "200" ] || [ "$code" = "401" ]; then exit 0; fi
  sleep 10
done
exit 1
```

最多轮询 12 次（间隔 10 秒，总计 120 秒），接受 HTTP 200 或 401 作为健康状态（401 表示 API 网关已响应，只是未认证）。

---

### 2.4 前端部署 — deploy-frontend-149.yml

**文件路径**：`.github/workflows/deploy-frontend-149.yml`（129 行）

> **历史说明**：此文件原位于 `.github/deploy-149.yml`（不在 `workflows/` 子目录内），不会被 GitHub Actions 自动发现。已于 2026-09-27 移动到正确位置。

此 workflow 与 `deploy-prod.yml` 完全对称，但面向**前端静态资源**部署：
- 使用 Node.js 18 而非 JDK 17。
- 构建命令为 `npm ci --legacy-peer-deps` + `npm run generate`。
- 部署目标为 Nginx 静态资源目录而非后端 jar。
- 同样支持 green/blue 槽位、路径安全校验、HTTP 健康检查。

---

### 2.5 变量配置示例 — deploy-release.variables.example

**文件路径**：`.github/deploy-release.variables.example`（24 行）

这是一份**配置指南**，说明需要在 GitHub Repository Variables 中设置的变量。关键信息：

```text
# 必填
TEST25_DEPLOY_HOST=43.242.200.25
TEST25_DEPLOY_PORT=58753

# 可选（有合理默认值）
# TEST25_DEPLOY_USER=root
# TEST25_BACKEND_DEPLOY_DIR=/opt/osh/app/osh/osh-backend
# TEST25_BACKEND_JAR_NAME=backstage-admin.jar
# TEST25_DOCKER_COMPOSE_DIR=/opt/osh-deploy-tools/bundle
# TEST25_DOCKER_COMPOSE_FILE=docker-compose.osh.yml
# TEST25_DOCKER_COMPOSE_ENV=/opt/osh/001-docker-compose/osh/osh-stack.env
# TEST25_DOCKER_COMPOSE_PROJECT=osh
# TEST25_BACKEND_SERVICE_NAME=osh-backend
# TEST25_BACKEND_PORT=8081
```

注释明确说明 25 测试机与 149 生产机的变量**完全分开、互不影响**。

---

## 3. 构建与部署脚本分析

### 3.1 CI 辅助脚本（scripts/）

#### scripts/verify-ai-process.sh（70 行）

**用途**：OpenSpec 流程完整性校验，由 `pr-check.yml` 的 `process-guard` Job 调用。

**检查内容**：

1. **14 个必需文件存在性检查**：
   - `AGENTS.md`、`CONTRIBUTING.md`、`CODE_REVIEW.md`、`SECURITY.md`
   - `docs/ai/README.md`、`docs/ai/01-newcomer-guide.md`、`docs/ai/02-workflow.md`、`docs/ai/03-codex-harness.md`
   - `openspec/project.md`、`openspec/templates/proposal.md`、`openspec/templates/design.md`、`openspec/templates/tasks.md`、`openspec/templates/spec.md`
   - `.github/PULL_REQUEST_TEMPLATE.md`

2. **OpenSpec 变更完整性**：若 PR 涉及 `openspec/changes/` 目录的变更，强制要求每个 change 目录下包含 `proposal.md`、`design.md`、`tasks.md`、`specs/**/spec.md`。

3. **豁免机制**：若环境变量 `ALLOW_OPENSPEC_EXEMPT=true`（由 PR body 中的 `OpenSpec-Exempt: true` 触发），允许跳过 OpenSpec 检查。

#### scripts/code-quality-guard.sh（436 行）

**用途**：通用化编译前静态质量门禁，由 `pr-check.yml` 调用，也可本地运行（`bash scripts/code-quality-guard.sh`）或通过 Git pre-commit hook 运行。

**配置文件**：`.quality-guard.conf`

**6 项检查**：

| 检查项 | 说明 |
|--------|------|
| **CHECK 1**: 必填字段 Service 层完整性 | 对配置的 `REQUIRED_FIELDS`（当前为 `no`），若实体类有该字段，Service insert 路径必须调用 `setXxx()` |
| **CHECK 2**: 必填字段四层一致性 | DDL / ALTER / Entity / VO 四层同步检查 |
| **CHECK 3**: 枚举值使用追踪 | 扫描 `ENUM_FILES` 中配置的枚举类，报告未被引用的枚举值 |
| **CHECK 4**: ALTER vs 初始 DDL 同步 | ALTER ADD COLUMN 的列必须在初始建表 DDL 中也存在 |
| **CHECK 5**: DDL 索引幽灵列 | 索引引用的列必须在同一文件中有列定义 |
| **CHECK 6**: DDL vs 实体类型一致性 | Java 字段类型与 SQL 列类型的映射比对 |

**动态实体发现机制**：Phase 0 阶段自动扫描 `*ServiceImpl.java` 中的 `mapper.insert` 调用，解析涉及的实体类型，无需硬编码实体名。

#### scripts/install-hooks.sh（31 行）

**用途**：安装本地 Git pre-commit 钩子，使每次 `git commit` 前自动运行 `code-quality-guard.sh`。

```bash
cat > "$HOOK_FILE" << 'HOOK'
echo "Running code-quality-guard..."
bash scripts/code-quality-guard.sh
HOOK
```

**与 CI 的关系**：这是 CI 门禁的**本地前置**——开发者在本地提交前即可发现问题，而非等到 PR CI 运行后才得知。可通过 `git commit --no-verify` 临时跳过。

---

### 3.2 本地开发脚本（bin/ 与根目录）

#### bin/run.bat（14 行）

Windows 本地启动脚本，直接执行 `java -jar backstage-admin.jar`。

```batch
set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m
java -jar %JAVA_OPTS% backstage-admin.jar
```

**判断**：纯本地开发用，**不参与 CI/CD**。JVM 参数与 `ry.sh` 不同（内存配置更小），适合开发机环境。

#### bin/package.bat（12 行）

Windows 本地 Maven 打包脚本：

```batch
call mvn clean package -Dmaven.test.skip=true
```

**判断**：纯本地构建用，**不参与 CI/CD**。CI 中使用 `mvn clean install -DskipTests -pl '!backstage-flink'`，命令更精确（排除 Flink 模块）。

#### bin/clean.bat（12 行）

Windows 本地 Maven 清理脚本：`call mvn clean`。**不参与 CI/CD**。

#### bin/ensure_zentao_mcp_token.ps1（510 行）

ZenTao MCP Token 守卫脚本。功能：
1. 读取 `MCP.json` 配置中的禅道 MCP 服务器信息。
2. 测试当前 token 是否有效（调用 ZenTao API 验证）。
3. 若 token 无效/过期，提示用户输入账号密码，调用登录接口获取新 token。
4. 将新 token 写回配置文件并验证。

**判断**：这是 **AI Agent 工作流辅助工具**，用于确保禅道 MCP 调用的认证有效。与 CI/CD 部署流程**无关**。

#### ry.sh（87 行）

Linux 服务管理脚本（若依框架原生），支持 `start`/`stop`/`restart`/`status` 命令：

```bash
nohup java $JVM_OPTS -jar $AppName > /dev/null 2>&1 &
```

**判断**：可被远程 SSH 调用管理裸进程服务，但**当前 CI 未使用此脚本**。CI 部署流程使用 Docker Compose 管理容器（`docker compose stop/up`），而非直接管理 Java 进程。此脚本是若依框架的遗留组件，主要用于开发/调试场景。

---

## 4. 其他 CI 工具痕迹排查

| CI 工具 | 搜索路径 | 结果 |
|---------|---------|------|
| Jenkins | `**/Jenkinsfile` | **未找到** |
| Travis CI | `**/.travis.yml` | **未找到** |
| GitLab CI | `**/.gitlab-ci.yml` | **未找到** |
| Docker | `**/Dockerfile*` | **未找到**（Docker 配置在远程服务器上） |
| Docker Compose | `**/docker-compose*` | **未找到**（配置文件在 25/149 服务器上） |
| CircleCI | `**/.circleci/` | **未找到** |
| Azure Pipelines | `**/azure-pipelines.yml` | **未找到** |
| SonarQube | `sonar-project.properties` | **存在**，但仅用于代码质量分析，不涉及部署 |

**结论**：项目**仅使用 GitHub Actions** 作为 CI/CD 平台，未使用过其他 CI 工具。Docker Compose 编排文件存在于远程服务器（如 `/opt/osh-deploy-tools/bundle/docker-compose.osh.yml`），不在代码仓库中。

---

## 5. CI/CD 分层架构总结

### 5.1 三层流水线架构

```
  ┌─────────────────────────────────────────────────────────┐
  │                    分支流转模型                           │
  │  feature/* ──PR──→ qa/** ──push──→ release/** ──manual──→ master │
  └─────────────────────────────────────────────────────────┘
         │                │                  │
         ▼                ▼                  ▼
  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐
  │ pr-check.yml│  │deploy-qa.yml │  │ deploy-prod.yml  │
  │  PR 质量门禁  │  │ QA 自动部署   │  │ 生产蓝绿部署      │
  │  (自动)      │  │ (自动)        │  │ (手动审批)        │
  ├─────────────┤  ├──────────────┤  ├──────────────────┤
  │• OpenSpec   │  │• Maven 构建   │  │• Maven 构建       │
  │  完整性校验  │  │• SHA256 校验  │  │• 热补丁(Patch)    │
  │• 6项代码     │  │• SSH + rsync  │  │• 槽位安全校验     │
  │  静态检查    │  │• Docker 重启  │  │• SSH + rsync      │
  │• 7个核心     │  │• 邮件通知     │  │• SHA256 双重校验  │
  │  测试套件    │  │              │  │• HTTP 健康轮询     │
  │• Maven 构建  │  │              │  │  (120s)           │
  └─────────────┘  └──────────────┘  └──────────────────┘
```

### 5.2 环境隔离策略

| 维度 | QA 环境（25 测试机） | 生产环境（149 生产机） |
|------|---------------------|---------------------|
| 服务器 IP | `43.242.200.25` | `149.88.92.159` |
| SSH 端口 | `58753` | `16328` |
| 部署方式 | Docker Compose | Docker（直接管理容器） |
| 触发方式 | Push 到 qa 分支自动触发 | 手动审批触发 |
| 槽位 | 单实例 | Green / Blue 双槽位 |
| 热补丁 | 不需要 | `patch-149-config.py` 替换地址 |
| 健康检查 | 无（仅输出日志） | HTTP 轮询 120s |
| 邮件通知 | 有 | 有 |

### 5.3 安全校验措施

| 校验项 | 位置 | 说明 |
|--------|------|------|
| SHA256 双重校验 | deploy-qa.yml、deploy-prod.yml | 本地构建 jar 的 SHA256 必须与远程接收后计算的 SHA256 完全一致 |
| jar 完整性校验 | deploy-qa.yml、deploy-prod.yml | `unzip -t` 验证 jar 包内依赖可读 |
| 槽位路径 Guard | deploy-prod.yml | green 必须部署到 `/opt/osh-green/*`，blue 必须部署到 `/opt/osh/app/*`，禁止交叉 |
| 容器名 Guard | deploy-prod.yml | green 容器必须 `osh-g-*` 前缀，blue 容器必须 `osh-backend` |
| SSH known_hosts | 所有部署 workflow | 3 次重试添加 known_hosts，防止中间人攻击 |
| 热补丁验证 | patch-149-config.py | 替换完成后验证产物中不再包含测试机 IP `43.242.200.25` |
| 并发控制 | 所有 workflow | `cancel-in-progress: false`（部署类）防止半部署状态 |
| 健康检查 | deploy-prod.yml | HTTP 轮询 `/api/`，接受 200 或 401，最多 120 秒 |

---

## 6. GitHub Secrets 与 Variables 完整清单

### 6.1 Secrets（敏感信息）

| Secret 名称 | 用途 | 使用 Workflow |
|-------------|------|--------------|
| `DEPLOY_SSH_PRIVATE_KEY` | 25 测试机 SSH 部署私钥 | deploy-qa.yml |
| `HOST_149_SSH_PRIVATE_KEY` | 149 生产机 SSH 部署私钥（首选） | deploy-prod.yml |
| `PROD_SSH_PRIVATE_KEY` | 149 生产机 SSH 部署私钥（备选） | deploy-frontend-149.yml |
| `MAIL_PASSWORD` | QQ 邮箱 SMTP 授权码 | deploy-qa.yml、deploy-prod.yml |
| `NOTIFY_EMAIL` | 通知邮箱地址（备选，优先用 Variable） | deploy-qa.yml、deploy-prod.yml |

### 6.2 Variables（可配置参数）

#### 25 测试机 Variables

| Variable 名称 | 用途 | 默认值 |
|--------------|------|--------|
| `TEST25_DEPLOY_HOST` | 测试机 IP | 无（**必填**） |
| `TEST25_DEPLOY_PORT` | SSH 端口 | 无（**必填**） |
| `TEST25_DEPLOY_USER` | SSH 用户 | `root` |
| `TEST25_BACKEND_DEPLOY_DIR` | jar 上传目录 | `/opt/osh/app/osh/osh-backend` |
| `TEST25_BACKEND_JAR_NAME` | jar 文件名 | `backstage-admin.jar` |
| `TEST25_DOCKER_COMPOSE_DIR` | Docker Compose 目录 | `/opt/osh-deploy-tools/bundle` |
| `TEST25_DOCKER_COMPOSE_FILE` | Compose 文件名 | `docker-compose.osh.yml` |
| `TEST25_DOCKER_COMPOSE_ENV` | 环境变量文件 | `/opt/osh/001-docker-compose/osh/osh-stack.env` |
| `TEST25_DOCKER_COMPOSE_PROJECT` | Compose 项目名 | `osh` |
| `TEST25_BACKEND_SERVICE_NAME` | 后端服务名 | `osh-backend` |

#### 149 生产机 Variables

| Variable 名称 | 用途 | 默认值 |
|--------------|------|--------|
| `HOST_149` / `GREEN149_HOST` | 生产机 IP | `149.88.92.159` |
| `HOST_149_USER` / `GREEN149_USER` | SSH 用户 | `root` |
| `HOST_149_PORT` / `GREEN149_PORT` | SSH 端口 | `16328` |
| `GREEN149_BACKEND_DIR` | Green 后端目录 | `/opt/osh-green/app/osh/osh-backend` |
| `GREEN149_BACKEND_CONTAINER` | Green 容器名 | `osh-g-backend` |
| `GREEN149_NGINX_PORT` | Green Nginx 端口 | `28080` |
| `BLUE149_BACKEND_DIR` | Blue 后端目录 | `/opt/osh/app/osh/osh-backend` |
| `BLUE149_BACKEND_CONTAINER` | Blue 容器名 | `osh-backend` |
| `BLUE149_NGINX_PORT` | Blue Nginx 端口 | `58080` |
| `BACKEND_JAR_NAME` | jar 文件名 | `backstage-admin.jar` |

#### 通用 Variables

| Variable 名称 | 用途 | 默认值 |
|--------------|------|--------|
| `NOTIFY_EMAIL` | 通知邮箱 | `1402272093@qq.com` |
| `MAIL_USERNAME` | 发件邮箱 | `1402272093@qq.com` |
| `MAIL_SERVER` | SMTP 服务器 | `smtp.qq.com` |
| `MAIL_PORT` | SMTP 端口 | `465` |

---

## 7. 硬编码值与可配置性分析

### 7.1 Workflow 中的硬编码值

| 硬编码值 | 所在文件 | 可配置性 |
|----------|---------|---------|
| `JAVA_VERSION: '17'` | 所有 workflow | 环境变量级，修改 env 块即可 |
| `149.88.92.159` | deploy-prod.yml | 有 Variable 覆盖（`HOST_149`），硬编码仅作最终 fallback |
| `16328` | deploy-prod.yml | 有 Variable 覆盖（`HOST_149_PORT`） |
| `/opt/osh-green/*`、`/opt/osh/app/*` | deploy-prod.yml | 有 Variable 覆盖，但 Guard 步骤的路径校验也硬编码了这些前缀 |
| `smtp.qq.com`、`465` | deploy-qa.yml、deploy-prod.yml | 有 Variable 覆盖 |
| `1402272093@qq.com` | deploy-qa.yml、deploy-prod.yml | 有 Variable/Secret 覆盖 |
| `43.242.200.25` | deploy-qa.yml | 有 Variable 覆盖（`TEST25_DEPLOY_HOST`） |
| `http://43.242.200.25:8081/` | deploy-qa.yml（邮件 body） | ~~硬编码~~ **已修复**：改用 `${{ env.DEPLOY_HOST }}:${{ vars.TEST25_BACKEND_PORT || '8081' }}` |
| `release/20260628` | deploy-prod.yml（default） | workflow_dispatch 的默认值，手动触发时可覆盖 |

### 7.2 patch-149-config.py 中的硬编码值

| 硬编码值 | 说明 | 风险 |
|----------|------|------|
| `SRC_HOST = "43.242.200.25"` | 测试机 IP，作为替换源 | ~~中等~~ **已修复**：新增 `--src-host` 参数，默认值保持向后兼容 |
| Green Nacos: `osh-g-nacos:8848` | Docker 内部服务名 | 低——与 Docker Compose 配置绑定 |
| Blue Nacos: `osh-nacos:8848` | Docker 内部服务名 | 低 |
| Green Secret Manager: `http://osh-g-secret-manager:59100` | Docker 内部服务名 | 低 |
| Green 端口 `28080`/`28081`、Blue 端口 `58080`/`58081` | 槽位端口 | 低——与服务器实际配置一致即可 |

### 7.3 code-quality-guard.sh 的配置化设计

此脚本的配置通过 `.quality-guard.conf` 外部化，当前配置：

```properties
REQUIRED_FIELDS=no                    # 当前未启用必填字段检查
TYPE_CHECK_FIELDS=resource_type       # 对 resource_type 字段做类型一致性检查
ENUM_FILES=...ResourceCodePrefixEnum.java  # 追踪此枚举的使用情况
SERVICE_DIR=backstage-system
ENTITY_SEARCH_DIR=backstage-system
DDL_DIR=sql
```

---

## 8. 改进建议（已全部实施，2026-09-27）

本章记录 CI/CD 审计中发现的 6 项问题的实施结果。每项包含问题描述、实施状态、关键代码变更、验证方法和注意事项。

---

### 8.1 deploy-149.yml 位置修复

**问题描述**：前端部署 workflow 文件 `.github/deploy-149.yml` 不在 `.github/workflows/` 目录下，GitHub Actions **仅自动发现** `workflows/` 子目录中的 YAML 文件，因此该 workflow 永远不会被执行。这是一个严重遗漏——前端蓝绿部署能力完全失效。

**实施状态**：✅ 已完成

**修改的文件**：

| 文件 | 操作 | 说明 |
|------|------|------|
| `.github/workflows/deploy-frontend-149.yml` | 新建（129 行） | 从原位置迁入 `workflows/` 目录 |
| `.github/deploy-149.yml` | 删除 | 原位置文件已移除 |

**关键代码变更**：

```yaml
# 文件: .github/workflows/deploy-frontend-149.yml (L1)
# 149 蓝/绿槽位前端部署 — 平台审批后触发，同一套 workflow，slot 区分目标
name: Deploy Frontend to 149
```

首行注释从原先的“代码部署”更正为“前端部署”，明确其职责为 Nuxt 静态资源部署（`npm ci` → `npm run generate` → `rsync`），与后端 `deploy-prod.yml` 形成前后端对称部署架构。

**验证方法**：
1. 推送后在 GitHub 仓库 `Actions` 页面查看是否出现 “Deploy Frontend to 149” workflow。
2. 确认 `.github/deploy-149.yml` 已不存在：`git ls-files .github/deploy-149.yml` 应返回空。
3. 手动触发 `workflow_dispatch` 测试前端部署是否正常执行。

**注意事项**：
- 此 workflow 依赖前端项目的 `npm run generate` 脚本，需确保 `backstage-ui/package.json` 中存在该命令。
- SSH 密钥 Secret 链 `HOST_149_SSH_PRIVATE_KEY → GREEN149_SSH_PRIVATE_KEY → PROD_SSH_PRIVATE_KEY → DEPLOY_SSH_PRIVATE_KEY` 至少需要配置一个。

---

### 8.2 + 8.3 deploy-prod.yml 添加邮件通知

**问题描述**：
- **8.2**：`deploy-qa.yml` 有成功/失败邮件通知（`dawidd6/action-send-mail@v3`），但 `deploy-prod.yml` 完全没有任何通知步骤，团队无法第一时间获知生产部署结果。
- **8.3**：`deploy-prod.yml` 的健康检查（120s HTTP 轮询）失败后仅 `exit 1`，无主动通知机制，运维需手动查看 Actions 日志才能发现失败。

**实施状态**：✅ 已完成（8.3 由 8.2 的 `Notify on failure` 步骤一并覆盖）

**修改的文件**：

| 文件 | 行号 | 说明 |
|------|------|------|
| `.github/workflows/deploy-prod.yml` | L181–L205 | 新增 “Notify on success” 步骤 |
| `.github/workflows/deploy-prod.yml` | L207–L228 | 新增 “Notify on failure” 步骤 |

**关键代码变更**：

成功通知（L181–L205）：

```yaml
- name: Notify on success
  if: success()
  continue-on-error: true
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: ${{ env.MAIL_SERVER }}
    server_port: ${{ env.MAIL_PORT }}
    secure: true
    username: ${{ env.MAIL_USERNAME }}
    password: ${{ secrets.MAIL_PASSWORD }}
    subject: "✅ 生产后端部署成功 - ${{ github.repository }} [${{ env.SLOT }}]"
    to: ${{ env.NOTIFY_EMAIL }}
    from: GitHub Actions <${{ env.MAIL_USERNAME }}>
    body: |
      生产后端部署成功！

      仓库：${{ github.repository }}
      Git Ref：${{ env.GIT_REF }}
      槽位：${{ env.SLOT }}
      Release ID：${{ env.RELEASE_ID || 'N/A' }}
      提交：${{ github.sha }}
      触发人：${{ github.actor }}
      Nginx 端口：${{ steps.target.outputs.nginx_port }}

      访问地址：http://${{ env.DEPLOY_HOST }}:${{ steps.target.outputs.nginx_port }}/
```

失败通知（L207–L228）：

```yaml
- name: Notify on failure
  if: failure()
  continue-on-error: true
  uses: dawidd6/action-send-mail@v3
  with:
    # ... SMTP 配置同上 ...
    subject: "❌ 生产后端部署失败 - ${{ github.repository }} [${{ env.SLOT }}]"
    body: |
      生产后端部署失败！请检查 Actions 日志。
      ...
      查看详情：https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }}
```

**验证方法**：
1. 手动触发 `workflow_dispatch` 部署一次，检查邮箱是否收到成功/失败邮件。
2. 确认邮件主题包含 `[green]` 或 `[blue]` 槽位标识。
3. 确认成功邮件中的访问地址为动态拼接（`http://149.88.92.159:28080/` 或 `:58080/`），而非硬编码。

**注意事项**：
- `MAIL_PASSWORD` Secret 必须已在仓库中配置，否则邮件发送会静默失败（`continue-on-error: true`）。
- 失败通知中使用 `env.GIT_REF` 而非 `github.sha`，因为 `workflow_dispatch` 的 `github.sha` 指向默认分支而非用户指定的 `git_ref`。
- 8.3 的健康检查失败会触发 Job-level failure，`if: failure()` 条件自动满足，无需额外的健康检查专用通知步骤。

---

### 8.4 ry.sh / run.bat 适用场景文档化

**问题描述**：`ry.sh` 使用 `nohup java -jar` 裸进程管理，而 CI/CD 使用 Docker/Docker Compose 管理容器。两套方式共存，新成员可能误在生产服务器上使用 `ry.sh start`，导致进程与 Docker 容器端口冲突。

**实施状态**：✅ 已完成

**修改的文件**：

| 文件 | 行号 | 说明 |
|------|------|------|
| `ry.sh` | L1–L10 | 替换原单行注释为醒目警告块 |
| `bin/run.bat` | L2–L8 | 新增 REM 警告注释块 |
| `README.md` | L77–L99 | 新增“快速启动”章节 |

**关键代码变更**：

`ry.sh` 顶部警告注释（L1–L10）：

```sh
#!/bin/sh
# ============================================================
# ry.sh — 若依框架原生服务管理脚本（本地开发/调试专用）
#
# ⚠️  警告：本脚本仅用于本地开发环境，禁止在生产/测试服务器使用。
#    生产环境统一使用 Docker 容器管理，由 CI/CD 流水线
#    (deploy-qa.yml / deploy-prod.yml) 通过 SSH 远程执行
#    docker compose / docker 命令。
#
# 用法: ./ry.sh {start|stop|restart|status}
# ============================================================
```

`bin/run.bat` 顶部警告注释（L2–L8）：

```batch
REM ============================================================
REM run.bat — 本地开发启动脚本（Windows）
REM
REM [WARNING] 本脚本仅用于本地开发环境，禁止在生产/测试服务器使用。
REM 生产环境统一使用 Docker 容器管理，由 CI/CD 流水线
REM (deploy-qa.yml / deploy-prod.yml) 远程执行 docker 命令。
REM ============================================================
```

`README.md` 新增“快速启动”章节（L77–L99）：

```markdown
## 快速启动

### 开发环境启动

| 平台 | 启动方式 |
|------|----------|
| Windows | 运行 `bin\run.bat`，或直接在 IDE 中启动 `RuoYiApplication` 主类 |
| Linux/macOS | 运行 `./ry.sh start`，停止用 `./ry.sh stop` |

> **注意**：`ry.sh` 和 `bin/run.bat` 使用裸进程管理（`nohup java -jar`），**仅适用于本地开发**。

### 生产环境部署

| 环境 | 触发方式 | 说明 |
|------|----------|------|
| QA 测试机（25） | Push 到 `qa/**` 分支自动触发 | `deploy-qa.yml` 自动构建并部署 |
| 生产机（149） | 手动审批触发（workflow_dispatch） | `deploy-prod.yml` 支持绿/蓝槽位部署 |

生产环境使用 Docker 容器管理，禁止使用 `ry.sh` 或 `run.bat` 启动服务。
```

**验证方法**：
1. `head -10 ry.sh` 确认警告注释块已就位。
2. `type bin\run.bat` 确认 REM 注释已就位。
3. 在 GitHub 仓库首页查看 README.md 中是否显示“快速启动”章节。

**注意事项**：
- 这些注释是纯文档性变更，不影响脚本的实际执行逻辑。
- 若未来引入 Docker Compose 本地开发环境（`docker-compose.dev.yml`），应在 README 的“开发环境启动”章节中同步补充。

---

### 8.5 patch-149-config.py SRC_HOST 参数化

**问题描述**：`.github/scripts/patch-149-config.py` 第 12 行硬编码 `SRC_HOST = "43.242.200.25"`。若测试机 IP 变更，必须修改 Python 源码并提交代码，增加了不必要的代码变更和审查成本。

**实施状态**：✅ 已完成

**修改的文件**：

| 文件 | 行号 | 说明 |
|------|------|------|
| `.github/scripts/patch-149-config.py` | L12 | 添加注释标注可覆盖 |
| `.github/scripts/patch-149-config.py` | L151–L155 | 新增 `--src-host` argparse 参数 |
| `.github/scripts/patch-149-config.py` | L161–L162 | `main()` 中更新全局变量 |
| `.github/workflows/deploy-prod.yml` | L89 | 本地 patch 传入 `--src-host` |
| `.github/workflows/deploy-prod.yml` | L151 | 远程 fallback patch 传入 `--src-host` |

**关键代码变更**：

`patch-149-config.py` — 新增参数（L12, L151–L155, L161–L162）：

```python
SRC_HOST = "43.242.200.25"  # Default, overridable via --src-host

# ... (在 main() 中)
parser.add_argument(
    "--src-host",
    default="43.242.200.25",
    help="Source host IP to replace in artifacts (default: 43.242.200.25)",
)

# ... (解析参数后)
# Allow overriding source host without changing function signatures
global SRC_HOST
SRC_HOST = args.src_host
```

`deploy-prod.yml` — 本地 patch 调用（L89）：

```yaml
- name: Patch jar bootstrap for 149 slot
  run: python3 .github/scripts/patch-149-config.py --jar backstage-admin/target/backstage-admin.jar --slot "$SLOT" --src-host "${{ vars.TEST25_DEPLOY_HOST || '43.242.200.25' }}"
```

`deploy-prod.yml` — 远程 fallback patch（L151）：

```yaml
python3 /opt/osh-green/005-scripts/patch-149-config.py --jar '$BACKEND_DIR/$BACKEND_JAR_NAME' --slot '$SLOT' --src-host '${{ vars.TEST25_DEPLOY_HOST || '43.242.200.25' }}' || \
```

**验证方法**：

```bash
# 验证参数已注册
python3 .github/scripts/patch-149-config.py --help
# 输出应包含: --src-host SRC_HOST  Source host IP to replace in artifacts

# 验证默认值仍生效（向后兼容）
python3 .github/scripts/patch-149-config.py --jar <path> --slot green
# 应使用默认 43.242.200.25

# 验证自定义值生效
python3 .github/scripts/patch-149-config.py --jar <path> --slot green --src-host 10.0.0.1
# 应替换 10.0.0.1 而非 43.242.200.25
```

**注意事项**：
- 使用 `global SRC_HOST` 模式而非修改所有函数签名，因为 `patch_frontend_text()`、`patch_bootstrap_text()`、`verify_no_src_host()` 均通过全局变量访问 `SRC_HOST`，参数化修改在 `main()` 入口处一次性生效。
- 若测试机 IP 变更，只需在 GitHub Repository Variables 中修改 `TEST25_DEPLOY_HOST`，脚本代码无需改动。
- 远程 fallback 脚本（`/opt/osh-green/005-scripts/patch-149-config.py`）可能是旧版本，不认识 `--src-host` 参数。但该步骤有 `|| echo 'skip remote patch (script missing)'` 容错，不影响主流程。

---

### 8.6 deploy-qa.yml 邮件地址硬编码修复

**问题描述**：`deploy-qa.yml` 第 154 行邮件 body 中硬编码 `http://43.242.200.25:8081/`，未使用 workflow 变量。若测试机 IP 或端口变更，部署成功邮件中的访问地址将指向错误位置。

**实施状态**：✅ 已完成

**修改的文件**：

| 文件 | 行号 | 说明 |
|------|------|------|
| `.github/workflows/deploy-qa.yml` | L154 | 替换为动态表达式 |
| `.github/deploy-release.variables.example` | L20 | 新增 `TEST25_BACKEND_PORT` 变量示例 |

**关键代码变更**：

`deploy-qa.yml` L154 — 替换前：

```yaml
            访问地址：http://43.242.200.25:8081/
```

替换后：

```yaml
            访问地址：http://${{ env.DEPLOY_HOST }}:${{ vars.TEST25_BACKEND_PORT || '8081' }}/
```

- `env.DEPLOY_HOST` 已在 workflow 的 `env` 块中定义（L16：`${{ vars.TEST25_DEPLOY_HOST }}`），无需新增变量。
- `vars.TEST25_BACKEND_PORT` 为新增可选变量，默认值 `'8081'` 保持向后兼容。

`deploy-release.variables.example` L19–L20 — 新增变量示例：

```text
#
# 【可选，25 后端访问端口（用于部署成功邮件通知中的访问地址）】
# TEST25_BACKEND_PORT=8081
```

**验证方法**：

```bash
# 确认硬编码 IP 已消除
grep -n '43\.242\.200\.25:8081' .github/workflows/deploy-qa.yml
# 应返回空（0 结果）

# 确认动态表达式已就位
grep -n 'DEPLOY_HOST.*TEST25_BACKEND_PORT' .github/workflows/deploy-qa.yml
# 应返回: 154:             访问地址：http://${{ env.DEPLOY_HOST }}:${{ vars.TEST25_BACKEND_PORT || '8081' }}/
```

推送后触发一次 QA 部署，检查成功邮件中的访问地址是否显示为 `http://43.242.200.25:8081/`（当前默认值下与修改前效果一致）。

**注意事项**：
- 若未在 GitHub Variables 中配置 `TEST25_BACKEND_PORT`，表达式会 fallback 到默认值 `'8081'`，行为与修改前完全一致。
- `env.DEPLOY_HOST` 的值来自 `vars.TEST25_DEPLOY_HOST`，该变量是**必填项**（无默认值）。若未配置，workflow 会在 SSH 连接阶段就失败，不会走到邮件通知步骤。

---

### 变更文件清单总览

| 文件 | 操作 | 行数变化 | 关联改进 |
|------|------|---------|----------|
| `.github/workflows/deploy-frontend-149.yml` | 新建 | +129 | 8.1 |
| `.github/deploy-149.yml` | 删除 | -129 | 8.1 |
| `.github/workflows/deploy-prod.yml` | 修改 | +54 / -3 | 8.2, 8.3, 8.5 |
| `.github/workflows/deploy-qa.yml` | 修改 | +1 / -1 | 8.6 |
| `.github/scripts/patch-149-config.py` | 修改 | +10 / -1 | 8.5 |
| `ry.sh` | 修改 | +10 / -1 | 8.4 |
| `bin/run.bat` | 修改 | +7 | 8.4 |
| `README.md` | 修改 | +27 | 8.4 |
| `.github/deploy-release.variables.example` | 修改 | +3 | 8.6 |
| `docs/analysis/CI_CD_Audit_Report.md` | 修改 | 本报告同步更新 | 全部 |

---

> **附注**：本报告所有分析均基于代码库中的实际文件内容，未包含任何推测性结论。报告中的“判断”和“建议”基于对 GitHub Actions 机制和 DevOps 最佳实践的理解。第 8 章所有改进建议已于 2026-09-27 全部实施完毕。
