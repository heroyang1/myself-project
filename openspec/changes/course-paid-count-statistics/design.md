# Design: course-paid-count-statistics

## 方案概览

课程 MySQL 查询直接使用 `osh_course_buy` 实时统计 paid 购买人数。课程列表走 ES 时，保留 ES 查询能力，但在返回前根据当前页课程 ID 批量查询 MySQL paid 人数，并覆盖 ES 文档中的旧 `salesCount`。

## 数据来源

真实学习人数定义为：

```sql
count(distinct user_id)
from osh_course_buy
where course_id = ?
  and pay_status = 'paid'
  and delete_flag = 0
```

该定义同时覆盖课程单独购买和套餐支付成功后发放课程权益的记录。

## 详细设计

### MySQL 课程查询

- `searchCourseColumns` 中 `sales_count` 改为 paid 购买人数子查询。
- `courseDetailColumns` 中 `buy_count` 改为 paid 购买人数子查询。
- 列表排序分中的销量权重使用相同 paid 购买人数。

### ES 搜索结果回填

- 新增 `OshCourseEsSearchMapper.selectCoursePaidCounts(courseIds)`。
- `OshCourseEsServiceImpl.searchCourses` 在填充收藏/购买状态后调用 `fillSalesCount`。
- `fillSalesCount` 仅按当前页课程 ID 批量查询一次 MySQL，并覆盖每行 `salesCount`。

## 影响面

- 仅影响课程模块搜索/详情学习人数展示。
- 不影响支付创建、订单状态、套餐权益发放逻辑。
- 不要求立即重建 ES 索引。

## 测试计划

- 后端编译：`mvn -pl backstage-admin -am -DskipTests compile`。
- 接口验证：请求 `/pc/course/search`，确认 paid 课程返回非 0 `salesCount`。
- 页面验证：刷新 `/course/1`，确认「X 人学过」显示接口返回值。

## 回滚

回滚本 change 对应提交即可恢复旧逻辑；无需数据库迁移。
