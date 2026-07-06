# Spec: ai-dev-process

## ADDED Requirements

### Requirement: OpenSpec 豁免说明必须可被标准化复用

仓库 SHALL 为 `Spec & Process Guard` 的 OpenSpec 校验失败场景提供标准化说明，明确何时必须新建 `openspec/changes/<change-id>/`，何时才允许使用 `OpenSpec-Exempt: true`。

#### Scenario: 研发遇到未检测到 OpenSpec change 的报错

- GIVEN PR 校验日志出现 `No OpenSpec change detected in this PR.`
- WHEN 研发根据仓库规范处理该问题
- THEN 仓库 SHALL 提供一份可直接复用的标准说明
- AND 说明中 SHALL 明确必须建 change 的场景与允许豁免的场景

### Requirement: OpenSpec 豁免 PR 描述必须包含固定字段与清晰原因

仓库 SHALL 要求所有 OpenSpec 豁免类 PR 在描述中包含 `OpenSpec-Exempt: true` 与 `Reason:`，且 `Reason:` 必须清晰说明为何本次改动属于轻量非行为性变更。

#### Scenario: 研发准备以豁免方式提交轻量 PR

- GIVEN 本次 PR 不包含 `openspec/changes/<change-id>/`
- AND 本次改动仅为文档、注释、格式或其他轻量非行为性调整
- WHEN 研发编写 PR 描述
- THEN PR 描述 SHALL 包含 `OpenSpec-Exempt: true`
- AND PR 描述 SHALL 包含带明确原因的 `Reason:`

### Requirement: CI 豁免开关只能在满足前置条件后使用

仓库 SHALL 明确 `ALLOW_OPENSPEC_EXEMPT=true` 不是独立豁免手段，而只能在 PR 描述已经满足豁免声明要求后作为 CI 配置配合使用。

#### Scenario: 研发希望通过 CI 豁免继续执行检查

- GIVEN 研发计划在 CI 中使用 `ALLOW_OPENSPEC_EXEMPT=true`
- WHEN PR 描述尚未包含 `OpenSpec-Exempt: true` 或缺少清晰 `Reason:`
- THEN 该用法 SHALL 被视为不符合流程规范
- AND 团队 SHALL 优先补齐 PR 描述或补建 OpenSpec change
