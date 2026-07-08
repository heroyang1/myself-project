# GET /pc/admin/internal-member/page

## 用途

分页查看付费用户学习总览。

## 请求参数

- `keyword` 可选，匹配用户名或邮箱
- `memberType` 可选，典型值: `vip`、`small_class`
- `activeOnly` 可选，是否仅查询当前有效会员

## 返回

- `TableDataInfo`
- 行类型: `InternalMemberStudyOverviewVO`
- 主要字段: `userId`、`username`、`memberType`、`planName`、`memberActive`、`memberExpireTime`、`totalCourseCount`、`startedCourseCount`、`completedCourseCount`、`overallProgress`、`lastLearnTime`

## 业务说明

- 数据同时依赖会员订单、购课记录、学习进度
- `memberActive` 根据 `expire_time > now()` 计算

## 代码入口

- Controller: `InternalMemberAdminController.page`
- Service: `InternalMemberStudyServiceImpl.selectPaidMemberStudyOverview`

