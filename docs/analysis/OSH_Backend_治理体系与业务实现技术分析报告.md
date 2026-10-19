# OSH Backend 治理体系与业务实现技术分析报告

> 本报告基于 `osh-backend` 仓库的治理文档（AGENTS.md、CONTRIBUTING.md、CODE_REVIEW.md 等）、CI 流水线配置、脚本文件，以及拼团模块、内部会员模块、课程文档模块的代码实现，进行系统性技术分析。

---

## 一、核心功能定位

### 1.1 SDD / OpenSpec 规则

**要解决的技术挑战**：`osh-backend` 是一个 Spring Boot 多模块单体，承载课程、工具、订单、拼团、秒杀、内部会员、反馈、问答等业务域。当多个 AI Agent 与人类开发者同时协作时，"需求只留在聊天记录/微信群/口头说明"会导致行为契约被随意破坏、变更不可追溯、回滚无依据。

**在架构中的角色**：SDD（Spec Driven Development）是**行为契约层**，位于业务代码之上、CI 流水线之内。它不关心 Java 实现，只关心"这个 PR 改变了什么行为、是否有评审依据、是否可验证"。

`openspec/project.md` 定义了分层边界（Controller 只做入口、Service 写业务编排、Mapper 只做数据访问），以及 9 类必须建 OpenSpec change 的场景（新功能、行为变化、接口/权限/数据/中间件/部署变化、基础组件升级、微服务拆分）。

### 1.2 ZenTao MCP Token Guard

**要解决的技术挑战**：AI Agent 通过 MCP 协议调用禅道（`http://43.242.200.25:52002/zentao/mcp`）进行项目管理操作，token 有有效期，过期后 MCP 调用会静默失败。

**在架构中的角色**：这是**凭证安全层**，确保 AI Agent 调用外部系统前凭证有效，且刷新过程不泄露明文密码。

### 1.3 治理文档矩阵

| 文档 | 面向角色 | 核心职责 |
|------|---------|---------|
| `AGENTS.md` | AI Agent | 强制阅读清单 + ZenTao 令牌守护 |
| `CONTRIBUTING.md` | 人类开发者 | 开发前/中/后的操作手册 |
| `CODE_REVIEW.md` | Reviewer | 审核必须检查的 6 条硬性门禁 |
| `docs/ai/07-review-rubric.md` | Reviewer | 7 维度 20+ 检查点的审核细则 |

---

## 二、关键实现细节

### 2.1 OpenSpec 目录的工程落地

当前 `openspec/changes/` 下有 **30+ 个实际变更目录**，覆盖课程支付、拼团修复、内部会员、文档管理等业务域。每个变更严格遵循：

```text
openspec/changes/<change-id>/
  proposal.md    # 为什么做（背景、目标、非目标、用户影响、风险、验收标准）
  design.md      # 怎么做（方案概览、接口设计、数据设计、业务流程、兼容性、回滚）
  tasks.md       # 分步执行（按优先级分阶段的 checklist）
  specs/
    <capability>/
      spec.md    # 行为契约（GIVEN/WHEN/THEN 格式）
```

**三个典型变更实例**：

| 变更 | 业务域 | 核心内容 |
|------|--------|---------|
| `fix-group-module-quality` | 拼团模块 | 21 项 P0-P3 代码漏洞修复：分布式锁、枚举常量化、Map→VO、状态越界 Bug |
| `add-internal-member-document-upload` | 内部会员 | 就业档案文档上传/预览/下载功能 |
| `course-section-doc-save-read` | 课程文档 | 课程小节文档持久化与读取 |

### 2.2 CI 流水线的五道质量关卡

`.github/workflows/pr-check.yml` 在 `qa/**`、`release/**`、`main`、`master` 分支触发：

```text
关卡 1: 流程合规（Spec & Process Guard）
  → verify-ai-process.sh 校验 14 个核心文件 + OpenSpec change 完整性
  → 正则检测 PR body 的 OpenSpec-Exempt 声明

关卡 2: 编译验证
  → mvn clean install -DskipTests -pl '!backstage-flink'

关卡 3: jar 完整性校验（deploy-qa/deploy-prod）
  → unzip -t 验证 jar 包结构 + sha256sum 生成摘要

关卡 4: 远程一致性校验
  → 部署后对比本地和远程 jar 的 SHA256

关卡 5: 健康检查（deploy-prod）
  → 轮询 API 端点，最多重试 12 次，每次间隔 10 秒
```

