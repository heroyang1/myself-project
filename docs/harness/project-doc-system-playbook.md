---
title: 项目文档体系搭建指南（可复用模版）
path: docs/project-doc-system-playbook.md
status: active
owner: @heroyang
created: 2026-10-10
last_updated: 2026-10-10
openspec: none
---

# 项目文档体系搭建指南

> 本文提炼自 osh-backend 2026-10 文档治理会话的实战经验（4 份新建文档 + 10 余份修改 + 3 次假规则拦截），供其他 Spring Boot 多模块项目复现同一工作流。文末附**可粘贴给 AI 的提示语模版**。

## 适用前提

- Spring Boot 单体多模块（Maven 多模块，模块间有明确依赖方向）。
- 变更管理采用 OpenSpec 类四件套（proposal / design / tasks / specs）+ CI 门禁脚本。
- 提交规范为 Conventional Commits。
- 有 AI 编码助手参与日常开发（文档的主要读者和消费者都是 AI，写法因此必须"可核验、防漂移"）。

## 一、本次完成的文档清单与落位关系

| 文件 | 定位 | 动作 | AGENTS.md 导航指针（想做什么→去看） |
|---|---|---|---|
| `AGENTS.md`（82 行） | 导航中枢 + 硬性规则 | 重构 161→82 行，正文全部外移 | —（它本身就是导航表） |
| `docs/architecture/module-dependency-rules.md` | 模块边界与依赖规则主文档 | 新建 135 行 | 模块职责/依赖链/禁止调用方向/数据权限边界/配置分工 |
| `docs/ai/conventions/coding-standards.md` | 编码规范主文档（AI 新增代码必读） | 新建 390 行 | 编码规范细则（命名/三层/响应/异常/注解/SQL） |
| `docs/reference/error-codes.md` | 错误码参考 | 扩充 76→158 行 | 异常体系与错误码 |
| `docs/plans/current-sprint.md` | 迭代计划（数据快照型） | 新建 112 行 | 了解当前迭代任务 |
| `docs/reference/api-spec.md` | API 契约入口（指向运行时 Swagger） | 修改 | API 响应约定与前端对接 |
| `docs/ai/conventions/backend.md` | 基类与注解速查 | 修改（+核心基类表+14 注解表） | 核心基类/注解速查 |
| `docs/ai/conventions/database.md` | 数据库约定 | 修改（补表前缀/必备字段/四层一致） | 表约定 |
| `docs/architecture/overview.md` / `boundaries.md` / `package-structure.md` | 模块地图 / 分层边界 / 包结构 | 修改（boundaries 删重复 bullets 改指针，消灭双源） | 对应三行导航 |
| `docs/ai/05-code-style.md` / `README.md` | 风格原则清单 / 规范入口 | 修改（顶部加"细则见 coding-standards"指针） | — |

**落位铁律**：一个事实只住一个文档；其他文档只放链接。AGENTS.md 新增内容 = 新增一行导航，而非复制正文。

## 二、文档生成方法论（四原则，每条附本次实证）

### 1. 单一事实源（SSOT）
规则只写一处，其余放指针。实证：`boundaries.md` 原有 5 条模块边界 bullets 与 pom 实测依赖链重复且部分过时 → 删除 bullets 改为指向 `module-dependency-rules.md`；`AGENTS.md` 中 C 端 Controller 位置两处描述互相矛盾 → 收敛到 package-structure.md 单源。

### 2. 先实测后成文
文档中每个类名/测试名/参数值，落笔前 Glob + Read 验证。实证（三次拦截）：
- **杜撰 CI 测试类**：初稿写"CI 的 `PageHelperStartPageCheckTest` 阻断式捕获"——Glob 实测 0 结果，凭记忆编造。降级为"评审拦截——尚无机器强制"。
- **引用死代码类**：初稿写 C 端上下文由 `UserContextInterceptor` 填充——Read 发现整个文件被逐行注释废弃，真实链路是 `OshAuthenticationFilter`。文档改正并加"已废弃勿引用"注。
- **虚构代码示例**：起草错误码用法示例时写出 `BindingResult` 手动判错模式——全仓实测 0 处使用，真实模式是 `@Validated @RequestBody`（90 处）。示例换成实测原句并标注来源行号。
- 注解默认参数也必须实测：`@RepeatSubmit` 初写"默认 10s"，读注解定义实为 `interval=5000`（5 秒）。

### 3. 强制方式如实标注
每条规则标注三级之一：**CI 阻断**（写出拦截脚本名）/ **评审拦截**（无机器强制）/ **约定**。禁止把"应该如此"写成"CI 会拦截"。实证：覆盖率目标——jacoco 实测未引入，文档不写"覆盖率须 ≥80%"（模板里的示例值），写"无工具、无强制目标、评估引入中"。

### 4. 导航优先
AGENTS.md / README 只做"想做什么→去哪里看"二列表，头部写明"勿在本文复制正文"。实证：AGENTS.md 从 161 行嵌正文瘦到 82 行纯导航，AI 每次加载成本减半且消灭了正文与目标文档的漂移面。

## 三、四类文档可复用骨架

