# GET /pc/admin/internal-member/{userId}/summary

## 用途

查看单个付费用户的学习总览。

## 路径参数

- `userId` 必填，付费用户 ID

## 返回

- `R<InternalMemberStudySummaryVO>`
- 主要字段: `userId`、`username`、`memberType`、`planName`、`memberActive`、`memberExpireTime`、`totalCourseCount`、`startedCourseCount`、`completedCourseCount`、`unstartedCourseCount`、`overallProgress`、`firstLearnTime`、`lastLearnTime`、`currentStage`

## 业务说明

- 先做付费用户校验
- 查不到会抛 `Paid member record not found`
- 会额外计算当前阶段 `currentStage/currentStageName/currentStageIndex`

## 代码入口

- Controller: `InternalMemberAdminController.summary`
- Service: `InternalMemberStudyServiceImpl.selectMemberStudySummary`

