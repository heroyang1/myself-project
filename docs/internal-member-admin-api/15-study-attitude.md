# GET /pc/admin/internal-member/{userId}/study-attitude

## 用途

查看用户学习态度评分和信号。

## 路径参数

- `userId` 必填

## 返回

- `R<InternalMemberStudyAttitudeVO>`
- 字段: `attitude`、`score`、`overallProgress`、`startedCourseCount`、`completedCourseCount`、`unstartedCourseCount`、`lastLearnTime`、`signals`

## 业务说明

- 先做付费用户校验
- 基于最近学习时间、开课率、完课率、暂停课程、卡顿课程综合打分
- `attitude` 取值大致为: `positive`、`normal`、`negative`

## 代码入口

- Controller: `InternalMemberAdminController.studyAttitude`
- Service: `InternalMemberStudyServiceImpl.selectStudyAttitude`

