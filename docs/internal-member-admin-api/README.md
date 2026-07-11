# 付费用户后台模块接口总览

## 范围

- 控制器: `backstage-system/src/main/java/com/backstage/system/controller/internalmember/InternalMemberAdminController.java`
- 统一前缀: `/pc/admin/internal-member`
- 当前接口总数: `18`

## 接口分组

### 1. 管理员辅助

- [01-admin-users.md](./01-admin-users.md): `GET /admin-users`

### 2. 付费用户列表与结果页

- [02-page.md](./02-page.md): `GET /page`
- [03-outcome-page.md](./03-outcome-page.md): `GET /outcome/page`

### 3. 学习概览与课程进度

- [04-summary.md](./04-summary.md): `GET /{userId}/summary`
- [05-courses.md](./05-courses.md): `GET /{userId}/courses`
- [15-study-attitude.md](./15-study-attitude.md): `GET /{userId}/study-attitude`

### 4. 就业档案

- [06-employment-profile-get.md](./06-employment-profile-get.md): `GET /{userId}/employment-profile`
- [07-employment-profile-put.md](./07-employment-profile-put.md): `PUT /{userId}/employment-profile`
- [16-salary-summary.md](./16-salary-summary.md): `GET /{userId}/salary-summary`
- [17-resume-summary.md](./17-resume-summary.md): `GET /{userId}/resume-summary`
- [18-interview-summary.md](./18-interview-summary.md): `GET /{userId}/interview-summary`

### 5. 文档管理

- [08-documents-post.md](./08-documents-post.md): `POST /{userId}/documents`
- [09-documents-preview.md](./09-documents-preview.md): `GET /{userId}/documents/preview`
- [10-documents-download.md](./10-documents-download.md): `GET /{userId}/documents/download`
- [11-documents-delete.md](./11-documents-delete.md): `DELETE /{userId}/documents`

### 6. 跟进记录

- [12-follow-records-get.md](./12-follow-records-get.md): `GET /{userId}/follow-records`
- [13-follow-records-post.md](./13-follow-records-post.md): `POST /{userId}/follow-records`

### 7. 考试

- [14-exam-summary.md](./14-exam-summary.md): `GET /{userId}/exam-summary`

## 当前实现结构

- Controller: `InternalMemberAdminController`
- Service:
  - `IInternalMemberService` / `InternalMemberServiceImpl`
  - `IInternalMemberStudyService` / `InternalMemberStudyServiceImpl`
- Mapper:
  - `InternalMemberMapper`
  - `InternalMemberStudyMapper`
- Support:
  - `InternalMemberDocumentProcessor`
  - `InternalMemberProgressStageResolver`
  - `OssUtil`

## 现状判断

- 这块不是“接口太多”，而是“单个 Service 过胖”。
- `InternalMemberServiceImpl` 集中了档案、文档、跟进、结果页、考试、薪资等多种职责。
- `InternalMemberStudyServiceImpl` 负责学习数据聚合和阶段判断。
- 接口之间高度复用同一套“付费会员校验”和“就业档案”数据。

## 两天是否够

### 两天够的情况

- 只重构 `internalmember` 后台模块
- 不改接口路径和返回结构
- 不改数据库表结构
- 以“拆职责 + 提高可读性 + 补测试”为目标

### 两天不够的情况

- 还要一起重构会员支付中心、角色发放、用户侧成长故事
- 要改接口契约或前端调用方式
- 要改文档上传链路、对象存储、权限模型

## 建议的两天方案

### Day 1

- 先按“学习域 / 就业域 / 文档域 / 跟进域”拆 service 责任
- 提取统一校验: `ensurePaidMember`、`ensureInternalMemberAdminUser`
- 提取档案查询与文档查询的内部 helper
- 先保证 controller 不动，服务内聚提升

### Day 2

- 给关键接口补单元测试和 controller 测试
- 清理重复 SQL / VO 装配逻辑
- 统一异常文案和参数校验
- 做一次接口回归

## 已有测试

- `backstage-system/src/test/java/com/backstage/system/controller/internalmember/InternalMemberAdminControllerTest.java`
- `backstage-system/src/test/java/com/backstage/system/service/internalmember/InternalMemberServiceImplTest.java`
- `backstage-system/src/test/java/com/backstage/system/service/internalmember/support/InternalMemberProgressStageResolverTest.java`

## 重构优先级建议

1. 先拆 `InternalMemberServiceImpl`
2. 再整理 `InternalMemberStudyServiceImpl`
3. 最后再考虑 mapper SQL 去重

