# Tasks: course-paid-count-statistics

## 准备

- [x] 确认本地接口 `/pc/course/search` 返回 `salesCount=0`。
- [x] 确认数据库 `osh_course_buy` 中课程 `8` 有 7 个 paid 用户，课程 `5/6/7` 有 2 个 paid 用户。
- [x] 定位课程列表优先走 ES，ES 文档 `salesCount` 陈旧。

## 实现

- [x] MySQL 课程列表 `salesCount` 改为实时 paid 购买人数。
- [x] 课程详情 `buyCount` 改为实时 paid 购买人数。
- [x] ES 搜索结果增加 MySQL paid 人数批量回填。
- [x] 保持收藏状态、购买状态、封面签名 URL 逻辑不变。

## 验证

- [x] 后端编译通过：`mvn -pl backstage-admin -am -DskipTests compile`。
- [x] 工作区检查通过：`git diff --check`。
- [x] 接口验证：课程 `8` 返回 `sales=7`，课程 `5/6/7` 返回 `sales=2`。
- [x] 用户本地手动测试通过。

## PR

- [x] OpenSpec change 已补充。
- [ ] PR 描述关联 `openspec/changes/course-paid-count-statistics`。
