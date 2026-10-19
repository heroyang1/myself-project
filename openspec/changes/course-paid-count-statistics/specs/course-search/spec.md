# Spec: course-search

## MODIFIED Requirements

### Requirement: Course learned count shall reflect paid purchase records

课程搜索和课程详情 SHALL 使用真实 paid 购买记录统计学习人数，而不是仅依赖课程表旧统计字段或 ES 旧索引字段。

#### Scenario: 课程列表返回真实学习人数

- GIVEN `osh_course_buy` 中某课程存在 `pay_status='paid'` 且 `delete_flag=0` 的购买记录
- WHEN 用户请求 `/pc/course/search`
- THEN 返回行的 `salesCount` 等于该课程 paid 购买记录的去重用户数
- AND 即使课程搜索命中 ES，也会使用 MySQL 实时统计覆盖 ES 旧值

#### Scenario: 课程详情返回真实学习人数

- GIVEN `osh_course_buy` 中某课程存在 paid 购买记录
- WHEN 用户请求课程详情接口
- THEN 返回的 `buyCount` 等于该课程 paid 购买记录的去重用户数

#### Scenario: 新购买课程后刷新列表

- GIVEN 用户完成课程购买或通过套餐获得课程权益
- AND 系统写入或更新 `osh_course_buy` 为 `pay_status='paid'`
- WHEN 用户刷新课程列表
- THEN 对应课程 `salesCount` 反映最新 paid 购买人数
