# Proposal: plan-microservice-split-roadmap

## 背景

`osh-backend` 当前为 RuoYi 3.9.0 单体多模块架构，核心业务集中在 `backstage-system`（1296 个 Java 类、37 个业务域：课程/考试/拼团/秒杀/订单/支付/会员/电子书/社区/工具/审核等）。所有域共享一个进程，导致：

- 秒杀/拼团高峰与课程浏览、后台管理互相争抢资源，一个域的故障会拖垮全站。
- 任何改动都需全量构建、全量发布，发布风险与业务体量成正比。
- 团队无法按业务域并行开发，代码边界依赖自觉而非物理隔离。

`openspec/project.md` 已明确"按业务边界演进到微服务"的方向，本变更是该项工作的总纲（Roadmap Charter），固化拆分策略、服务边界、技术决策与各阶段 change 规划，本身不含代码改动。

## 目标

- 确立绞杀者模式（Strangler Fig）渐进拆分策略：新服务逐步接管老单体流量，全程可灰度、可回滚、不停机。
- 确立粗粒度 7 服务边界：gateway-service、auth-user-service、course-service、trade-service、content-service、tool-service、platform-service，每个服务的数据归属与远期裂变方向见 design.md。
- 确立四项关键技术决策（已由需求方拍板确认）：
  1. 拆分策略：绞杀者渐进式；
  2. 服务粒度：粗粒度 7 服务，成熟一个再裂变一个；
  3. 同步 RPC：OpenFeign（HTTP+JSON，契约以独立 `xxx-api` Maven 模块发布）；
  4. 分布式事务：Outbox 可靠消息最终一致 + 跨服务长流程 SAGA 补偿，不引入 Seata AT。
- 规划 Phase 0~4 实施路线与对应的 OpenSpec change 清单，作为后续所有 `split-*` / `upgrade-*` change 的立项依据。

## 非目标

- 本次不改动任何代码、配置、数据库或部署行为（纯规划文档 change）。
- 本次不拆分 backstage-admin 系统管理（菜单/字典/日志/监控）与 backstage-generator，二者保留在单体中。
- 本次不调整 backstage-flink / backstage-hbase 的独立作业形态，仅在后续 change 中约定其 topic/索引归属。
- 不在本 change 内执行 JDK/Spring Boot 升级（由 `upgrade-jdk17-springboot27` 承接）。

## 用户影响

- 对终端用户：拆分全程 URL、参数、响应结构不变，前端/App/小程序零感知。
- 对开发者：后续所有涉及跨域调用、数据归属、事件发布的 change 必须以本总纲的服务边界为准；禁止绕过边界直接跨域查库。
- 对历史数据/缓存/权限：本 change 无影响；各阶段 change 需遵循"一表一主、锁 key 冻结、LoginUser key 冻结"三原则。

## 风险

- 兼容性风险：路线图周期长（约 6-9 个月），期间业务需求与拆分工作并行，可能产生 change 冲突。缓解：拆分 change 与业务 change 分支隔离，每个 Phase 串行推进一个 split。
- 数据风险：Phase 4 物理分库涉及双写与对账。缓解：共库期（Phase 1~3）只切换写权限归属不迁数据；分库走"双写→对账→切读→断写"四步，每步可回滚。
- 性能风险：跨服务调用引入网络开销。缓解：粗粒度 7 服务将跨服务事务降到最低；读多场景用事件冗余字段替代跨服务 JOIN。
- 安全风险：网关鉴权与服务内鉴权职责划分不当可能出现权限绕过。缓解：延续现有 C 端/B 端权限体系教训，权限字符串冻结，网关粗校验 + 服务内 @PreAuthorize 细校验双层兜底。
- 回滚风险：本 change 为纯文档，无需回滚；后续各 change 按 design.md 约定三级回滚（路由级/数据级/版本级）。

## 验收标准

- [ ] 本 change 四件套（proposal/design/tasks/specs）齐备并通过评审。
- [ ] design.md 包含：7 服务边界表（职责/承接域/数据归属/远期裂变）、四项技术决策记录、Phase 0~4 路线图、change 映射清单、三级回滚策略。
- [ ] specs/architecture-boundary/spec.md 以 GIVEN/WHEN/THEN 定义服务边界与数据归属的可验收约束。
- [ ] Phase 0 三个前置 change（upgrade-jdk17-springboot27、add-gateway-service、add-observability-stack）已建立目录并各自四件套齐备。
- [ ] `openspec/changes/README.md` 或团队知晓渠道确认后续 `split-*` change 命名规范。

## 关联信息

- 禅道任务：待创建
- PR：待提交
- 设计稿：design.md（本 change 内）
- 参考：`openspec/project.md` 升级原则七要素；`docs/ai/02-workflow.md`；已有 Outbox/三重闸门/分布式锁实践（docs/design/）
