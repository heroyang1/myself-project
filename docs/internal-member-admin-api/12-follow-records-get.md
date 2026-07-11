# GET /pc/admin/internal-member/{userId}/follow-records

## 用途

查看用户跟进记录列表。

## 路径参数

- `userId` 必填

## 返回

- `R<List<InternalMemberFollowRecordVO>>`
- 主要字段: `id`、`followType`、`content`、`operatorId`、`operatorName`、`responsibleUserId`、`responsibleUserName`、`nextFollowTime`、`createTime`

## 业务说明

- 先做付费用户校验
- 按创建时间倒序返回

## 代码入口

- Controller: `InternalMemberAdminController.followRecords`
- Service: `InternalMemberServiceImpl.selectFollowRecordList`

