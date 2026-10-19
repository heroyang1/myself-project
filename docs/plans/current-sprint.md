---
title: 当前迭代计划（Current Sprint）
path: docs/plans/current-sprint.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-10
last_updated: 2026-10-10
openspec: none          # 关联的 change-id；无则 none
---

# 当前迭代计划

> **数据快照时间：2026-10-10**。本文事实来源：`git log`（origin/qa/20260708、origin/release/20260708 主线）、`openspec/changes/*/tasks.md` 勾选进度、`scripts/preflight.ps1` 自检输出——**不虚构任务**，人工新增条目须标注"（人工排期）"。
> 迭代结束时按文末模版新建 `docs/plans/sprint-<yyyymmdd>.md` 并回填本文。

## 一、近期已合并（主线，git 实测）

| 日期 | 内容 | 分支 |
|---|---|---|
| 2026-08-16 | `feat: store poetry full text in elasticsearch`（诗词全文入 ES） | qa/20260708 |
| 2026-08-16 | `feat: add storage registry and poetry importer`（存储注册表 + 诗词导入器） | qa/20260708 |
| 2026-08-16 | `fix: point deploy workflows at current OSH servers`（部署 workflow 指向现役服务器） | qa/20260708 |
| 2026-08-05 | `fix: harden 92ydl download link extraction` + `ci: 资源中心导入加固 / jar 原子部署` | qa/20260708 |
| 2026-07-30 | 每日新闻模块（`codex/daily-news-20260730`） | 特性分支 |
| 2026-07-11 | 拼团模块系统性优化 P0–P3（CAS 索引/UNION 拆分/价格接口化）· 统一支付宝收单 · 内容背景管理 · 订单删除守卫 | course_hero → qa |

> 注：主线自 2026-08-16 后无新合并记录（截至快照日）。

## 二、当前迭代任务

### 进行中（openspec change，按 tasks.md 勾选率）

| change-id | 进度 | 域 |
|---|---|---|
| `fix-group-module-quality` | 43/47 | 拼团质量治理 |
| `redeem-code-resource-search` | 22/26 | 核销码/资源搜索 |
| `add-object-storage-slimmer` | 23/29 | OSS 视频瘦身 |
| `book-bookmark-enhancement` | 23/27 | 电子书书签 |
| `product-bundle-management` | 14/18 | 套餐管理 |
| `course-bundle-payment-qr` | 12/17 | 课程套餐支付二维码 |
| `protect-book-comic-assets` | 12/15 | 书漫资源保护 |
| `refactor-internal-member-paid-role-source` | 12/16 | 付费用户角色来源重构 |
| `unify-alipay-payment-only` | 11/15 | 支付统一（代码已并线，任务未勾满） |
| `fix-pc-endpoint-cside-auth` | ⚠️ 本地进行中，**缺 proposal/design/spec 三件**（preflight 报告） | C 端鉴权修复 |
| （本会话治理工作，未建 change） | AGENTS.md 导航化 + coding-standards.md + module-dependency-rules.md + 测试规范分析——**触碰 CI 域，提交前须建 change** | 工程治理 |

### 待开始（勾选率 ≤10% 或 0/24+）

| change-id | 进度 | 说明 |
|---|---|---|
| `split-tool-service` | 4/52 | 工具服务拆分（微服务演进第一刀） |
| `add-observability-stack` | 0/25 | 可观测性栈（Prometheus/Grafana） |
| `add-gateway-service` | 0/23 | 网关服务 |
| `upgrade-jdk17-springboot27` | 0/21 | JDK17/SB2.7 升级（须过 CI 构建基线） |
| `add-prod-nacos-config` | 2/24 | 生产 Nacos 配置落地 |
| `plan-microservice-split-roadmap` | 0/19 | 拆分路线图（规划件） |

### 已完成（100% 勾选，**待归档** → 移入 `openspec/changes/archive/`）

`add-internal-member-admin-user-options` · `book-announcement-backend` · `course-announcement` · `feedback-enhancement` · `fix-book-content-access-control` · `fix-book-vip-payment-access` · `fix-current-compilation-regressions`

## 三、已知问题（本迭代需消化）

| # | 问题 | 影响 | 处置建议 |
|---|---|---|---|
| 1 | 本地 `feature/course_hero` 停在 07-11，落后 `origin/qa/20260708`（08-16）约 5 周 | 基于旧基线开发会撞冲突 | 新工作一律从最新 qa/* 切出；存量改动先 `preflight.ps1` 再 rebase |
| 2 | 7 个 change 已 100% 但未归档，归档区为空 | 归档守卫（本周期刚建）首次实跑未执行 | 逐个 PR 移入 archive/，验证 `verify-ai-process.sh` 归档守卫 |
| 3 | `fix-pc-endpoint-cside-auth` 四件套缺 3 件 | PR 必被 Spec Guard 拦截 | 补齐 proposal/design/spec |
| 4 | 本会话治理改动（CI workflow/脚本/文档）未提交且未建 change | 触碰保护域，豁免无效 | 建 `enhance-harness-governance` change 一并提交 |
| 5 | `bootstrap.yml` 含 Nacos 明文口令 | 泄露风险（gitleaks 只拦新增） | 迁移至环境变量/Nacos 加密项，另轮换口令 |
| 6 | ArchUnit `archunit-store` 冻结基线未生成提交 | FreezingArchRule 在 CI 首跑行为未定 | 本地 `mvn test -pl backstage-admin -am` 生成后随治理 change 提交 |
| 7 | gitleaks 组织账户需 `GITLEAKS_LICENSE` secret | 未配置则 Secret scan 步骤失败 | 仓库 Settings → Secrets 配置 |
| 8 | 无覆盖率工具（jacoco 未引入），覆盖率目标目前无任何文档承载 | 覆盖率无法度量与设定 | 评估引入 jacoco + CI 报告；测试规范体系化（补 Controller/Mapper/断言规范缺口）另建 change（人工排期） |

## 四、迭代说明模版（新建迭代时复制）

````markdown
---
title: 迭代计划 <sprint 名称>
path: docs/plans/sprint-<yyyymmdd>.md
status: active
owner: @<负责人>
created: <yyyy-mm-dd>
last_updated: <yyyy-mm-dd>
openspec: <关联 change-id 或 none>
---

# 迭代计划：<名称>（<开始日期> ~ <结束日期>）

## 迭代目标（1-3 条，可验收）
- GB: <本迭代结束时可演示/可度量的结果>

## 进行中
| change-id / 任务 | 负责人 | 进度 | 备注 |

## 待开始
| change-id / 任务 | 负责人 | 优先级 | 依赖 |

## 本迭代已完成
| 内容 | 合并分支 | 关联 change |

## 已知问题 / 风险
| # | 问题 | 影响 | 处置 |

## 迭代结束动作（checklist）
- [ ] 100% 的 change 归档至 `openspec/changes/archive/`
- [ ] 更新 `docs/plans/current-sprint.md` 指向下一迭代
- [ ] 未完成 change 写明顺延原因与新区间
- [ ] 回顾：本迭代新增的"评审拦截"规则是否可机器化（ArchUnit/CI）
````

> 模版字段与 CI 门禁对齐：任务行必须能对应一个 `openspec/changes/<id>` 或标注"（人工排期）"，防止聊天记录需求（见 [AGENTS.md 硬性规则 1](../../AGENTS.md)）。