`scripts/verify-ai-process.sh` 的核心逻辑：

1. 强制 14 个核心文件存在且非空（`AGENTS.md`、`CONTRIBUTING.md`、`CODE_REVIEW.md`、`openspec/project.md` 等）
2. `git diff` 获取变更文件，若涉及 `openspec/changes/<change-id>/`，强制 `proposal.md`/`design.md`/`tasks.md`/`specs/**/spec.md` 齐全
3. 若无 OpenSpec 变更，检查 `ALLOW_OPENSPEC_EXEMPT=true`（由 PR body 的正则检测设置）

### 2.3 ZenTao Token Guard 的实现

`bin/ensure_zentao_mcp_token.ps1`（510 行）的完整生命周期：

```text
读取 mcp.json → 取出 zentao.url、headers.token、apiBaseUrl
     │
     ▼
Test-ZentaoToken：用现有 token 调用 /api.php/v2/users?page=1&limit=1
     │
     ├─ 有效 → 回写 apiBaseUrl → exit 0
     │
     └─ 失效/为空
           │
           ▼
           Read-Host 交互式索取账号密码（SecureString 读入）
           │
           ▼
           Invoke-ZentaoLogin：尝试 /api.php/v2/users/login 和 /api.php/v2/user/login
           │
           ▼
           从 JSON 响应或 Set-Cookie 头解析新 token
           │
           ▼
           写回 mcp.json → 二次验证 → exit 0
```

### 2.4 业务模块的代码层落地

#### 拼团模块：OshGroupServerServiceImpl

`OshGroupServerServiceImpl`（2159 行）是 `fix-group-module-quality` 变更的核心实施对象，体现了治理规范如何落地到代码：

- **分布式锁**：`LOCK_GROUP_JOIN_PREFIX`/`LOCK_GROUP_PAY_CALLBACK_PREFIX` 使用 Redisson `setIfAbsent` + 10s TTL + `finally delete`，对应 spec 中"拼团写操作接口必须使用分布式锁防重复提交"的行为契约
- **CAS 防超卖**：支付回调中 `currentNum` 使用 CAS 原子递增，`join` 不递增、延迟到支付成功回调才递增，对应 design.md 中"延迟递增 currentNum"的架构决策
- **双状态码体系**：`osh_group_work.group_status`（0=待支付, 1=已支付, 2=已成团, 3=已结束）与 `osh_group_user_initiated.group_status`（0=招募中, 1=已成团, 2=已结束）独立映射
- **枚举常量化**：`GroupStatusEnum`、`GroupOrderStatusEnum`、`GroupConstants`（35 个常量）替代 54+ 处魔法值
- **Kafka 事件发布**：`GroupEventPublisher` 在事务提交后发布参团/支付事件，防止回滚不一致

#### 内部会员模块：InternalMemberServiceImpl

`InternalMemberServiceImpl`（817 行）是 `add-internal-member-document-upload` 变更的实施对象：

- **文档管理全链路**：`uploadEmploymentDocument`（上传到 OSS）→ `previewEmploymentDocument`（生成预签名 URL，默认 30 分钟有效）→ `downloadEmploymentDocument` → `deleteEmploymentDocument`
- **跟进任务系统**：`addFollowTask` → `completeFollowTask`（自动追加跟进记录）→ `processDueFollowTaskReminders`（定时发送邮件提醒）→ `markOverdueFollowTasks`（标记逾期任务）
- **成长故事生成**：`InternalMemberGrowthStoryServiceImpl` 聚合学习摘要、学习态度、就业档案、简历摘要、面试摘要、考试摘要 6 个维度的数据

#### 课程文档模块：OshDoc + OshDocRef

`OshDoc` 和 `OshDocRef` 是 `course-section-doc-save-read` 变更的数据模型基础：

