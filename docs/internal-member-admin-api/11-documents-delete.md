# DELETE /pc/admin/internal-member/{userId}/documents

## 用途

删除用户就业文档及其对象存储文件。

## 路径参数

- `userId` 必填

## 查询参数

- `docType` 必填: `resume` 或 `offer`

## 返回

- `R<Void>`

## 业务说明

- 先做付费用户校验
- 先清数据库字段，再删对象存储文件
- 对象存储删除失败会报业务异常

## 代码入口

- Controller: `InternalMemberAdminController.deleteDocument`
- Service: `InternalMemberServiceImpl.deleteEmploymentDocument`

