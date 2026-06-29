# Tasks: redeem-code-resource-search

## 准备

- [x] 确认课程、电子书、工具、会员套餐资源编号、名称、价格字段。
- [x] 确认核销码生成不再预绑定消费者，使用时再绑定当前用户。
- [x] 确认资源搜索需要补充的 MySQL 索引。

## 实现

- [x] 新增核销资源搜索 DTO、Mapper、XML。
- [x] 新增 `POST /pc/admin/redeem/resource/search` 接口。
- [x] 搜索接口限制 level>=5。
- [x] `redeem_code.sql` 追加资源搜索与编号定位索引。
- [x] 保持创建核销码接口继续使用内部 `resourceNo` 契约。
- [x] 创建核销码接口去除用户邮箱定位逻辑，生成未绑定的通用核销码。
- [x] 新增 `POST /pc/admin/redeem/code/batch-create` 批量生成核销码接口。
- [x] 批量生成接口返回逐条成功/失败结果，并限制单次最多 200 条。
- [x] 用户中心核销接口支持手动输入 `code`，仅校验未使用/未作废，成功后绑定当前用户。
- [x] `redeem_code.sql` 追加 `osh_redeem_code.user_id` 可为空迁移。
- [x] 新增 `osh_redeem_invite_link` 表与实体，核销码生成时创建一一对应邀请链接。
- [x] 新增匿名邀请链接查询接口和登录后核销码解析接口。
- [x] 注册提交支持可选 `redeemInviteToken`，有效链接复用邀请注册关系。
- [x] 新增包含唯一标识、核销码和资源名称的增强注册邮件模板。
- [x] `redeem_code.sql` 追加历史核销码邀请链接回填。

## 验证

- [x] 后端构建通过。
- [x] 远端 MySQL 索引存在。
- [x] 资源搜索 SQL 可返回候选资源。
- [x] 前端构建通过。
- [ ] 核销邀请链接注册邮件 smoke 验证。

## PR

- [ ] PR 描述关联 OpenSpec change。
- [ ] 填写测试结果。
- [ ] 填写风险与回滚。
