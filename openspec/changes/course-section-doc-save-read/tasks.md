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

- [x] 本地集成测试通过。
- [ ] CI `Spec & Process Guard` 通过。
- [ ] 与前端联调：保存 ZIP 导入文档后 editContent 一致。

## PR

- [x] 提交至 `feature/course_hero_hope_xinghao` 并 push。
- [ ] PR 描述关联 `openspec/changes/course-section-doc-save-read`。
