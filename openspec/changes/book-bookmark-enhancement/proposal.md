# Proposal: book-bookmark-enhancement

## 背景

电子书模块已扩展书签能力，并引入漫画与电子书的书籍级媒体类型区分。原说明文档位于 `docs/book-bookmark-enhancement.md`，不符合后端 OpenSpec change 的标准目录结构，需要迁移到 `openspec/changes/book-bookmark-enhancement/`，让需求、设计、任务和验收标准与后续代码维护保持一致。

## 目标

- 规范记录电子书书签增强的行为要求，包括高亮、去重、自定义标签、删除确认、拖拽创建和跳转定位。
- 规范记录漫画书籍级媒体类型设计，使用 `osh_book.media_type` 维护电子书/漫画差异。
- 规范记录漫画章节页数据持久化方案，使用 MySQL 表 `osh_book_chapter_page` 存储图片页快照。
- 规范记录漫画保存时按当前图片顺序重建页码，避免拖动排序后出现重复页码。
- 规范记录电子书导入占位接口 `/pc/book/import/ebook`，解析落库后续实现。
- 将原 `docs/book-bookmark-enhancement.md` 移入本 change 目录，作为历史说明参考。

## 非目标

- 本次不新增前端 UI 设计说明。
- 本次不实现电子书导入解析与落库流程。
- 本次不新增漫画专用资源编号前缀。
- 本次不改变用户模块、课程模块、开源项目模块等非电子书模块行为。

## 用户影响

- 阅读用户可以更稳定地新增、查看、跳转和删除电子书书签。
- 漫画书籍在书籍层面区分媒体类型，章节阅读器可以按漫画页长滚动展示。
- 编辑者拖动漫画图片排序后，保存时页码由后端重新生成，降低唯一索引冲突风险。
- 管理类接口继续受权限等级与既有权限控制保护。

## 风险

- 兼容性风险：中。`osh_book.media_type` 和漫画页表为增量字段/表，旧数据需要默认电子书类型。
- 数据风险：中。漫画章节页采用物理清理后重建，保存失败需依赖事务保证一致性。
- 性能风险：低到中。漫画章节页数量较多时，保存会批量重建页数据。
- 安全风险：低。资源编号继续使用 `ResourceCodePrefixEnum.BOOK`，管理接口需保留权限等级控制。
- 回滚风险：中。涉及表结构与数据字段，回滚需兼容 `media_type` 和章节页表。

## 验收标准

- [x] 书签支持高亮、去重、自定义标签、删除确认、拖拽创建和点击跳转。
- [x] 电子书/漫画区别维护在书籍层面 `mediaType`，不依赖章节层面区分。
- [x] 漫画章节详情接口返回按 `page_no` 排序的 `pages`。
- [x] 漫画保存时按数组顺序重新生成页码，避免唯一键重复。
- [x] 资源编号继续使用后端 `ResourceCodePrefixEnum.BOOK`。
- [x] 电子书导入暂时仅暴露占位接口。
- [x] 原说明文档迁移到 OpenSpec change 目录。

## 关联信息

- 原文档：`docs/book-bookmark-enhancement.md`
- 迁移后：`openspec/changes/book-bookmark-enhancement/book-bookmark-enhancement.md`
- 相关 SQL：`sql/20260624_book_bookmark.sql`