- **OshDoc 主表**：`id`（雪花算法）、`title`、`content_format`（html/markdown）、`content`（longtext）、`status`（0 草稿/2 审核中/4 已发布/6 审核拒绝）、`visibility`（0 私有/1 跟随资源）
- **OshDocRef 关联表**：`ref_type` + `ref_id` 的多态设计，支持课程小节、考试、书籍、工具等多种资源类型；`anchor_type`（full/fragment）支持引用整篇文档或文档片段
- **持久化流程**：`OshCourseServiceImpl.saveSectionDocIfNeeded` 实现三种模式：`BIND_EXISTING`（绑定已有文档）、更新已有文档内容、创建新文档并关联
- **读取回退策略**：`resolveSectionDocContent` 优先返回小节绑定的主文档内容；无文档时回退 `text_content`；视频小节在无文档时回退 `media_url`

---

## 三、数据流转与交互

### 3.1 PR 流程中的 Spec & Process Guard 校验

```text
开发者/AI Agent 提交 PR（目标分支：qa/**、release/**、main、master）
     │
     ▼
CI 触发 pr-check.yml
     │
     ├─► Job 1: Spec & Process Guard
     │     │
     │     ├─► 步骤 1: Detect OpenSpec exemption
     │     │     正则 /OpenSpec-Exempt:\s*true/i + /Reason:\s*\S+/i 检测 PR body
     │     │     输出 ALLOW_OPENSPEC_EXEMPT=true/false
     │     │
     │     └─► 步骤 2: verify-ai-process.sh
     │           ├─► 检查 14 个核心文件存在且非空
     │           ├─► git diff 获取变更文件列表
     │           ├─► 若涉及 openspec/changes/<change-id>/
     │           │     强制 proposal.md、design.md、tasks.md、specs/**/spec.md 存在
     │           └─► 若无 OpenSpec 变更
     │                 若 ALLOW_OPENSPEC_EXEMPT=true → 通过
     │                 否则 → 失败
     │
     └─► Job 2: Build & Verify
           ├─► mvn clean install -DskipTests（排除 backstage-flink）
           ├─► mvn test（PaymentScenarioTest、GroupBuyingScenarioTest 等 7 类核心测试）
           └─► 验证 backstage-admin.jar 存在
```

### 3.2 OpenSpec 变更从 proposal 到 spec 的流转

```text
需求进入
     │
     ▼
判断是否需要 OpenSpec（CONTRIBUTING.md 第 9-15 行的 5 类场景）
     │
     ├─ 不需要 → PR body 声明 OpenSpec-Exempt: true + Reason
     │
     └─ 需要 → 创建 openspec/changes/<change-id>/
           │
           ▼
           proposal.md（为什么做）
           │  例：fix-group-module-quality 的 proposal 列出 21 项 P0-P3 漏洞
           │      和 initiated 状态越界严重 Bug
           ▼
           design.md（怎么做）
           │  例：四层实施方案（安全加固→类型安全→业务一致性→规范化）
           │      双状态码体系表格、分布式锁流程图、支付回调状态流转图
           ▼
           tasks.md（分步执行）
           │  例：6 个阶段 40+ 个可执行步骤
           │      准备→安全加固(P0)→权限与规范(P1)→业务一致性(P1)
           │      →枚举常量化(P2-P3)→日志与价格(P3)
           ▼
           specs/<capability>/spec.md（行为契约）
           │  例：group-security/spec.md 定义 4 类安全需求
           │      group-code-quality/spec.md 定义 7 类代码质量需求
           ▼
           按 tasks.md 实现 → 本地验证 → 提 PR → CI 校验 → Reviewer 审核 → 合并
           │
           ▼
           ADDED 行为沉淀为系统当前规格
```

### 3.3 代码审核的具体步骤

Reviewer 按 `CODE_REVIEW.md` 的 6 条 + `07-review-rubric.md` 的 7 维度逐项检查：

```text
1. 需求和规格
   → 关联 OpenSpec change？proposal 讲清楚为什么做？spec 可验收？tasks 完成？

2. 行为正确性
   → 满足 GIVEN/WHEN/THEN？覆盖异常分支？保留老行为兼容？

3. 接口兼容
   → 路径/参数/响应结构是否变更？前端是否同步？

4. 权限和安全
   → 绕过登录？越权访问？暴露敏感字段？记录敏感日志？

5. 数据和中间件
   → 事务边界正确？缓存一致性？ES 索引重建？Kafka 幂等？

6. 可维护性
   → 命名清楚？分层合理？无重复逻辑？无无关重构？

7. 验证和回滚
   → 本地验证充分？QA 验证路径清楚？回滚方案可操作？
```

