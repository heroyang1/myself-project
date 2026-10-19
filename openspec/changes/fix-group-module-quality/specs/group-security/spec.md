# Spec: group-security

## ADDED Requirements

### Requirement: 拼团写操作接口必须使用分布式锁防重复提交

系统 SHALL 在 join、create、pay 三个写操作接口使用 Redis 分布式锁（setIfAbsent + TTL 10s + finally delete），同一用户同一操作的并发请求必须返回友好提示。

#### Scenario: 用户快速连点参团按钮

- GIVEN 用户已点击参团按钮且请求正在处理中
- WHEN 用户在 10 秒内再次点击参团按钮
- THEN 系统 SHALL 返回"操作正在处理中，请勿重复提交"
- AND 不执行第二次参团逻辑

#### Scenario: 用户快速连点发起拼团按钮

- GIVEN 用户已点击发起拼团按钮且请求正在处理中
- WHEN 用户在 10 秒内再次点击
- THEN 系统 SHALL 返回"操作正在处理中，请勿重复提交"

#### Scenario: 用户快速连点支付按钮

- GIVEN 用户已点击支付按钮且请求正在处理中
- WHEN 用户在 10 秒内再次点击
- THEN 系统 SHALL 返回"操作正在处理中，请勿重复提交"

#### Scenario: 分布式锁自动释放

- GIVEN 用户的写操作请求已完成（成功或异常）
- WHEN 业务逻辑执行结束
- THEN 系统 SHALL 在 finally 块中释放 Redis 锁
- AND 用户可以立即发起新请求

### Requirement: 发起拼团接口使用权限注解校验

系统 SHALL 使用 `@PreAuthorize("hasAuthority('group:activity:initiable')")` 替代手写 userLevel 校验。

#### Scenario: 有权限用户查询可发起活动

- GIVEN 用户角色已配置 `group:activity:initiable` 权限
- WHEN 用户调用 `/pc/group/activity/initiable`
- THEN 系统 SHALL 返回可发起的拼团活动列表

#### Scenario: 无权限用户查询可发起活动

- GIVEN 用户角色未配置 `group:activity:initiable` 权限
- WHEN 用户调用 `/pc/group/activity/initiable`
- THEN 系统 SHALL 返回 403 Forbidden

### Requirement: 支付和订单查询接口必须校验订单归属

系统 SHALL 在 pay 和 getOrderStatus 接口校验请求用户 ID 与订单所属用户 ID 一致。

#### Scenario: 用户操作自己的订单

- GIVEN 已登录用户查询或支付自己的订单
- WHEN 用户调用 pay 或 getOrderStatus
- THEN 系统 SHALL 正常处理请求

#### Scenario: 用户尝试操作他人订单

- GIVEN 已登录用户尝试操作非本人订单
- WHEN 用户调用 pay 或 getOrderStatus
- THEN 系统 SHALL 拒绝请求并返回错误提示

### Requirement: 拼团模块全局异常处理覆盖

系统 SHALL 将 servergroup 和 order 两个 Controller 包纳入 PcExceptionHandler 全局异常兜底范围。

#### Scenario: Controller 抛出 ServiceException

- GIVEN 拼团相关 Controller 方法抛出 ServiceException
- WHEN 请求到达全局异常处理器
- THEN 系统 SHALL 返回统一的错误响应格式（R.fail）
