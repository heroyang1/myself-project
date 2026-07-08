# GET /pc/admin/internal-member/admin-users

## 用途

查询可分配为“负责人”的内部成员管理员候选列表。

## 请求参数

- `keyword` 可选，按 `username/email/role` 模糊过滤

## 返回

- `R<List<InternalMemberAdminUserOptionVO>>`
- 主要字段: `userId`、`username`、`email`、`avatar`、`roleCode`、`roleName`、`roleLevel`

## 业务说明

- 只返回有效管理员角色
- 主要给跟进记录的 `responsibleUserId` 选择器使用

## 代码入口

- Controller: `InternalMemberAdminController.adminUsers`
- Service: `InternalMemberServiceImpl.selectAdminUserOptions`

