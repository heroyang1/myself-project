# GET /pc/admin/internal-member/{userId}/interview-summary

## 用途

查看用户面试汇总。

## 路径参数

- `userId` 必填

## 返回

- `R<InternalMemberInterviewSummaryVO>`
- 字段: `userId`、`interviewCompanyCount`、`pendingInterviewCount`、`completedInterviewCount`、`interviewRoundCount`、`interviewStatus`、`interviewResult`、`lastInterviewTime`、`updateTime`

## 业务说明

- 先做付费用户校验
- 如果档案不存在，返回带 `userId` 的空对象

## 代码入口

- Controller: `InternalMemberAdminController.interviewSummary`
- Service: `InternalMemberServiceImpl.selectInterviewSummary`

