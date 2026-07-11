# GET /pc/admin/internal-member/{userId}/resume-summary

## 用途

查看用户简历投递汇总。

## 路径参数

- `userId` 必填

## 返回

- `R<InternalMemberResumeSummaryVO>`
- 字段: `userId`、`resumeDeliveryCount`、`updateTime`

## 业务说明

- 先做付费用户校验
- 如果档案不存在，返回带 `userId` 的空对象

## 代码入口

- Controller: `InternalMemberAdminController.resumeSummary`
- Service: `InternalMemberServiceImpl.selectResumeSummary`

