# Spec: harness-wiki-knowledge-base

## ADDED Requirements

### Requirement: 五层知识库文件齐备且自带装载时机

系统 SHALL 在 `.harness/wiki/` 下维护五个文件（业务模型/接口协议/数据模型/领域术语/事故复盘），
每个文件第一区块 SHALL 为一行"装载时机"，说明什么任务下需要读它。

#### Scenario: 按任务类型定位知识层

- GIVEN 开发者/AI 接到"修并发 bug"任务
- WHEN 查 ROUTING.md 表 1
- THEN 存在"修 bug / 改并发逻辑 → `.harness/wiki/事故复盘.md`"路由行
- AND 目标文件首区块含装载时机声明

### Requirement: 领域术语层重点收录词义陷阱

领域术语.md SHALL 以对照表收录"项目含义与通用含义不一致"的词，
每条 SHALL 含：通用误解、项目真义、误读后果、代码证据。

#### Scenario: 核对多义词

- GIVEN AI 在会话中遇到"用户"一词
- WHEN 读取领域术语.md §1
- THEN 能查到 OshUser≠SysUser 双身份及误读后果，且附代码证据列

### Requirement: 知识库与正文事实源不双源

wiki 各文件 SHALL 只做装载式摘要与指针，正文唯一住所保留在 docs/ 与代码；
事故复盘层 SHALL 以"编号+一句话禁令+案例集指针"形态存在，不复制案例全文。

#### Scenario: 审查双源

- GIVEN wiki 五文件落盘
- WHEN 对照 `docs/analysis/典型技术缺陷与优化案例集.md` 与 `docs/reference/error-codes.md`
- THEN wiki 中不存在被整段复制的正文，仅有摘要句与指针

## MODIFIED Requirements

### Requirement: 宪法命名口径段瘦身

CLAUDE.md 第 1 层命名口径 SHALL 压缩为三条最critical铁律加指针
（指向 `.harness/wiki/领域术语.md`），细节词条唯一住所迁至领域术语层；
压缩后 CLAUDE.md 总行数 SHALL 保持 ≤100。

#### Scenario: 词条迁移后语义完整

- GIVEN 命名口径段压缩完成
- WHEN 检查被迁出词条（表前缀/实体命名/服务接口命名）
- THEN 在领域术语.md 或既有 conventions 中可查到，无信息丢失

## REMOVED Requirements

无。
