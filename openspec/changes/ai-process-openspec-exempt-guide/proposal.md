# Proposal: ai-process-openspec-exempt-guide

## 背景

当前仓库已经接入 GitHub Actions `Spec & Process Guard`，用于校验 PR 是否包含有效的 OpenSpec change，或是否满足轻量变更豁免条件。

实际协作中，研发在看到如下报错后，常常不知道应该：

- 何时补建 `openspec/changes/<change-id>/`
- 何时允许使用 `OpenSpec-Exempt: true`
- `Reason:` 应该如何按中文规范撰写
- `ALLOW_OPENSPEC_EXEMPT=true` 是否可以直接使用

这会导致：

- PR 反复因为流程校验失败被打回
- 团队对豁免边界理解不一致
- PR 描述写法不统一，审核成本升高

因此需要补充一份标准化的 OpenSpec 变更说明，明确 `Spec & Process Guard` 的触发条件、豁免边界、PR 说明模板与不合规示例。

## 目标

- 为 `Spec & Process Guard` 的 OpenSpec 校验失败场景提供统一中文说明
- 明确“必须建 change”与“允许豁免”的边界
- 提供可直接复制的 PR 描述模板
- 统一 `OpenSpec-Exempt: true` 与 `Reason:` 的写法规范

## 非目标

- 本次不修改 GitHub Actions 工作流逻辑
- 本次不修改 `scripts/verify-ai-process.sh` 的校验实现
- 本次不调整现有业务模块、接口或数据库设计
- 本次不放宽 OpenSpec 适用范围

## 用户影响

- 研发在看到 `No OpenSpec change detected in this PR.` 后，能够快速判断下一步操作
- 研发可以直接按模板补充 PR 描述，减少流程失败次数
- Reviewer 可以按统一口径判断是否允许豁免

## 风险

- 兼容性风险：低。本次仅新增流程说明，不改运行时代码。
- 数据风险：无。不涉及数据库、缓存或索引。
- 性能风险：无。不涉及服务调用链。
- 安全风险：无。不涉及权限、认证、密钥或配置。
- 回滚风险：低。若说明不合适，可直接回滚本次变更目录。

## 验收标准

- [ ] 在 `openspec/changes/ai-process-openspec-exempt-guide/` 下提供完整的 `proposal.md`、`design.md`、`tasks.md`、`specs/**/spec.md`
- [ ] 文档使用中文说明，且包含报错场景、适用边界、模板、反例与处理步骤
- [ ] 文档明确说明只有轻量非行为性变更才允许声明 `OpenSpec-Exempt: true`
- [ ] 文档明确说明 `ALLOW_OPENSPEC_EXEMPT=true` 只能在满足 PR 描述前置条件后使用
- [ ] `bash scripts/verify-ai-process.sh` 通过

## 关联信息

- 相关脚本：`scripts/verify-ai-process.sh`
- 相关流程文档：`docs/ai/README.md`、`docs/ai/02-workflow.md`
- 相关基线规范：`openspec/project.md`
