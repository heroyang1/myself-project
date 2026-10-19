# Tasks: add-harness-wiki-knowledge-base

## 准备

- [x] 阅读 `openspec/project.md`
- [x] 阅读相关 `docs/ai/conventions/*.md`（database / api-spec 正文核对，wiki 只摘要不复制）
- [x] 确认 proposal/design 已通过评审（大纲两轮确认 + "直接写到对应文档"指令）

## 实现

- [x] 新建 `.harness/wiki/业务模型.md`（装载时机行 + 8 节，含双枚举撞号警告）
- [x] 新建 `.harness/wiki/接口协议.md`（含实扫域接口清单）
- [x] 新建 `.harness/wiki/数据模型.md`
- [x] 新建 `.harness/wiki/领域术语.md`（重点：通用≠项目对照表 + 代码证据列）
- [x] 新建 `.harness/wiki/事故复盘.md`（A-01~A-12 映射案例集，编号+禁令+指针）
- [x] ROUTING.md 表 1 登记五层任务路由
- [x] CLAUDE.md 第 3 层加 wiki 索引行 + 命名口径压缩（守 100 行）
- [ ] 权限与安全处理：不适用（纯文档）
- [ ] 日志与监控补充：不适用（纯文档）

## 验证

- [ ] `bash scripts/verify-ai-process.sh` 通过
- [ ] CLAUDE.md ≤100 行、ROUTING.md ≤150 行
- [ ] 五文件装载时机行存在；引用路径 Test-Path 无悬空
- [ ] 更新接口文档或调用说明：不适用

## PR

- [ ] PR 标题：`docs(harness): 建立 .harness/wiki 五层知识库并接入路由与宪法索引`
- [ ] PR 描述关联 OpenSpec change：`add-harness-wiki-knowledge-base`
- [ ] 填写测试结果（验证三条输出）
- [ ] 填写风险与回滚（revert 即回滚）
- [ ] 指派 Reviewer
