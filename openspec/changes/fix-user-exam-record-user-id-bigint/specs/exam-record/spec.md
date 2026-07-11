# Spec: exam-record

## MODIFIED Requirements

### Requirement: 用户考试记录 ID 列类型

表 `osh_user_exam_record` 的 `user_id` 与 `exam_id` 列 SHALL 为 `BIGINT(20) NOT NULL`，以存储雪花算法生成的用户 ID 与考试 ID。

#### Scenario: 雪花 user_id 写入考试记录

- GIVEN 用户 ID 为雪花 bigint（如 `2099000000000000123`）
- WHEN 调用 `GET /pc/testpaper/read` 创建或更新用户考试记录
- THEN INSERT/UPDATE 成功
- AND 不出现 `Out of range value for column 'user_id'`

#### Scenario: 已有 INT 范围内历史数据

- GIVEN 表中存在历史 INT 范围 ID
- WHEN 执行 ALTER MODIFY 为 bigint
- THEN 历史数据值保持不变
- AND 新记录可使用完整雪花 ID
