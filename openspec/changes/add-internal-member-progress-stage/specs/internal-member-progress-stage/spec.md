# Spec: internal-member-progress-stage

## ADDED Requirements

### Requirement: 内部付费用户接口应返回统一阶段枚举

系统 SHALL 在内部付费用户就业相关接口中返回统一的当前阶段字段，供前端直接展示用户求职进度。

#### Scenario: 查询付费用户就业结果列表

- GIVEN 管理端调用 `GET /pc/admin/internal-member/outcome/page`
- WHEN 后端返回某个付费用户的就业数据
- THEN 响应项 SHALL 追加 `progressStageCode`、`progressStageName`、`progressStageOrder`、`progressStageTotal`
- AND 原有 `jobStatus`、`jobStatusName` 等字段 SHALL 保持兼容

#### Scenario: 查询单个付费用户就业档案

- GIVEN 管理端调用 `GET /pc/admin/internal-member/{userId}/employment-profile`
- WHEN 后端返回该用户就业档案
- THEN 响应 SHALL 追加 `progressStageCode`、`progressStageName`、`progressStageOrder`、`progressStageTotal`
- AND 原有档案字段 SHALL 保持兼容

### Requirement: 当前阶段应由后端统一推导

系统 SHALL 基于已有就业档案字段统一推导当前阶段，而不是由前端自行拼接规则。

#### Scenario: 用户已入职

- GIVEN 用户 `jobStatus = 2`，或 `interviewResult = onboarded`，或 `entryTime` 非空
- WHEN 后端计算当前阶段
- THEN 当前阶段 SHALL 为 `onboarded`

#### Scenario: 用户处于 Offer 待入职

- GIVEN 用户 `interviewResult = offer_sent`
- WHEN 后端计算当前阶段
- THEN 当前阶段 SHALL 为 `offer_pending`

#### Scenario: 用户处于面试中

- GIVEN 用户存在面试相关状态或面试数量
- WHEN 后端计算当前阶段
- THEN 当前阶段 SHALL 为 `interviewing`

#### Scenario: 用户已投递但未进入面试

- GIVEN 用户 `resumeDeliveryCount > 0`
- AND 用户不满足面试中、Offer、已入职条件
- WHEN 后端计算当前阶段
- THEN 当前阶段 SHALL 为 `delivering`

#### Scenario: 用户仅进入求职状态

- GIVEN 用户 `jobStatus = 1`
- AND 用户没有投递、面试、Offer、入职信号
- WHEN 后端计算当前阶段
- THEN 当前阶段 SHALL 为 `preparing`

#### Scenario: 用户尚未开始求职

- GIVEN 用户不满足以上任一条件
- WHEN 后端计算当前阶段
- THEN 当前阶段 SHALL 为 `separated`
