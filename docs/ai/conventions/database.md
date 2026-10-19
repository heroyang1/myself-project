# 数据库约定

## 表结构

- 业务表 `osh_` 前缀、系统表 `sys_` 前缀；DDL 脚本入仓库根 [`sql/`](../../../sql/)（无 Flyway，人工执行）。
- 必备字段 `create_by/create_time/update_by/update_time/remark`；逻辑删除用 `delete_flag`（0 正常）。
- 表↔实体↔VO↔ALTER 四层一致由 [`scripts/code-quality-guard.sh`](../../../scripts/code-quality-guard.sh) CI 强制（见 [AGENTS.md](../../../AGENTS.md) 硬性规则 2）。
- 字段命名表达业务含义。
- 状态字段必须列出枚举含义。
- 金额、积分、次数等字段必须说明单位。
- 重要表变更必须写迁移和回滚。

## 事务

- 事务边界放在 Service。
- 支付、订单、权益、次数扣减必须考虑幂等。
- 不在事务里做不可控慢外部调用。

## 数据迁移

OpenSpec design 必须包含：

- 迁移 SQL。
- 是否可重复执行。
- 回滚 SQL。
- 数据量估算。
- 执行窗口。
