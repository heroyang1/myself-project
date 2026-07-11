# GET /pc/admin/internal-member/{userId}/employment-profile

## 用途

查看用户就业档案详情。

## 路径参数

- `userId` 必填

## 返回

- `R<InternalMemberEmploymentProfileVO>`
- 主要字段:
  - 基础档案: `jobStatus`、`jobStatusName`、`companyName`、`positionName`、`city`
  - 文档字段: `resumeFileKey`、`resumeFileName`、`offerFileKey`、`offerFileName`
  - 薪资字段: `salaryBefore`、`salaryAfter`、`targetSalary`、`salaryIncrease`
  - 求职过程: `resumeDeliveryCount`、`interviewCompanyCount`、`pendingInterviewCount`、`completedInterviewCount`、`interviewRoundCount`
  - 面试状态: `interviewStatus`、`interviewResult`、`lastInterviewTime`
  - 阶段字段: `progressStageCode`、`progressStageName`

## 业务说明

- 先做付费用户校验
- 如果档案不存在，返回可能为 `null`
- 会补充进展阶段字段

## 代码入口

- Controller: `InternalMemberAdminController.employmentProfile`
- Service: `InternalMemberServiceImpl.selectEmploymentProfile`

