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
- WHEN 前端提交包含 `resourceNo`、`channelId` 和金额的创建请求
- THEN 系统按 `resourceNo` 校验资源
- AND 创建待核销状态的核销码
- AND 待核销码不预绑定用户

### Requirement: 后台支持批量生成核销码

系统 SHALL 为 level>=5 用户提供批量生成核销码接口，支持同一商品一次生成多个通用核销码，也支持多行不同商品分别生成。

#### Scenario: 同一资源批量生成多个通用核销码

- GIVEN level>=5 用户提交 `POST /pc/admin/redeem/code/batch-create`
- AND 请求体 `items` 中包含多个拥有相同 `resourceNo` 的条目
- WHEN 系统处理批量创建请求
- THEN 系统逐条按内部 `resourceNo` 校验资源并生成待核销状态核销码
- AND 生成的核销码均不预绑定用户
- AND 响应包含 `total`、`successCount`、`failCount` 和逐条结果

#### Scenario: 多个资源批量生成

- GIVEN 请求体 `items` 中包含不同 `resourceNo` 的条目
- WHEN 系统处理批量创建请求
- THEN 每条核销码 SHALL 使用该条目自己的资源快照、金额、渠道和备注

#### Scenario: 批量生成部分失败

- GIVEN 批量请求中存在资源无效或参数缺失的条目
- WHEN 系统处理批量创建请求
- THEN 系统 SHALL 返回该条目 `success=false` 和失败原因
- AND 其他合法条目仍可生成核销码

#### Scenario: 批量数量超过上限

- GIVEN 请求体 `items` 超过单次批量上限
- WHEN 系统处理批量创建请求
- THEN 系统返回业务错误且不创建核销码

### Requirement: 用户中心可使用通用待核销码

系统 SHALL 在用户中心支持手动输入核销码使用，并在服务端只校验核销码是否存在且未使用、未作废，不校验预绑定归属。核销成功后 SHALL 将该核销码绑定到当前用户。

#### Scenario: 当前用户手动输入未使用核销码

- GIVEN 存在一条 `status=0` 的核销码记录
- WHEN 当前登录用户请求 `POST /pc/user/redeem/consume` 并提交该记录 `code`
- THEN 系统对该记录加锁
- AND 校验状态为待核销
- AND 核销成功后仅在 `id` 和待核销状态同时匹配时将状态更新为已核销
- AND 系统将 `user_id` 写为当前登录用户并发放资源

#### Scenario: 当前用户确认自己列表中的待核销码

- GIVEN 当前登录用户拥有一条 `status=0` 的核销码记录
- WHEN 用户请求 `POST /pc/user/redeem/consume` 并提交该记录 `id`
- THEN 系统对该记录加锁
- AND 校验状态为待核销
- AND 核销成功后将 `user_id` 写为当前登录用户并发放资源

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
- AND `osh_redeem_code.user_id` 允许为空以支持待核销码不预绑定用户

### Requirement: 核销码拥有一一对应邀请链接

系统 SHALL 为每个核销码维护一条一一对应的核销邀请链接，链接可用于外部平台购买后的注册引导与登录后手动核销。

#### Scenario: 批量生成核销码时返回邀请链接 token

- GIVEN level>=5 用户请求批量生成核销码
- WHEN 某条核销码创建成功
- THEN 系统创建一条 `osh_redeem_invite_link` 记录
- AND 该记录的 `redeem_code_id` 与核销码唯一对应
- AND 响应条目包含 `inviteLinkToken`

#### Scenario: 匿名访问邀请链接只查看资源信息

- GIVEN 存在启用且未过期的核销邀请链接
- WHEN 用户请求 `GET /pc/user/redeem/invite-link`
- THEN 系统返回资源名称、资源类型、金额、来源和状态
- AND 响应不包含核销码明文

#### Scenario: 登录用户解析邀请链接

- GIVEN 当前用户已登录
- AND 存在启用且未过期的核销邀请链接
- WHEN 用户请求 `GET /pc/user/redeem/invite-link/resolve`
- THEN 系统返回该链接对应的核销码
- AND 不自动核销

### Requirement: 核销邀请链接增强注册邮件

系统 SHALL 在注册提交携带有效 `redeemInviteToken` 时发送增强注册邮件，邮件同时包含唯一标识和核销码。

#### Scenario: 未注册用户通过核销邀请链接提交注册

- GIVEN 用户通过核销邀请链接进入注册页
- WHEN 前端提交注册请求且携带 `redeemInviteToken`
- THEN 系统校验链接启用、未过期且核销码待核销
- AND 若链接包含邀请人的邀请码，注册流程使用该邀请码
- AND 系统发送增强邮件模板，包含唯一标识、核销码和资源名称
- AND 核销码不自动核销

#### Scenario: 用户完成唯一标识验证

- GIVEN 用户注册提交时携带了有效 `redeemInviteToken`
- WHEN 用户通过唯一标识完成注册验证
- THEN 系统记录该邀请链接的注册邮箱、注册用户和注册时间