### 1. 编码规范文档（`conventions/coding-standards.md`）

```
§1 命名规范        类命名表（Controller/Service接口I前缀/Impl/Mapper/Entity/VO/DTO）
                  方法命名（selectXxx/pageQueryXxx/canXxx/ensureXxx）、字段命名（BigDecimal 金额铁律）
§2 Controller 规范 管理端模板 + C端模板（各一段真实代码，标注来源文件）+ 注解固定顺序 + 规则清单
§3 Service 规范    接口/实现结构 + @Transactional(rollbackFor=Exception.class) 必显式 + 禁止事项
§4 Mapper 规范     接口 @Param 具名示例 + XML <sql>片段/软删除显式条件/#{} 参数化铁律
§5 实体类规范      @TableName + 基类继承 + 字段注解
§6 响应封装        三种响应体禁混用表（单对象/分页/统一包装，何时用哪个）
§7 异常处理        ✅throw ServiceException / ❌吞异常假成功 对照（真实调用点原文）
§8 注解使用        自定义注解速查表（参数默认值实测值）
§9 SQL 规范        DDL 必备字段 + 查询规则
§10 日志           放指针（勿复制，链接到 logging.md）
§11 强制方式总表   每条规则 → CI阻断(脚本名)/评审拦截/约定
```
关键条款：开头声明"所有示例从现有真实代码提炼并标注来源文件；本文未覆盖场景，模仿同模块既有代码"。

### 2. 错误码参考文档（`reference/error-codes.md`）

```
§状态码全表        常量类逐行抄录（码/常量名/含义）
§响应结构          各响应体 JSON 示例 + 失败态示例 + "HTTP 状态与 body code 的关系"铁律
§异常→码映射       按处理器分包一张表（B端/C端各一），每行=真实 @ExceptionHandler
§认证与权限错误     每场景"处理者类 + 实际返回"两列（写实测行为，如 HTTP 200 + body code:401）
§代码使用示例      抛异常/参数校验/直接返回失败态/前端消费 四段，全部取真实调用点原句
§使用约定          文字条款 + 指向 API 契约文档
```
易漏点：① 校验异常处理器（BindException/MethodArgumentNotValidException）最常被文档漏收录；② 多异常处理器作用域隔离（`@RestControllerAdvice(basePackages)` + `@Order`）导致的"作用域内消息被兜底吞掉"差异必须实测写清。

### 3. 模块边界与依赖规则文档（`architecture/module-dependency-rules.md`）

```
§1 依赖链          pom 实测模块依赖方向图（含"谁在谁之上"的反直觉点）
§2 模块内容矩阵    每模块：允许包含 / 禁止包含 / 强制方式 三列
§3 层内职责        Controller/Service/Mapper/Client 各层职责与禁止事项
§4 跨模块调用      允许方向（走接口不走实现/走 client 层）+ 域级规则（跨域注入约束、数据完整性规则）
§5 允许调用方向表  白名单
§6 禁止调用方向表  黑名单 + 违反后果（CI 阻断脚本/规则名）
§7 数据权限边界    接口鉴权面 / 行级隔离 / 字段投影 三层
§8 配置与代码      配置文件分工（哪些属保护域须 change、哪些本地覆盖、密钥禁入库）
```

### 4. 迭代计划文档（`plans/current-sprint.md`）

```
头部：数据快照时间 + 事实来源声明（git log / tasks.md 勾选率 / 自检脚本），"不虚构任务，人工条目须标注（人工排期）"
§近期已合并        git 实测表（日期/内容/分支），写清主干最后一次合并日
§当前迭代任务      进行中（change-id + 勾选率）/ 待开始（低勾选或规划件）/ 已完成待归档
§已知问题          编号表：问题/影响/处置建议（来源：落后分支、未归档 change、缺件套、技术债、未引入工具）
§迭代说明模版      内嵌 fenced markdown 模板块（front-matter + 目标/三态任务/风险/结束 checklist），新建迭代时直接复制
```

## 四、CI 门禁与文档联动（脚本实测）

以本仓库为例，其他项目可按同思路自建：

| 门禁 | 校验内容 | 对文档体系的影响 |
|---|---|---|
| `verify-ai-process.sh` | ① 17 个"AI 流程必要文件"存在且非空（AGENTS.md、docs/ai/README+01~03、openspec 模板等——**硬编码路径，重命名/搬移即 CI 红**）；② 归档守卫：archive/ 只读；③ 变更含 change 目录则四件套齐全非空；④ 保护域（pom.xml / sql/ / application*.yml）改动**必须带 change，豁免无效** | 规范文档一旦被列入必要文件清单就不可随意迁移；大改文档结构前先核对清单 |
| `code-quality-guard.sh` | 6 项 CHECK：实体发现→必填字段 Service 完整性→**四层一致（DDL/ALTER/Entity/VO）**→枚举使用→ALTER 与初始 DDL 同步→索引幽灵列→列类型 Java↔SQL 匹配 | DDL↔实体文档（database.md）写的规则有机器背书，可标"CI 阻断"；VO 字段文档同理 |
| `pr-check.yml` build | `mvn clean install -DskipTests` + 核心测试套件 + ArchUnit 分层规则 | 文档中"分层方向"条款可引 ArchUnit 类名作强制背书 |
| OpenSpec 豁免通道 | PR 正文 `OpenSpec-Exempt: true` + `Reason:` 且未碰保护域 | 纯文档小改走豁免；**触碰 CI workflow/脚本属保护域相邻改动，建议建 change** |

