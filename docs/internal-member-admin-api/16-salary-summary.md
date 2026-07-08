# GET /pc/admin/internal-member/{userId}/salary-summary

## 用途

查看用户薪资汇总。

## 路径参数

- `userId` 必填

## 返回

- `R<InternalMemberSalarySummaryVO>`
- 字段: `jobStatus`、`jobStatusName`、`companyName`、`positionName`、`salaryBefore`、`salaryAfter`、`targetSalary`、`salaryIncrease`、`salaryIncreaseRate`、`gapToTargetSalary`、`entryTime`、`caseEligible`、`updateTime`

## 业务说明

- 先做付费用户校验
- 如果就业档案不存在，返回一个只有 `userId` 的空汇总对象

## 代码入口

- Controller: `InternalMemberAdminController.salarySummary`
- Service: `InternalMemberServiceImpl.selectSalarySummary`

