# Proposal: course-section-doc-save-read

## 背景

课程小节文档保存后，前端再次请求 `editContent` 或学员端 `getCourseSectionContent` 时可能读到空内容或与保存不一致。根因为读取路径与写入路径不同源（`osh_doc` vs `text_content`），且 `isBindExistingDocMode` 误判导致 bind 模式跳过内容更新。

## 目标

- 统一小节文档读取优先级：优先 `osh_doc` 主文档，再回退 `osh_course_section.text_content`。
- `getCourseSectionEditContent` 不回退 video `media_url`，与编辑场景一致。
- `isBindExistingDocMode` 仅依据 `docMode=bindExisting` 判断。
- `saveOrBindSectionDoc` 正确更新/创建 `osh_doc` 并维护 section 引用。

## 非目标

- 不新增数据库表或字段。
- 不改变课程目录、视频播放、公告等无关接口。
- 不改造 OSS 上传逻辑。

## 用户影响

- 讲师：保存后 `editContent` 与保存内容一致。
- 学员：学习中心读取到与保存一致的文档内容。

## 风险

- 兼容性风险：低。读取增加 `text_content` 回退，对历史仅写 `text_content` 的数据更友好。
- 数据风险：低。不删除历史数据，仅统一读写逻辑。
- 性能风险：低。单次查询增加 doc 表读取（已有逻辑）。
- 安全风险：无变更。
- 回滚风险：revert commit `e0c0b5db` 即可。

## 验收标准

- [x] 保存 text 小节后 `GET .../editContent` 返回与保存一致内容。
- [x] 学员端 `getCourseSectionContent` 与 editContent 同源（video 除外）。
- [x] `OshCourseControllerSectionApiTest` 通过。
- [x] `mvn compile` 通过。

## 关联信息

- 分支：`feature/course_hero_hope_xinghao`（已合并 `qa/20260708` @ `73a1b668`）
- 实现提交：`e0c0b5db`（fix）、`4becad67`（OpenSpec）
- 前端配套：`feature/course_xinghao` / `openspec/changes/course-doc-markdown-study-center`（已合并 qa 并联调）
- 核心类：`OshCourseServiceImpl`
