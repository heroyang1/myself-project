# Tasks: redeem-code-resource-search

## 准备

- [x] 确认课程、电子书、工具、会员套餐资源编号、名称、价格字段。
- [x] 确认 `osh_user.email` 唯一索引可用于后台邮箱定位消费者。
- [x] 确认资源搜索需要补充的 MySQL 索引。

## 实现

- [x] 新增核销资源搜索 DTO、Mapper、XML。
- [x] 新增 `POST /pc/admin/redeem/resource/search` 接口。
- [x] 搜索接口限制 level>=5。
- [x] `redeem_code.sql` 追加资源搜索与编号定位索引。
- [x] 保持创建核销码接口继续使用内部 `resourceNo` 契约。
- [x] 用户中心核销接口支持按本人核销记录 `id` 确认或手动输入 `code`，并统一校验归属与待核销状态。

## 验证

- [x] 后端构建通过。
- [x] 远端 MySQL 索引存在。
- [x] 资源搜索 SQL 可返回候选资源。
- [x] 前端构建通过。

## PR

- [ ] PR 描述关联 OpenSpec change。
- [ ] 填写测试结果。
- [ ] 填写风险与回滚。
