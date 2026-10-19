# PUT /pc/admin/internal-member/{userId}/employment-profile

## 用途

新增或更新用户就业档案。

## 路径参数

- `userId` 必填

## 请求体

- `jobStatus` 必填: `0` 未就业, `1` 求职中, `2` 已就业
- `companyName`
- `positionName`
- `city`
- `salaryBefore`
- `salaryAfter`
- `targetSalary`
- `entryTime` 格式: `yyyy-MM-dd HH:mm:ss`
- `caseEligible`
- `remark`
- `resumeDeliveryCount`
- `interviewCompanyCount`
- `pendingInterviewCount`
- `completedInterviewCount`
- `interviewRoundCount`
- `interviewStatus` 可选: `pending_interview`、`interviewing`、`finished`
- `interviewResult` 可选: `pending`、`passed`、`failed`、`offer_sent`、`onboarded`
- `lastInterviewTime` 格式: `yyyy-MM-dd HH:mm:ss`

## 返回

- `R<Void>`

## 业务说明

- 先做付费用户校验
- 若已存在则更新，不存在则插入
- Service 内部会规范化字符串和枚举值

## 代码入口

- Controller: `InternalMemberAdminController.saveEmploymentProfile`
- Service: `InternalMemberServiceImpl.saveEmploymentProfile`

