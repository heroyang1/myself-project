# GET /pc/admin/internal-member/{userId}/courses

## 用途

查看单个付费用户的课程进度明细。

## 路径参数

- `userId` 必填

## 查询参数

- `keyword` 可选，课程标题关键字
- `includeUnstarted` 可选，是否包含未开课课程

## 返回

- `TableDataInfo`
- 行类型: `InternalMemberCourseProgressVO`
- 主要字段: `courseId`、`courseTitle`、`purchasedFlag`、`startedFlag`、`learnedSectionCount`、`totalSectionCount`、`progressPercent`、`completedFlag`、`progressStatus`、`progressStatusName`、`purchaseTime`、`startLearnTime`、`lastLearnTime`

## 业务说明

- 先做付费用户校验
- 课程来源包括已购课程和已有学习记录课程

## 代码入口

- Controller: `InternalMemberAdminController.courseDetail`
- Service: `InternalMemberStudyServiceImpl.selectMemberCourseProgressList`

