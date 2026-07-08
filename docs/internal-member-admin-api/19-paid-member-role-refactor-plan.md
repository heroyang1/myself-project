# Internal Member 付费用户口径改造施工单

## 1. 文档目的

这份文档用于指导 `internalmember` 后台模块从“订单口径”切换到“会员角色口径”。

目标不是改接口结构，而是：

- 不修改任何现有 `VO` 字段和返回结构
- 只调整付费用户判断逻辑和相关查询 SQL
- 统一 `internalmember` 模块内所有接口的付费用户口径

适用范围：

- 控制器：`InternalMemberAdminController`
- 服务层：`InternalMemberServiceImpl`、`InternalMemberStudyServiceImpl`
- 数据访问层：`InternalMemberMapper.xml`、`InternalMemberStudyMapper.xml`

---

## 2. 本次改造的硬约束

### 2.1 不允许改动

- 不改接口路径
- 不改请求参数
- 不改任何 `VO` 字段名
- 不改返回 JSON 结构
- 不删除前端已在使用的字段

### 2.2 必须统一的新规则

`internalmember` 模块后续统一以 `osh_user_role + osh_role` 作为当前有效付费用户判断口径，`osh_member_order` 不再作为付费用户判断依据。

旧规则：

- 付费用户 = 有会员订单的人

新规则：

- 付费用户 = 当前拥有有效会员角色的人
- `role_code in ('vip', 'small_class')`
- `expire_time > now()`

### 2.3 字段语义约束

因为本次要求 `internalmember` 模块不再依赖 `osh_member_order`，所以以下字段语义需要同步调整，但字段本身不能变：

- `memberType`：来自 `osh_role.role_code`
- `memberTypeName`：来自角色映射名称
- `memberExpireTime`：来自 `osh_user_role.expire_time`
- `memberActive`：来自角色是否当前有效
- `planName`：不再代表订单套餐名，而代表当前会员身份显示名

建议映射：

```sql
case r.role_code
    when 'vip' then 'VIP用户'
    when 'small_class' then '小班用户'
    else r.role_name
end as planName
```

---

## 3. 当前代码中的问题

## 3.1 地基问题：付费用户判断仍然是订单口径

当前两个 service 都有自己的 `ensurePaidMember`：

- `InternalMemberServiceImpl.ensurePaidMember`
- `InternalMemberStudyServiceImpl.ensurePaidMember`

它们最终都调用各自 mapper 中的：

- `countPaidMemberByUserId`

当前 SQL 是：

```sql
select count(1)
from osh_member_order
where user_id = #{userId}
  and pay_status = 1
  and delete_flag = 0
```

问题：

- 这只能证明“买过单”
- 不能证明“当前仍然是有效会员”
- 会造成列表和详情口径不一致

典型错误体验：

- 列表里看得到用户
- 点详情报“不是付费用户”

## 3.2 列表问题：主集合仍然由订单表圈人

当前以下接口的主查询直接从 `osh_member_order` 取“最新订单”：

- `GET /page`
- `GET /outcome/page`
- `GET /{userId}/summary`

问题：

- 主集合不是“当前有效会员”
- 是“最近有一笔会员订单的人”
- 会把历史付费但当前已过期的人一起查出来
- `memberType`、`planName`、`memberExpireTime` 全部是订单口径

## 3.3 代码结构问题：同一套旧逻辑被复制了两遍

以下两个 mapper 都复制了一份付费会员逻辑：

- `InternalMemberMapper.xml`
- `InternalMemberStudyMapper.xml`

问题：

- 修改成本高
- 容易漏改
- 一处改了另一处没改，就会继续口径不一致

---

## 4. 统一后的底层 SQL 口径

## 4.1 当前有效付费用户集合

这是整个模块后续应统一依赖的基础集合。

```sql
select
    ur.user_id,
    r.role_code as member_type,
    r.role_name as role_name,
    ur.expire_time
from osh_user_role ur
join osh_role r
    on r.id = ur.role_id
   and r.delete_flag = 0
   and r.status = 1
where ur.delete_flag = 0
  and r.role_code in ('vip', 'small_class')
  and ur.expire_time is not null
  and ur.expire_time > now()
```

## 4.2 单用户是否为当前有效付费用户

用于替换所有 `countPaidMemberByUserId`。

