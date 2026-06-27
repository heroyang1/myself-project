# Tasks: course-section-doc-save-read

## 准备

- [x] 定位 editContent 与 save 读写不同源问题。
- [x] 阅读 `OshCourseServiceImpl` 现有 doc 绑定逻辑。

## 实现

- [x] 新增 `resolveSectionDocContent(sectionId, allowVideoMediaFallback)`。
- [x] `getCourseSectionEditContent` 调用时 `allowVideoMediaFallback=false`。
- [x] `isBindExistingDocMode` 仅判断 `docMode=bindExisting`。
- [x] `saveOrBindSectionDoc` 更新/创建 `osh_doc` 逻辑修正。
- [x] `OshCourseControllerSectionApiTest` 补充/更新用例。
- [x] 新增本 OpenSpec change 文档。

## 验证

- [x] 本地集成测试通过（`OshCourseControllerSectionApiTest#shouldPersistAndReloadVideoSectionDocContentForEditPage`）。
- [x] 与前端联调：Markdown ZIP 导入保存后 `editContent` 与学习中心内容一致（`feature/course_xinghao`）。
- [ ] CI `Spec & Process Guard` 通过（待 push 后观察）。

## 合并

- [x] 将 `qa/20260708`（`73a1b668`）Fast-forward 合并入 `feature/course_hero_hope_xinghao`。
- [x] 前端 `feature/course_xinghao` 已同步合并 `qa/20260708`，与后端分支对齐联调。

## PR

- [x] 提交至 `feature/course_hero_hope_xinghao` 并 push。
- [x] PR 描述关联 `openspec/changes/course-section-doc-save-read`。
