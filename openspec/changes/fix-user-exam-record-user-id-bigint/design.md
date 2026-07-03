# Design: fix-user-exam-record-user-id-bigint

## 方案概览

通过 DDL 将 `osh_user_exam_record` 的 `user_id`、`exam_id` 从 `INT` 改为 `BIGINT(20)`，与雪花 ID 及 Java `Long` 类型对齐。

## 现状分析

- 表：`osh_user_exam_record`
- 列：`user_id INT`、`exam_id INT`
- 现象：雪花 user_id 超出 INT 范围导致 INSERT 失败

## 详细设计

### 接口设计

| Method | Path | 变化 | 兼容策略 |
|---|---|---|---|
| GET | `/pc/testpaper/read` | 无代码变更 | 依赖 DB 列类型修复 |

### SQL

```sql
ALTER TABLE `osh_user_exam_record`
    MODIFY COLUMN `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN `exam_id` bigint(20) NOT NULL COMMENT '考试ID';
```

### 发布与回滚

- 发布：各环境 DBA/运维执行 `sql/2026/07/fix_user_exam_record_user_id_bigint.sql`
- 验证：VIP 用户调用 testpaper/read 成功；检查表结构 `SHOW CREATE TABLE osh_user_exam_record`
- 回滚：仅在确认无超 INT 数据时可改回 INT（不推荐）

## 测试计划

- dev 库已验证 ALTER 成功
- QA/生产执行后冒烟：登录 VIP → testpaper/read → 返回 200 且插入记录
