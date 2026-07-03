# Proposal: fix-user-exam-record-user-id-bigint

## 背景

QA/生产环境用户 ID 为雪花算法 `bigint`，而表 `osh_user_exam_record` 的 `user_id`、`exam_id` 列为 `INT`。调用 `GET /pc/testpaper/read` 插入考试记录时报错：

```text
Data truncation: Out of range value for column 'user_id' at row 1
```

本地 dev 库若已迁移则表现正常，导致环境行为不一致。

## 目标

- 将 `osh_user_exam_record.user_id`、`exam_id` 修改为 `bigint(20) NOT NULL`。
- 提供可重复执行的 SQL 迁移脚本。

## 非目标

- 不改业务代码逻辑（Java 层已使用 Long）。
- 不迁移其他表的 int 主键（除非后续单独 change）。

## 用户影响

- VIP 用户可在 QA/生产正常「开始考试」，不再因 DB 截断失败。

## 风险

- 数据风险：低。INT 范围内数据向 bigint 扩展无损。
- 锁表风险：ALTER 期间短暂锁表，建议低峰执行。
- 回滚：可将列改回 int（仅当无超 int 范围数据时安全）。

## 验收标准

- [ ] QA/生产执行迁移后，`/pc/testpaper/read` 可成功写入 `osh_user_exam_record`。
- [ ] 现有记录可读，无数据丢失。

## 关联信息

- SQL：`sql/2026/07/fix_user_exam_record_user_id_bigint.sql`
- 前端配套 change：`osh-frontend/openspec/changes/fix-exam-start-and-auth-flow`
- 分支：`feature/exam_xinghao`
