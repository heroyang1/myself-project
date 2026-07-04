# Tasks: fix-user-exam-record-user-id-bigint

## 准备

- [x] 确认表结构与错误日志
- [x] 编写 SQL 脚本

## 实现

- [x] 新增 `sql/2026/07/fix_user_exam_record_user_id_bigint.sql`

## 验证

- [x] dev 库执行 ALTER 成功
- [ ] QA 库执行迁移
- [ ] 生产库执行迁移（按发布窗口）
- [ ] QA 冒烟：VIP 用户开始考试成功

## PR

- [ ] PR 描述关联 `openspec/changes/fix-user-exam-record-user-id-bigint`
- [ ] 部署清单注明 DBA 执行 SQL
