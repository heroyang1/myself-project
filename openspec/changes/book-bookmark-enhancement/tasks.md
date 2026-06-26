# Tasks: book-bookmark-enhancement

## 准备

- [x] 阅读 `openspec/project.md`。
- [x] 确认电子书模块书签与漫画需求范围。
- [x] 确认资源编号继续使用 `ResourceCodePrefixEnum.BOOK`。
- [x] 确认表结构 SQL 追加到 `20260624_book_bookmark.sql`。

## 实现

- [x] 书签新增高亮、去重、自定义标签、删除确认、拖拽创建和跳转定位能力。
- [x] 书籍层新增媒体类型字段，区分电子书与漫画。
- [x] 漫画章节页存储到 MySQL `osh_book_chapter_page`。
- [x] 漫画章节详情返回按 `page_no` 排序的 `pages`。
- [x] 漫画保存采用物理清理旧页后重建当前页快照。
- [x] 保存时忽略前端旧 `pageNo`，按数组顺序重排页码。
- [x] 电子书导入暂时暴露 `/pc/book/import/ebook` 占位接口。
- [x] 管理类接口补充权限等级注解，并保留既有权限控制。
- [x] 原 `docs/book-bookmark-enhancement.md` 移动到 OpenSpec change 目录。

## 验证

- [x] 保存漫画页排序不再触发 `uk_chapter_page` 重复键错误。
- [x] 章节详情可读取漫画页数组。
- [x] 普通电子书阅读逻辑保持可用。
- [x] 书签列表、新增、跳转、删除流程可用。
- [x] 后端相关模块编译通过。
- [x] 前端静态生成通过。

## PR

- [ ] PR 描述关联 `openspec/changes/book-bookmark-enhancement`。
- [ ] 填写测试结果。
- [ ] 填写风险与回滚。
- [ ] 指派 Reviewer。
