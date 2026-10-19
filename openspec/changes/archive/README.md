# OpenSpec Changes Archive

已完成（merged）的 change 整目录移入本目录归档：

```text
openspec/changes/archive/<change-id>/
  proposal.md
  design.md
  tasks.md
  specs/
```

## 归档规则

1. change 对应 PR 合并后，把 `openspec/changes/<change-id>/` 整个目录移动到本目录。
2. 移动时将 `specs/**/spec.md` 中的关键行为规格并入 `openspec/specs/baseline/`，保持基线规格是"当前系统行为"的单一事实来源。
3. **归档区只读**：CI（`scripts/verify-ai-process.sh`）禁止对 `openspec/changes/archive/` 下已有文件做修改或删除；新增归档目录是允许的（即归档动作本身）。要修正历史规格，新建一个 change。