```sql
select count(1)
from osh_user_role ur
join osh_role r
    on r.id = ur.role_id
   and r.delete_flag = 0
   and r.status = 1
where ur.user_id = #{userId}
  and ur.delete_flag = 0
  and r.role_code in ('vip', 'small_class')
  and ur.expire_time is not null
  and ur.expire_time > now()
```

## 4.3 单用户当前会员信息

用于替换 `summary`、`page`、`outcome/page` 中的会员信息来源。

如果一个用户同时存在多个有效会员角色，建议优先级如下：

- `small_class` 优先于 `vip`
- 同角色时按 `expire_time` 更晚优先

建议 SQL：

```sql
select
    t.user_id,
    t.member_type,
    t.role_name,
    t.expire_time
from (
    select
        ur.user_id,
        r.role_code as member_type,
        r.role_name,
        ur.expire_time,
        row_number() over (
            partition by ur.user_id
            order by
                case r.role_code
                    when 'small_class' then 2
                    when 'vip' then 1
                    else 0
                end desc,
                ur.expire_time desc,
                ur.update_time desc,
                ur.id desc
        ) as rn
    from osh_user_role ur
    join osh_role r
        on r.id = ur.role_id
       and r.delete_flag = 0
       and r.status = 1
    where ur.delete_flag = 0
      and r.role_code in ('vip', 'small_class')
      and ur.expire_time is not null
      and ur.expire_time > now()
) t
where t.rn = 1
```

## 4.4 返回字段映射建议

```sql
t.member_type as memberType,
case t.member_type
    when 'vip' then 'VIP Member'
    when 'small_class' then 'Small Class Member'
    else t.member_type
end as memberTypeName,
case t.member_type
    when 'vip' then 'VIP用户'
    when 'small_class' then '小班用户'
    else t.role_name
end as planName,
1 as memberActive,
date_format(t.expire_time, '%Y-%m-%d %H:%i:%s') as memberExpireTime
```

说明：

- `memberTypeName` 保持现有英文风格，减少前端展示波动
- `planName` 继续保留字段，但语义改为“会员身份展示名”
- 因为主集合已经只保留当前有效会员，所以 `memberActive` 实际上会固定为 `1`

---

## 5. 改造顺序

建议严格按下面顺序改，不要先改列表。

### 第一步：先改准入判断

先统一替换：

- `InternalMemberMapper.xml -> countPaidMemberByUserId`
- `InternalMemberStudyMapper.xml -> countPaidMemberByUserId`

这样所有详情、操作类接口会先变成统一口径。

### 第二步：再改所有单用户详情/操作接口

这些接口本身多数不是从订单表取主数据，但入口处都依赖 `ensurePaidMember`。

### 第三步：最后改列表与汇总类接口

包括：

- `/page`
- `/outcome/page`
- `/{userId}/summary`

这三类接口不仅依赖准入判断，还直接在 SQL 中使用了订单口径会员信息。

---

## 6. 接口改造清单

下表按改造优先级排序。

| 优先级 | 接口 | 当前问题 | 改造建议 | 主要改动点 |
| --- | --- | --- | --- | --- |
| P0 | `GET /{userId}/summary` | 准入判断走订单口径；主查询会员信息也走订单口径 | 改 `ensurePaidMember`，并把会员信息来源切到角色表 | `InternalMemberStudyServiceImpl`、`InternalMemberStudyMapper.xml` |
| P0 | `GET /{userId}/courses` | 准入判断走订单口径 | 只需切准入判断，不动 VO | `InternalMemberStudyServiceImpl`、`InternalMemberStudyMapper.xml` |
| P0 | `GET /{userId}/employment-profile` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `PUT /{userId}/employment-profile` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `POST /{userId}/documents` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/documents/preview` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/documents/download` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `DELETE /{userId}/documents` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/follow-records` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `POST /{userId}/follow-records` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/exam-summary` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/study-attitude` | 准入判断走订单口径，且内部依赖 `summary` | 先切准入判断，再确保依赖的 `summary` 已切角色口径 | `InternalMemberStudyServiceImpl`、`InternalMemberStudyMapper.xml` |
| P0 | `GET /{userId}/salary-summary` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/resume-summary` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P0 | `GET /{userId}/interview-summary` | 准入判断走订单口径 | 只需切准入判断 | `InternalMemberServiceImpl`、`InternalMemberMapper.xml` |
| P1 | `GET /page` | 主集合来自 `osh_member_order`；会员字段也来自订单 | 主集合改成角色口径；保持原 VO 字段不变 | `InternalMemberStudyMapper.xml` |
| P1 | `GET /outcome/page` | 主集合来自 `osh_member_order`；结果混入历史订单用户 | 主集合改成角色口径；保持原 VO 字段不变 | `InternalMemberMapper.xml` |

