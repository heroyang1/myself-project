# Tasks: add-external-dependency-spec

## 准备

- [x] 阅读 `openspec/project.md`
- [x] 阅读相关 `docs/ai/conventions/*.md`（coding-standards / middleware 无冲突确认）
- [x] 确认 proposal/design 已通过评审（用户拍板：中文文件名 + 立小 change）

## 实现

- [x] 新建 `.harness/外部依赖规范.md`（三节 + 附录，每节首行"什么时候读"）
- [x] `CLAUDE.md` 第 3 层索引表追加一行指针
- [x] `CLAUDE.md` 同次压缩引言两行为一行（守 100 行纪律）
- [ ] 权限与安全处理：不适用（纯文档）
- [ ] 日志与监控补充：不适用（纯文档）

## 验证

- [ ] `bash scripts/verify-ai-process.sh` 通过（四件套非空）
- [ ] `Get-Content CLAUDE.md | Measure-Object -Line` ≤ 100
- [ ] 规范文档内引用路径逐个 Test-Path（无悬空指针）
- [ ] 更新接口文档或调用说明：不适用

## PR

- [ ] PR 标题满足 Conventional Commits：`docs(harness): 新增外部依赖规范并接入宪法索引`
- [ ] PR 描述关联 OpenSpec change：`add-external-dependency-spec`
- [ ] 填写测试结果（三条验证命令输出）
- [ ] 填写风险与回滚（revert 即回滚）
- [ ] 指派 Reviewer