`.github/PULL_REQUEST_TEMPLATE.md` 的 Reviewer Checklist 直接对应上述检查项，让 Reviewer 在 PR 页面逐项勾选。

---

## 四、预期效果与价值

### 4.1 代码质量

- **行为可追溯**：每个中大型变更都有 `proposal/design/tasks/spec` 四件套。以 `fix-group-module-quality` 为例，21 项漏洞修复均有 spec 定义的行为契约（GIVEN/WHEN/THEN），Reviewer 按 spec 验收
- **接口兼容有保障**：`course-section-doc-save-read` 变更的 design.md 明确"JSON 字段完全一致，前端无感"，配合 CODE_REVIEW.md 第 9 行"是否破坏接口兼容"的硬性门禁
- **无关重构被遏制**：CODE_REVIEW.md 第 12 行"是否存在无关重构"防止 PR 范围膨胀

### 4.2 团队协作效率

- **新人上手快**：CONTRIBUTING.md 第 7 行直接指向 `01-newcomer-guide.md` 和 `02-workflow.md`
- **AI Agent 与人类对齐**：AGENTS.md（AI）+ CONTRIBUTING.md（人类）+ CODE_REVIEW.md（审核）三者确保所有参与者围绕同一份 spec 工作
- **审核标准统一**：07-review-rubric.md 的 7 维度 20+ 检查点，避免不同 Reviewer 标准不一

### 4.3 安全性

- **凭证不泄露**：ZenTao 密码用 `SecureString` 读入；CONTRIBUTING.md 第 22 行"不提交密钥、token、日志、构建产物"；07-review-rubric.md 第 31 行"是否记录敏感日志"
- **权限变更有评审**：`fix-group-module-quality` 将 `initiableActivities` 从手写 `userLevel<=2` 改为 `@PreAuthorize`，proposal.md 明确提示"需确保角色已配置该权限"
- **防重复提交**：拼团 join/create/pay 三个接口增加 Redis 分布式锁，对应 spec 中"同一用户同一操作的并发请求必须返回友好提示"的行为契约
- **订单归属校验**：pay/getOrderStatus 接口校验 userId 匹配，防止越权操作他人订单

### 4.4 可维护性

- **分层清晰**：`OshGroupServerServiceImpl`（2159 行）虽大但职责明确——8 大功能域（查询/参团/价格计算/发起拼团/支付回调/支付流水/定时任务/辅助功能），配合 `GroupPriceCalculator` 工具类消除内联计算逻辑
- **回滚有方案**：`fix-group-module-quality` 的 design.md 明确发布步骤、验证命令、回滚步骤（回退代码 + Redis 锁 10 秒自动过期）
- **技术债务可控**：CONTRIBUTING.md 第 9-15 行明确"什么程度的改动需要走规格流程"，避免过度工程化；同时 CODE_REVIEW.md 第 12 行防止 PR 范围膨胀

---

## 五、治理体系与业务实现的协同关系

```text
治理层（流程契约）
  │
  ├─► AGENTS.md（AI Agent 入口）
  ├─► CONTRIBUTING.md（人类开发者入口）
  ├─► CODE_REVIEW.md + 07-review-rubric.md（审核出口）
  │
  ▼
执行层（CI 门禁）
  │
  ├─► pr-check.yml → verify-ai-process.sh（流程合规）
  ├─► pr-check.yml → Build & Verify（编译 + 测试）
  ├─► ensure_zentao_mcp_token.ps1（凭证安全）
  │
  ▼
实现层（业务代码）
  │
  ├─► OshGroupServerServiceImpl（拼团：分布式锁、CAS、枚举常量化）
  ├─► InternalMemberServiceImpl（内部会员：文档管理、跟进任务、成长故事）
  └─► OshDoc + OshDocRef + OshCourseServiceImpl（课程文档：多态关联、回退策略）
```

**核心逻辑**：治理层定义"做什么"和"怎么审"，执行层通过 CI 自动校验"是否合规"，实现层在业务代码中落地"具体怎么做"。三层通过 OpenSpec 变更的 `proposal→design→tasks→spec` 链式驱动，确保需求对齐、实现可验证、评审有依据、行为可溯源。