唯一可不改的接口：

- `GET /admin-users`

原因：

- 它查的是内部管理员用户角色
- 与付费会员口径无关

---

## 7. 具体施工建议

## 7.1 Service 层施工点

### `InternalMemberServiceImpl`

受影响的方法：

- `selectEmploymentProfile`
- `selectResumeSummary`
- `selectInterviewSummary`
- `selectSalarySummary`
- `saveEmploymentProfile`
- `uploadEmploymentDocument`
- `previewEmploymentDocument`
- `downloadEmploymentDocument`
- `deleteEmploymentDocument`
- `selectFollowRecordList`
- `addFollowRecord`
- `selectOutcomeList`
- `selectExamSummary`
- `ensurePaidMember`

改造建议：

- 不改方法签名
- 不改返回类型
- 统一保留 `ensurePaidMember(userId)` 调用方式
- 只把 `ensurePaidMember` 底层依赖改成新 SQL

### `InternalMemberStudyServiceImpl`

受影响的方法：

- `selectMemberStudySummary`
- `selectMemberCourseProgressList`
- `selectStudyAttitude`
- `ensurePaidMember`

改造建议：

- 不改方法签名
- `selectStudyAttitude` 逻辑可保持不动
- 只要 `ensurePaidMember` 和 `selectMemberStudySummary` 的会员来源口径统一即可

## 7.2 Mapper 层施工点

### `InternalMemberMapper.xml`

必须改：

- `countPaidMemberByUserId`
- `selectOutcomeList`

建议同时检查但可视情况保留：

- `selectEmploymentProfileByUserId`
- `selectResumeSummaryByUserId`
- `selectInterviewSummaryByUserId`
- `selectExamListByUserId`

说明：

- 这几个查询本身不是订单口径，不需要大改
- 主要是它们的入口校验需要统一

### `InternalMemberStudyMapper.xml`

必须改：

- `countPaidMemberByUserId`
- `selectPaidMemberStudyOverview`
- `selectMemberStudySummary`

`selectMemberCourseProgressList` 可不动：

- 它查的是课程进度
- 不依赖订单口径会员信息
- 只依赖外层准入判断

---

## 8. 列表与汇总接口的 SQL 改造建议

## 8.1 `GET /page`

当前问题：

- `latest` 子查询来自 `osh_member_order`
- `memberType/memberTypeName/planName/memberExpireTime/memberActive` 全部取自订单

改造方式：

- 用“当前有效付费用户集合”子查询替换当前 `latest`
- `memberType` 改取 `role_code`
- `planName` 改为角色展示名
- `memberExpireTime` 改取 `osh_user_role.expire_time`
- `activeOnly` 可以继续保留，但因为主集合已是有效会员，所以实际恒成立

建议替换为类似结构：

```sql
inner join (
    select
        t.user_id,
        t.member_type,
        t.role_name,
        t.expire_time
    from (
        select
            ur.user_id,
            r.role_code as member_type,
            r.role_name,
            ur.expire_time,
            row_number() over (
                partition by ur.user_id
                order by
                    case r.role_code
                        when 'small_class' then 2
                        when 'vip' then 1
                        else 0
                    end desc,
                    ur.expire_time desc,
                    ur.update_time desc,
                    ur.id desc
            ) as rn
        from osh_user_role ur
        join osh_role r
            on r.id = ur.role_id
           and r.delete_flag = 0
           and r.status = 1
        where ur.delete_flag = 0
          and r.role_code in ('vip', 'small_class')
          and ur.expire_time is not null
          and ur.expire_time > now()
    ) t
    where t.rn = 1
) latest on latest.user_id = u.id
```

## 8.2 `GET /outcome/page`

当前问题：

- 会员主集合直接从订单表取
- 会混入当前已过期但历史上买过会员的人

改造方式：

- `latest` 子查询同样切到角色表
- 保留 `memberType`、`memberTypeName` 字段
- 如果当前 `InternalMemberOutcomeVO` 没有 `planName` 字段，则无需补

