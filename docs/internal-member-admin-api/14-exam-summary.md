# GET /pc/admin/internal-member/{userId}/exam-summary

## 用途

查看用户考试汇总。

## 路径参数

- `userId` 必填

## 返回

- `R<InternalMemberExamSummaryVO>`
- 汇总字段: `reportedCount`、`passedCount`、`failedCount`、`pendingReadCount`、`inProgressCount`、`lastExamTime`
- 明细字段: `exams[]`，包含 `recordId`、`examId`、`examTitle`、`resourceType`、`resourceId`、`score`、`totalScore`、`passScore`、`answerStatus`、`readStatus`、`result`、`reportTime`

## 业务说明

- 先做付费用户校验
- 会基于最新考试记录做结果归类

## 代码入口

- Controller: `InternalMemberAdminController.examSummary`
- Service: `InternalMemberServiceImpl.selectExamSummary`

