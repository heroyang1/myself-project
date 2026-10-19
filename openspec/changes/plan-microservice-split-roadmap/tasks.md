# Tasks: plan-microservice-split-roadmap

## 准备

- [ ] 阅读 `openspec/project.md`（升级原则七要素、架构边界）
- [ ] 阅读 `docs/ai/02-workflow.md` 与 `docs/ai/conventions/*.md`
- [ ] 确认四项技术决策（绞杀者/7服务/OpenFeign/Outbox+SAGA）已由需求方拍板

## 实现（本 change 为规划文档，无代码）

- [ ] 评审 design.md 服务边界表：37 个域到 7 服务的映射无遗漏、无重叠
- [ ] 评审数据归属表：每张 osh_*/sys_* 表有且仅有一个写权限归属服务
- [ ] 建立 Phase 0 三个 change 目录：upgrade-jdk17-springboot27、add-gateway-service、add-observability-stack
- [ ] 在 design.md 中固化四类契约冻结清单（URL、权限字符串、Redis 锁 key、LoginUser key）
- [ ] 约定后续 split-* change 命名与 design.md 强制章节（契约/数据归属/事件清单/灰度回滚/兼容性矩阵）

## 后续 change 排期（不在本 change 执行）

- [ ] Phase 1：创建 split-tool-service、split-platform-service
- [ ] Phase 2：创建 split-content-service、split-course-service、split-auth-user-service
- [ ] Phase 3：创建 split-trade-service（含 group/seckill 裂变设计）
- [ ] Phase 4：创建 split-database-per-service

## 验证

- [ ] Spec & Process Guard CI 通过
- [ ] 本 change 四件套齐备（proposal/design/tasks/specs/architecture-boundary）
- [ ] 团队评审会议通过服务边界宪章（记录到 design.md 评审结论）

## PR

- [ ] PR 描述关联本 OpenSpec change
- [ ] 填写评审结论与遗留问题
- [ ] 标注"纯文档变更，无部署动作，回滚=revert"
- [ ] 指派 Reviewer
