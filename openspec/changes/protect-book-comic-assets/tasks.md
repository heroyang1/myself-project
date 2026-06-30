# Tasks: protect-book-comic-assets

## 准备

- [x] 阅读 `AGENTS.md`
- [x] 阅读 `docs/ai/README.md`
- [x] 阅读 `docs/ai/02-workflow.md`
- [x] 阅读 `openspec/project.md`
- [x] 定位漫画图片 URL 泄露链路

## 实现

- [x] 在详情接口组装漫画页时按章节可读权限过滤 `pages`
- [x] 在菜单接口组装漫画页时按章节可读权限过滤 `pages`
- [x] 保持 `forEdit=true` 管理端编辑场景返回原始图片路径
- [x] 保持 `/pc/book/detail` 阅读接口原有章节权限校验

## 验证

- [x] 新增未授权/已授权漫画图片返回测试
- [x] 运行电子书接口测试
- [x] 检查 git diff 只包含本需求相关文件

## PR

- [ ] PR 描述关联 OpenSpec change
- [ ] 填写测试结果
- [ ] 填写风险与回滚
