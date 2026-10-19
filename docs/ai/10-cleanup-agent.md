# 后台清理 Agent 定时任务模板

代码已迭代多版，不少文档与技术债未随代码更新。本模板定义一个周期性运行的**代码库卫生清理 Agent**：每次执行把发现的问题按"一类问题一个 PR"的方式，生成**独立的**修复 PR，让仓库具备自我验证、自我修复能力。

建议节奏：每 1~2 周手动把下方提示词粘贴给 Agent 跑一轮（暂不接 cron）。

## 任务提示词（复制给 Agent）

```text
# 任务：代码库卫生清理

对 osh-backend 执行以下检查，对每个发现的问题生成独立的修复 PR（一个 PR 只修一类问题）。
执行前先读 AGENTS.md、docs/ai/README.md、openspec/project.md，并确认当前分支与工作区无未提交改动。

## 检查清单
1. 超长文件：找出 backstage-*/src/main/java/ 下超过 300 行的 .java 文件，评估能否拆分为更小的类。
   （>300 行仅为非阻断告警、不是硬门禁；拆分必须保证对外行为不变。）
2. 缺失测试：找出核心业务类（service / mapper 关键路径）缺少对应 *Test 的，补充有价值的基础测试，
   不刷无意义断言。
3. 未使用 import：清理各模块未使用的 import 语句。
4. TODO/FIXME：列出所有 TODO 和 FIXME，标出超过 30 天未处理的，逐条评估后再清理（不盲目删除）。
5. 重复代码：找出高度相似的代码段（>10 行），评估提取为共享工具（放 backstage-common 或对应 *Helper）；
   动公共层须先建 OpenSpec change。
6. 过时文档：检查 docs/design/ 中 git 最后更新超过 180 天的设计文档，核对是否已与现状不符，
   更新或在文首标注"已过时"。
7. 基线与告警清理：清理 ArchUnit FreezingArchRule 基线（backstage-admin/archunit-store）中已修复的历史违规；
   处理 pr-check.yml 累积的非阻断告警（文件 >300 行）。

## 约束（每个 PR 必须满足）
- 每个修复作为独立 PR，不要混在一起。
- 每个 PR 修改后必须确保构建与测试通过：
    mvn clean install -DskipTests -pl '!backstage-flink'
    mvn -B test -pl backstage-admin -am -Dtest='LayeredArchitectureTest,CodingConventionArchitectureTest'
  （核心业务测试套件见 .github/workflows/pr-check.yml build job，改动涉及时一并跑。）
- PR 标题格式：chore(cleanup): [具体描述]
- 不允许使用 Java 9+ 语法（record / var / text blocks），保持 Java 8 运行语义（CI 用 JDK 17 构建）。
- 不允许升级 Spring Boot 主版本（保持 2.5.15）；不允许擅自升级任何技术栈基线（见 AGENTS.md）。
- 改动只限当前清理项，保留用户本地修改，不提交密钥 / token / 日志 / 构建产物。
- 若某修复不确定是否安全，跳过并在 PR 描述中标注原因。
- 涉及接口 / 权限 / 数据 / 中间件 / 部署 / 存储的清理必须走 OpenSpec change；
  纯注释 / import / 文档类小改动可写 OpenSpec-Exempt: true + Reason。
```

## 与通用模板的差异（本项目适配）

| 通用模板写法 | 本项目实际 | 原因 |
|---|---|---|
| 保持 Spring Boot 2.7.x | 保持 **2.5.15** | 项目基线是 2.5.15，不可升 3.x，也不擅自升小版本 |
| `mvn -B clean verify` | `mvn clean install -DskipTests -pl '!backstage-flink'` | 仓库规范构建命令；`verify` 不会跑 Checkstyle/SpotBugs/JaCoCo（根 pom 未接入这些插件） |
| `src/main/java/` | `backstage-*/src/main/java/` | 9 模块 Maven 工程，无根 `src/` |
| 第 6 条按 "status: Draft" 查 | 按 git 最后更新时间 >180 天查 | `docs/design/*.md` 无 front-matter 状态头 |
| 第 7 条清 Checkstyle/SpotBugs 告警 | 清 ArchUnit 冻结基线 + pr-check 非阻断告警 | Checkstyle 仍是"待接入资产"，SpotBugs 未引入 |

## 自我验证与自我修复闭环

- **自我验证**：每个清理 PR 必须过构建 + ArchUnit 阻断门禁（CI 已强制，见 `pr-check.yml`），不通过即不合并。
- **自我修复**：清理 Agent 周期性消化两类"欠账"——`pr-check.yml` 累积的非阻断告警（文件 >300 行、文档过期），以及 ArchUnit `FreezingArchRule` 冻结基线里已修复的历史违规（从 `archunit-store` 移除）。
- **约定固化**：遵循"一条约定在 Code Review 被提过 3 次以上就固化成 Linter 规则"的经验法则，把重复出现的人工意见逐步沉淀为 `config/checkstyle/checkstyle.xml` 与 ArchUnit 规则。

相关：[Codex Harness 使用规范](03-codex-harness.md)、[标准包结构](../architecture/package-structure.md)、[日志规范](conventions/logging.md)、[HTTP 调用规范](conventions/api-calls.md)。