联动心法：**文档条款的"强制方式"标注，以脚本里能找到的对应拦截逻辑为准——找不到就如实写"评审拦截"**。

## 五、分步实施清单

1. **评估现状**：对每个目标文档类型跑"是否存在 + 逐条对照内容要求"矩阵（存在≠合格，重点找"文档滞后于代码"的事实错误，如映射表漏收录的异常处理器）。
2. **确定导航表**：先定 AGENTS.md 的"想做什么→去哪里看"行（这定义了后续所有文档的落位坐标），此时 AGENTS.md 允许暂嵌正文，最后一轮抽干。
3. **逐类沉淀规范**：每类文档按"采集真实代码样例（Grep 统计主流模式）→ 套 §三骨架成文 → 逐条标注强制方式 → 修正分析发现的假规则"推进；一次一类，一类一验收。
4. **挂导航 + 收敛双源**：新文档挂入 AGENTS.md 表；被抽走内容的旧文档改成指针（如 boundaries.md 删 bullets）。
5. **最终 AGENTS.md 抽干**：删除全部嵌正文，只留导航表 + 无法外移的硬性规则简述。
6. **沉淀记忆/交接**：把每份文档的"结构 + 实测参数基线 + 已知陷阱"存为长期记忆或交接文档，注明复核日期。

## 六、常见陷阱与避坑指南

| 陷阱 | 实例 | 避法 |
|---|---|---|
| 杜撰 CI 测试类/脚本名 | `PageHelperStartPageCheckTest` 不存在 | 引用任何类名前 Glob 验证；引用拦截逻辑前打开脚本找到对应行 |
| 引用死代码类 | `UserContextInterceptor` 全文被注释仍被当活跃链路 | 不仅验证文件存在，还要 Read 确认未被整体注释；用 javadoc 交叉验证真实调用方 |
| 虚构"主流模式"示例 | `BindingResult` 判错示例 vs 全仓 0 处使用 | 示例成文前 Grep 统计该模式真实使用次数，取调用点原句并标注来源 |
| 照抄外部模板的示例值 | 模板写"覆盖率≥80%"，项目根本没接 jacoco | 模板中的版本号/路径/阈值一律视为待核示例值 |
| 注解参数凭印象 | `@RepeatSubmit` 默认写 10s 实为 5s | 读注解定义源码的 `default` 值 |
| 异常处理器分包差异未写 | C 端作用域内校验消息被 Exception 兜底吞成"系统错误" | 逐一列出每个 `@RestControllerAdvice` 的 basePackages + @Order + 处理的异常类全集 |
| 双源漂移 | 模块边界 bullet 在两个文档各写一份且互相矛盾 | 新事实只落目标文档一处，其他位置只放链接；改写时问"这条会住哪个单源" |
| 错别字/繁体混入 | "兑底"（应为"兜底"）、"个別" | 中文转义字符易错，成文后专查一遍字形 |
| 虚构迭代数据 | 编造任务清单与排期 | 任务状态只从 tasks.md 勾选率/git log 取，人工条目显式标注（人工排期） |

## 七、可复用提示语模版（粘贴给 AI 复现工作流）

```text
请为我的 Spring Boot 多模块项目搭建治理文档体系。必须遵守四条纪律：
1. 单一事实源：每条规则只写入一个目标文档，其余文档只放链接指针，禁止复制正文；
2. 先实测后成文：写入任何类名/测试名/注解参数/代码示例前，必须搜索验证其存在、
   统计真实使用模式并取原句，禁止凭记忆或外部模板示例值编造；文件存在还要确认未被注释废弃；
3. 强制方式如实标注：每条规则标明属于「CI 阻断（写出拦截脚本与规则名）」「评审拦截」
   还是「约定」；无机器强制就不得写成 CI 强制；
4. 导航优先：AGENTS.md 只维护「想做什么→去哪看」二列导航表，具体内容分发到目标文档。

待建文档（按此顺序，每份先给我八项内容对照分析、再给 ≥2 个落地方案供我拍板、后成文）：
① 模块边界与依赖规则（依赖链/内容矩阵/层内职责/调用方向白黑名单/数据权限/配置分工）
② 编码规范主文档（命名/Controller/Service/Mapper/实体/响应封装/异常/注解/SQL/日志，全部带真实代码示例）
③ 错误码参考（状态码全表/响应结构 JSON/异常→码映射含校验异常/认证实际行为/代码使用示例/前端消费）
④ 迭代计划（近期已合并 git 实测/三态任务从 change 勾选率取/已知问题/迭代模版，禁止虚构任务）

成文后：挂 AGENTS.md 导航、把被抽走内容的旧文档改为指针、核对 CI 脚本清单避免重命名断链、
输出本轮"文档假设 vs 实测结论"差异清单与复核日期。
```
