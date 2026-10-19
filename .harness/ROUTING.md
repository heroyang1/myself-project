---
title: 上下文路由表
path: .harness/ROUTING.md
status: active           # 表1/表2 已填，表3 骨架待填
owner: @heroyang
created: 2026-10-18
interface: CLAUDE.md 顶部索引句 · .harness/candidate/README.md
---

# ROUTING.md —— 任务开始前先查这里

> 职责：把"该加载什么"从 AI 临场发挥（边做边找）收敛为**动手前查表**。
> 三种路由键各一张表：①按任务意图 ②按碰到的文件 ③按需求所处阶段。
> 本文件是路由层：只登记映射关系，不写任何规则正文；CLAUDE.md 只留一句索引指过来。

## 五步定位法（任何任务动手前必须走完 · 本方法的唯一住所就是本文件）

> **这五步的目的不是让 AI 更聪明，是让它更慢——慢在动手之前。**
> 它的默认惯性是「直接开始写」，这五步就是那道减速带；五种跳步各放行一种病。

| # | 做什么 | 做完的产出（可验收） |
|---|---|---|
| 1 **查路由** | 查本文件表 1，确定本次任务类型 | 任务类型一句话 + 按表 1 得到的本次加载清单 |
| 2 **定边界** | 确认本次只动哪个模块/域，划出改动半径 | 允许改动的路径清单（一句话：本次只动 X/Y，其余不碰） |
| 3 **取事实** | 读 `.harness/wiki/` 对应层 + 目标枚举/实体/配置源文件，拿当前真实状态 | 可复述的事实清单（状态值/表结构/路径均来自实读，不许靠记忆和推测） |
| 4 **找先例** | 去 `openspec/changes/` 翻同类变更，看上回怎么做的；修 bug 另查 `.harness/wiki/事故复盘.md` 编号禁令 | 可参照先例清单，或显式声明「无先例」 |
| 5 **验契约** | 检查 `openspec/changes/<id>/` 有无现成四件套契约；没有先建（本仓库暂无 /spec-analyzer，以 `openspec/templates/` 代替，命中 R2/R3 门禁时必走） | 确认的 change-id，或「契约已存在」结论 |

## 表 1 · 任务类型路由（任务类型 → 该加载的 Skill / 文档）

| 任务类型 | 加载（Skill / 文档） | 备注 |
|---|---|---|
| 中大型新功能 / 影响行为的改动 | `openspec/templates/` 四件套 → 按模板立 change | 存量 |
| 修 bug / 改并发逻辑 | `.harness/wiki/事故复盘.md`（编号禁令→案例集全文） | 知识库L5·必读 |
| 改表结构 | `docs/ai/conventions/database.md` + DDL 守卫（见第 4 层 04）；表关系速查 `.harness/wiki/数据模型.md` | 指针 |
| 跨模块调用 / 新依赖 | `docs/architecture/module-dependency-rules.md` | 存量 |
| 需求分析 / 改状态机 / 库存逻辑 | `.harness/wiki/业务模型.md` | 知识库L1 |
| 写/改 Controller / 联调 / 返回码不对 | `.harness/wiki/接口协议.md` | 知识库L2 |
| 任何会话开始（多义词核对） | `.harness/wiki/领域术语.md` | 知识库L4·普适 |
| （待填：后续逐讲补） | | |

> 填表来源：`.harness/candidate/` 经人确认的条目 + 第 9-12 讲逐讲填充。
> 本仓库现有机器能力清单见 CLAUDE.md 第 4 层（勿在此复制，避免双源漂移）。

## 表 2 · 文件路径路由（文件 glob → 规则文件）

| 文件 glob | 规则文件 | 装载方式 |
|---|---|---|
| `**/*.java`（新增代码） | `docs/ai/conventions/coding-standards.md` | 手动 |
| `sql/**` | `docs/ai/conventions/database.md` | 手动 |
| `**/controller/**` | `docs/reference/api-spec.md` | 手动 |
| （待填：自动通道建成后迁 paths） | | |

> 现状说明：本仓库尚未建立 `.claude/rules/*.md` 的 paths 自动装载通道，
> 入表条目暂按"手动·命中即读"执行；自动通道建成后，高频短规则再迁自动。

## 表 3 · 9-Skill 管道（一个需求从进到出的固定流水线）

| # | 阶段 | Skill | 何时触发 | 产物 | 质量门禁 |
|---|---|---|---|---|---|
| 1 | 契约确认 | verify-prd | （待填） | | |
| 2 | 规格分析 | spec-analyzer | （待填） | | |
| 3 | 迁移架构 | flyway-architect | （待填） | | |
| 4 | 测试守护 | tdd-guard | （待填） | | |
| 5 | 安全审计 | security-audit | （待填） | | |
| 6 | 三步审查 | three-step-review | （待填） | | |
| 7 | 门禁 | （机器拦截） | （待填） | | |
| 8 | 基线对比 | （存量冻结） | （待填） | | |
| 9 | 归档 | archiver | （待填） | | |

> 第 9-12 讲逐段填。与本仓库既有实物的映射（先登记、不展开）：
> 门禁 = pr-check.yml 六项 CI；基线对比 = `backstage-admin/archunit-store/`
> （FreezingArchRule 存量冻结）；归档 = `openspec/changes/` → archive/ 流程。

## 本文件纪律

- ROUTING 自身受预算约束：全文 ≤150 行，每行只允许"键 → 加载项"一个映射，
  规则解释一律留在目标文档里（本文件不是规则正文的家）。
- 入表门槛：只收经人确认的条目（来源 `.harness/candidate/` 晋升，或人直接指定）；
  AI 不得自行往表里加行——与 candidate 规则 3 同一拍板权。
- 删比加常态：目标文档合并/改名时，本表对应行必须同批更新或删行（幽灵指针比没有指针更坏）。

---

**没有走完五步定位法，不许开始写代码。**
