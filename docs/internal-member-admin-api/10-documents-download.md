# GET /pc/admin/internal-member/{userId}/documents/download

## 用途

获取文档下载地址。

## 路径参数

- `userId` 必填

## 查询参数

- `docType` 必填: `resume` 或 `offer`
- `minute` 可选，默认 `30`

## 返回

- `R<InternalMemberDocumentDownloadVO>`
- 字段: `docType`、`fileName`、`fileKey`、`downloadUrl`

## 业务说明

- 先做付费用户校验
- 返回的是带时效的下载签名地址

## 代码入口

- Controller: `InternalMemberAdminController.downloadDocument`
- Service: `InternalMemberServiceImpl.downloadEmploymentDocument`

