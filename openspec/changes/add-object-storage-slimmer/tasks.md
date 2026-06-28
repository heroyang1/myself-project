# Tasks: add-object-storage-slimmer

## 准备

- [x] 阅读 `AGENTS.md`
- [x] 阅读 `openspec/project.md`
- [x] 阅读 `docs/ai/02-workflow.md`
- [x] 阅读 `docs/ai/conventions/object-storage.md`
- [x] 确认参考项目 `/Users/rengang/Documents/osh-reduce-video-size`
- [x] 确认本机 `ffmpeg` / `ffprobe` 可用

## 实现

- [x] 新增后端 OpenSpec change 文档。
- [x] 新增 `osh_object_slim_task` SQL。
- [x] 扩展 R2/S3 工具能力：head/list/copy/bucket/create/upload/download/delete。
- [x] 新增对象瘦身领域对象、DTO、VO、Mapper。
- [x] 新增对象瘦身 Service：扫描、瘦身、验证、回滚、删除备份。
- [x] 新增可选瘦身模式：高画质、均衡默认、节省优先、极省空间。
- [x] 新增管理接口 `/pc/admin/object-slim/**`。
- [x] 新增 XXL-JOB handler `osh-object-storage-slimmer`。
- [x] 新增 Nuxt API 封装和主站后台页面。
- [x] 更新后台导航与子导航。
- [x] 修正 QA R2 bucket 为 `osh-qa`。

## 验证

- [x] 后端编译通过。
- [ ] 后端关键单元测试通过。
- [x] 前端 `npm run generate` 通过。
- [x] 浏览器打开后台页面验证基础展示。
- [x] 手动 smoke 验证 config/overview/page。
- [x] 手动 smoke 验证当前环境桶护栏，测试环境只允许 `osh-qa`。
- [x] dry-run 空队列验证不会触发对象写入或删除。
- [ ] 使用小视频验证完整瘦身链路。

## PR

- [ ] PR 描述关联 OpenSpec change。
- [ ] 填写测试结果。
- [ ] 填写风险与回滚。
- [ ] 指派 Reviewer。
