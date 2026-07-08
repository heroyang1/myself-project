# POST /pc/admin/internal-member/{userId}/documents

## 用途

上传用户就业文档，并进行脱敏/转 PDF 处理。

## 路径参数

- `userId` 必填

## 表单参数

- `docType` 必填: `resume` 或 `offer`
- `file` 必填，上传文件

## 返回

- `R<InternalMemberDocumentUploadVO>`
- 字段: `docType`、`fileName`、`fileKey`

## 业务说明

- 先做付费用户校验
- 会经过 `InternalMemberDocumentProcessor`
- 上传目标为对象存储
- 如果原文件存在，会在新文件成功后清理旧文件

## 代码入口

- Controller: `InternalMemberAdminController.uploadDocument`
- Service: `InternalMemberServiceImpl.uploadEmploymentDocument`

