# GET /pc/admin/internal-member/outcome/page

## 用途

分页查看付费用户就业结果和阶段进展。

## 请求参数

- `keyword` 可选，匹配用户名、邮箱、公司、岗位
- `jobStatus` 可选: `0` 未就业, `1` 求职中, `2` 已就业
- `minSalaryIncrease` 可选，最低涨薪金额
- `caseEligible` 可选: `0` 否, `1` 是

## 返回

- `TableDataInfo`
- 行类型: `InternalMemberOutcomeVO`
- 主要字段: `userId`、`memberType`、`jobStatus`、`companyName`、`positionName`、`city`、`salaryBefore`、`salaryAfter`、`salaryIncrease`、`entryTime`、`progressStageCode`、`progressStageName`、`lastFollowTime`

## 业务说明

- 聚合就业档案和跟进记录
- 会补充 `progressStageCode/progressStageName`

## 代码入口

- Controller: `InternalMemberAdminController.outcomePage`
- Service: `InternalMemberServiceImpl.selectOutcomeList`

