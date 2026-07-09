# Proposal: contentbg-management-and-order-delete-guard

## 背景

当前分支同时引入了两类后端行为变更：

1. 新增内容背景图上传、切换、管理、删除相关能力，并约束高等级用户才能操作管理功能。
2. 收缩订单相关删除能力，避免订单数据在业务链路中被直接删除，改为只允许查询、流转和状态管理。

这两类改动都会影响接口行为、权限边界和用户可见流程，不属于可以靠 PR 文本豁免的小改动，因此需要补齐后端 OpenSpec change，作为本次 PR 的需求事实来源。

## 目标

- 为内容背景图能力补充后端规格说明，明确匿名读取、登录后个性化覆盖、等级 5 上传/管理/删除的规则。
- 为订单相关删除逻辑收缩补充规格说明，明确订单不再提供直接删除路径。
- 让本次 PR 满足仓库 `Spec & Process Guard` 对 OpenSpec change 的要求。

## 非目标

- 本次不重写前端 OpenSpec 文档，前端已有的 `osh-frontend/openspec/**` 仍保留作为前端实现记录。
- 本次不新增订单归档、软删除或数据清理替代流程。
- 本次不调整对象存储底层实现策略，只描述本次接口与权限边界。

## 用户影响

- 未登录用户可以读取系统默认内容背景图；登录用户优先读取自定义背景图。
- 等级 `< 5` 的用户不能上传、管理或删除背景图。
- 订单相关接口不再允许删除订单数据，避免误删业务记录。

## 风险

- 兼容性风险：中。背景图接口新增删除能力，同时订单删除能力被移除，调用方需要同步预期。
- 数据风险：中。背景图删除采用逻辑删除；若用户当前正使用被删图片，需要正确回退默认背景或空背景。
- 性能风险：低。背景图管理主要是小文件上传/列表读取，对存量链路影响有限。
- 安全风险：中。背景图管理必须严格卡在等级 5 以上，订单删除必须彻底下线。
- 回滚风险：中。若回滚，需要同时恢复旧接口行为和权限判断。

## 验收标准

- [ ] OpenSpec change 完整包含 `proposal.md`、`design.md`、`tasks.md`、`specs/**/spec.md`。
- [ ] 内容背景图读取/上传/删除/切换的接口和权限规则在 spec 中有清晰描述。
- [ ] 订单删除逻辑移除后的行为在 spec 中有清晰描述。
- [ ] 本次 PR 通过 `Spec & Process Guard`。

## 关联信息

- PR：#307
- 分支：`feature/tool_yixingren`
- 相关模块：
  - `backstage-system/src/main/java/com/backstage/system/controller/contentbg/**`
  - `backstage-system/src/main/java/com/backstage/system/service/contentbg/**`
  - `backstage-system/src/main/java/com/backstage/system/service/order/**`
