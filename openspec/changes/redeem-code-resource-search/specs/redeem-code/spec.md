# Spec: Redeem Code Backend

## ADDED Requirements

### Requirement: 后台按资源名称搜索核销资源

系统 SHALL 为 level>=5 用户提供按资源类型和资源名称搜索可核销资源的接口。

#### Scenario: 搜索课程资源

- GIVEN 存在 `delete_flag=0`、`status=4` 且有资源编号的课程
- WHEN level>=5 用户请求 `POST /pc/admin/redeem/resource/search` 且 `resourceType=course`
- THEN 响应返回课程资源候选
- AND 每条候选包含资源名称、资源类型、价格和内部资源编号

#### Scenario: 搜索不支持的资源类型

- GIVEN 请求参数中的 `resourceType` 不是课程、电子书、工具或会员套餐
- WHEN 用户请求资源搜索接口
- THEN 系统返回业务错误

### Requirement: 创建核销码继续校验内部资源编号

系统 SHALL 在创建核销码时继续使用内部资源编号校验资源真实性，并保存资源快照。

#### Scenario: 前端选择资源后创建核销码

- GIVEN 后台用户已通过资源搜索选中一个资源
- WHEN 前端提交包含 `userEmail`、`resourceNo`、`channelId` 和金额的创建请求
- THEN 系统按用户邮箱定位消费者
- AND 按 `resourceNo` 校验资源
- AND 创建待核销状态的核销码

### Requirement: 后台支持批量生成核销码

系统 SHALL 为 level>=5 用户提供批量生成核销码接口，支持同一商品对应多个用户，也支持多行不同商品分别生成。

#### Scenario: 同一资源批量生成给多个邮箱

- GIVEN level>=5 用户提交 `POST /pc/admin/redeem/code/batch-create`
- AND 请求体 `items` 中包含多个拥有相同 `resourceNo`、不同 `userEmail` 的条目
- WHEN 系统处理批量创建请求
- THEN 系统逐条按用户邮箱定位消费者
- AND 逐条按内部 `resourceNo` 校验资源并生成待核销状态核销码
- AND 响应包含 `total`、`successCount`、`failCount` 和逐条结果

#### Scenario: 多个资源批量生成

- GIVEN 请求体 `items` 中包含不同 `resourceNo` 的条目
- WHEN 系统处理批量创建请求
- THEN 每条核销码 SHALL 使用该条目自己的资源快照、金额、渠道和备注

#### Scenario: 批量生成部分失败

- GIVEN 批量请求中存在邮箱不存在、资源无效或参数缺失的条目
- WHEN 系统处理批量创建请求
- THEN 系统 SHALL 返回该条目 `success=false` 和失败原因
- AND 其他合法条目仍可生成核销码

#### Scenario: 批量数量超过上限

- GIVEN 请求体 `items` 超过单次批量上限
- WHEN 系统处理批量创建请求
- THEN 系统返回业务错误且不创建核销码

### Requirement: 用户中心只能核销本人待核销记录

系统 SHALL 在用户中心支持列表确认和手动输入核销码两种入口，并在服务端统一校验核销码归属与状态。

#### Scenario: 当前用户确认自己的待核销码

- GIVEN 当前登录用户拥有一条 `status=0` 的核销码记录
- WHEN 用户请求 `POST /pc/user/redeem/consume` 并提交该记录 `id`
- THEN 系统对该记录加锁
- AND 校验记录归属用户等于当前登录用户
- AND 校验状态为待核销
- AND 核销成功后仅在 `id`、当前用户 ID 和待核销状态同时匹配时将状态更新为已核销

#### Scenario: 当前用户手动输入自己的待核销码

- GIVEN 当前登录用户拥有一条 `status=0` 的核销码记录
- WHEN 用户请求 `POST /pc/user/redeem/consume` 并提交该记录 `code`
- THEN 系统对该记录加锁
- AND 校验记录归属用户等于当前登录用户
- AND 校验状态为待核销
- AND 核销成功后仅在 `id`、当前用户 ID 和待核销状态同时匹配时将状态更新为已核销

#### Scenario: 用户尝试确认他人的核销码

- GIVEN 核销码记录归属其他用户
- WHEN 当前用户提交该记录 `id` 或 `code` 进行核销
- THEN 系统拒绝核销并不发放资源

#### Scenario: 用户重复确认已核销记录

- GIVEN 核销码记录状态为已核销
- WHEN 当前用户再次提交该记录 `id` 或 `code` 进行核销
- THEN 系统拒绝核销并不重复发放资源

### Requirement: SQL 迁移包含核销资源搜索索引

系统 SHALL 在核销码 SQL 迁移文件中维护资源搜索和资源编号定位需要的索引。

#### Scenario: 执行迁移脚本

- GIVEN 当前数据库使用 `backstage`
- WHEN 执行 `sql/2026/06/redeem_code.sql`
- THEN 核销码表与渠道表存在
- AND 课程、电子书、工具、会员套餐的核销辅助索引存在
