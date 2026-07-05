# Tasks: product-bundle-management

## 准备

- [x] 阅读 `openspec/project.md`。
- [x] 确认套餐模块只新增独立业务边界，不修改兑换码模块。
- [x] 确认套餐资源范围：课程、电子书、工具、考试。

## 实现

- [x] 新增套餐主表和套餐明细表 SQL。
- [x] 新增套餐 Entity、DTO、VO。
- [x] 新增套餐 Mapper、XML 和资源搜索 Mapper。
- [x] 新增套餐 Service，包含列表、详情、保存、状态更新、删除。
- [x] 新增 `/pc/admin/bundle/*` 后台接口。
- [x] 接口统一限制 `level>=5`。
- [x] 套餐资源搜索使用 `BundleResourceVO`，不依赖兑换码模块 VO。

## 验证

- [x] `git diff --check` 通过。
- [x] 前端 `npm run generate` 通过。
- [x] 用户完成套餐模块手动验收。
- [ ] 后端全量编译通过。

## PR

- [x] OpenSpec change 已补充。
- [ ] PR 描述关联 OpenSpec change。
- [ ] 填写测试结果。
- [ ] 填写风险与回滚。
