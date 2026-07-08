# GET /pc/admin/internal-member/{userId}/documents/preview

## 用途

获取文档预览地址。

## 路径参数

- `userId` 必填

## 查询参数

- `docType` 必填: `resume` 或 `offer`
- `minute` 可选，默认 `30`

## 返回

- `R<InternalMemberDocumentPreviewVO>`
- 字段: `docType`、`fileName`、`fileKey`、`previewUrl`

## 业务说明

- 先做付费用户校验
- 如果文档不存在会报错 `Employment document not found`
- 返回的是带时效的预览签名地址

## 代码入口

- Controller: `InternalMemberAdminController.previewDocument`
- Service: `InternalMemberServiceImpl.previewEmploymentDocument`

