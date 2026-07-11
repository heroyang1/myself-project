# Proposal: course-paid-count-statistics

## 背景

课程列表卡片的「X 人学过」一直显示为 0。排查后确认本地 dev 数据库 `osh_course_buy` 中已有课程 paid 购买记录，但课程列表优先走 ES，ES 索引里的 `salesCount` 是旧值 0，导致页面显示不准确。

## 目标

- 课程列表和课程详情展示的学习人数基于真实 paid 购买记录统计。
- ES 搜索结果返回后回填 MySQL 实时 paid 购买人数，避免 ES 旧索引导致显示 0。
- 保持课程搜索、收藏状态、购买状态等既有逻辑不变。

## 非目标

- 不重建 ES 索引作为唯一修复手段。
- 不改支付、套餐、公共订单模块。
- 不新增数据库表结构。

## 用户影响

用户在课程列表和课程详情看到的学习人数与实际已支付购买人数一致；课程被购买后刷新列表即可看到更新。

## 风险

- 性能风险：低。仅对当前页课程 ID 批量统计 paid 购买人数。
- 兼容性风险：低。返回字段仍为既有 `salesCount` / `buyCount`。
- 回滚风险：低。回滚后列表会重新依赖 ES/课程表旧统计字段。

## 验收标准

- [x] `/pc/course/search` 返回的 `salesCount` 对齐 `osh_course_buy` 中 `pay_status='paid'` 且 `delete_flag=0` 的去重用户数。
- [x] 课程详情 `buyCount` 对齐真实 paid 购买人数。
- [x] 后端编译通过：`mvn -pl backstage-admin -am -DskipTests compile`。
- [x] 本地接口验证课程 `8` 返回 `salesCount=7`，课程 `5/6/7` 返回 `salesCount=2`。

## 关联信息

- Mapper：`mapper/course/OshCourseMapper.xml`
- ES 辅助 Mapper：`mapper/course/OshCourseEsSearchMapper.*`
- 服务：`service/impl/course/OshCourseEsServiceImpl`