## 8.3 `GET /{userId}/summary`

当前问题：

- 查询本身直接 join 订单最新记录
- 即使 `ensurePaidMember` 改完，返回的会员字段仍然是订单口径

改造方式：

- 会员部分跟 `/page` 使用同一套角色来源
- 课程统计部分保持不动
- `currentStage` 填充逻辑保持不动

---

## 9. 逐接口施工顺序

建议按下面顺序提交改造，最稳：

1. 改 `InternalMemberMapper.xml -> countPaidMemberByUserId`
2. 改 `InternalMemberStudyMapper.xml -> countPaidMemberByUserId`
3. 验证所有详情/操作类接口的准入判断一致
4. 改 `InternalMemberStudyMapper.xml -> selectMemberStudySummary`
5. 改 `InternalMemberStudyMapper.xml -> selectPaidMemberStudyOverview`
6. 改 `InternalMemberMapper.xml -> selectOutcomeList`
7. 回归验证 `study-attitude`、`salary-summary`、`resume-summary`、`interview-summary`

不要先改 `/page` 和 `/outcome/page` 再去改 `ensurePaidMember`。

否则会出现：

- 列表是新口径
- 详情还是旧口径
- 用户点进去报错

---

## 10. 建议保留的异常与行为

这些行为建议保持不变，减少联动风险：

- 用户不存在或不符合准入时，仍然抛 `ServiceException`
- controller 不改
- `R.ok(...)`、`TableDataInfo` 包装不改
- 所有 `VO` 为空时的兜底逻辑不改

---

## 11. 测试与回归关注点

至少回归以下场景：

1. 用户有历史会员订单，但角色已过期
   - `/page` 不应出现
   - `/outcome/page` 不应出现
   - 详情接口应提示不是当前有效付费用户

2. 用户没有订单，但有当前有效 `vip` 角色
   - 应被识别为付费用户
   - 详情接口可正常访问
   - `/page`、`/outcome/page` 可正常出现

3. 用户没有订单，但有当前有效 `small_class` 角色
   - 应被识别为付费用户

4. 用户同时有 `vip` 和 `small_class` 两个有效角色
   - 返回哪个 `memberType` 要按本文档的优先级一致处理

5. 前端现有依赖字段仍然存在
   - `memberType`
   - `memberTypeName`
- `planName`
- `memberExpireTime`
- `memberActive`

---

## 11.1 会员类型下拉与筛选来源补充

这次切口径后，凡是涉及“会员类型”的筛选项、下拉框、选项映射，都不应该再从订单表或套餐表反推。

统一原则：

- 会员类型的唯一来源是 `osh_role`
- 当前模块只认 `vip`、`small_class`
- 不再把 `vip_month`、`vip_year`、`small_class_year` 这类套餐概念当成会员类型

推荐选项来源 SQL：

```sql
select
    r.role_code as memberType,
    case r.role_code
        when 'vip' then 'VIP Member'
        when 'small_class' then 'Small Class Member'
        else r.role_name
    end as memberTypeName,
    r.role_name as roleDisplayName
from osh_role r
where r.delete_flag = 0
  and r.status = 1
  and r.role_code in ('vip', 'small_class')
order by case r.role_code
    when 'vip' then 1
    when 'small_class' then 2
    else 99
end
```

当前代码扫描结论：

- 目前 `internalmember` 模块没有单独的“会员类型下拉接口”
- 当前明确存在的下拉接口只有 `GET /admin-users`
- `GET /page` 里有 `query.memberType` 过滤，但选项来源大概率是前端静态写死

因此本次改造建议是：

- 如果前端会员类型下拉是静态的，继续传 `vip` / `small_class` 即可
- 如果后端后续要补“会员类型选项接口”，必须直接查 `osh_role`
- 不要新增基于 `osh_member_plan` 或 `osh_member_order` 的会员类型选项逻辑

---

## 12. 结论

这次改造本质上不是“改两个列表 SQL”，而是：

- 重定义 `internalmember` 模块底层的付费用户口径
- 先统一准入判断
- 再替换所有依赖该口径的查询
- 在不改 VO 的前提下，把会员字段统一切换到角色来源

一句话定稿：

`internalmember` 模块只认当前有效会员角色，不再认 `osh_member_order` 作为付费用户判断依据。
