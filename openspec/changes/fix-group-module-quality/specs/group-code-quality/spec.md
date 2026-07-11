# Spec: group-code-quality

## ADDED Requirements

### Requirement: 拼团状态码必须使用枚举引用

系统 SHALL 使用 `GroupStatusEnum` 和 `GroupOrderStatusEnum` 管理所有组团状态和订单状态值，禁止硬编码魔法数字或字符串。

#### Scenario: 组团状态比较

- GIVEN 代码中需要比较组团状态值
- WHEN 判断活动是否为已成团状态
- THEN 代码 SHALL 使用 `GroupStatusEnum.FORMED.getCode()` 而非数字 `2`

#### Scenario: 订单状态设置

- GIVEN 代码中需要设置订单为待支付状态
- WHEN 创建新拼团订单
- THEN 代码 SHALL 使用 `GroupOrderStatusEnum.PENDING.getCode()` 而非字符串 `"pending"`

#### Scenario: initiated 表独立状态映射

- GIVEN 代码中操作 `osh_group_user_initiated.group_status` 字段
- WHEN 设置或比较 initiated 状态值
- THEN 代码 SHALL 使用原始值（0=招募中, 1=已成团, 2=已结束）并添加注释说明与 GroupStatusEnum 的差异

### Requirement: 拼团业务常量必须集中管理

系统 SHALL 将所有业务配置值（SSH 默认端口/用户名、订单号前缀、招募期、支付超时、缓存 Key 等）集中在 `GroupConstants` 类中管理。

#### Scenario: SSH 默认配置

- GIVEN 代码中需要设置 SSH 默认端口或用户名
- WHEN 创建或显示服务器配置
- THEN 代码 SHALL 使用 `GroupConstants.DEFAULT_SSH_PORT` 和 `GroupConstants.DEFAULT_SSH_USER`

#### Scenario: 订单号生成

- GIVEN 代码中需要生成拼团订单号
- WHEN 调用 generateOrderNo 或创建订单
- THEN 订单号 SHALL 以 `GroupConstants.ORDER_NO_PREFIX`（"GRP"）为前缀

### Requirement: 拼团接口响应必须使用强类型 VO

系统 SHALL 使用 `AddUserToGroupVO` 和 `GroupOrderStatusVO` 替代 `Map<String, Object>` 作为接口返回类型。

#### Scenario: 手动添加用户响应

- GIVEN 管理员调用 addUserToGroup 接口
- WHEN 操作成功
- THEN 系统 SHALL 返回 `AddUserToGroupVO` 对象，包含 success/activityId/userId/currentNum/maxNum/isSuccess/message/remark 字段

#### Scenario: 订单状态查询响应

- GIVEN 用户调用 getOrderStatus 接口
- WHEN 查询成功
- THEN 系统 SHALL 返回 `GroupOrderStatusVO` 对象，包含 orderNo/status/paidTime/payableAmount/productName/countdownSeconds 字段

### Requirement: 拼团日志必须使用结构化前缀

系统 SHALL 使用 `GroupConstants.LOG_*` 常量作为日志前缀，格式为 `[GROUP][XXX]`，替代旧格式 `【拼团XXX】`。

#### Scenario: 业务日志输出

- GIVEN 拼团模块执行关键业务操作
- WHEN 输出 INFO/WARN/ERROR 级别日志
- THEN 日志 SHALL 以对应的 `GroupConstants.LOG_JOIN`/`LOG_CREATE`/`LOG_PAY_CALLBACK` 等常量作为前缀

### Requirement: 价格计算必须统一使用工具类

系统 SHALL 使用 `GroupPriceCalculator` 工具类进行所有拼团价格计算，禁止内联计算逻辑。

#### Scenario: 参团价格计算

- GIVEN 用户参与一个进行中的拼团
- WHEN 系统计算当前价格
- THEN SHALL 调用 `GroupPriceCalculator.calculateCurrentPrice()` 而非内联除法

### Requirement: 事务内必须使用统一时间戳

系统 SHALL 在同一事务中使用统一的 `LocalDateTime now` 变量，禁止多次调用 `LocalDateTime.now()`。

#### Scenario: 发起拼团事务

- GIVEN 用户发起拼团
- WHEN 事务内创建订单、发起记录、参团记录
- THEN 所有记录的创建时间 SHALL 使用同一个 `now` 变量

### Requirement: 拼团写操作必须记录操作日志

系统 SHALL 在所有增删改接口添加 `@Log` 注解记录操作日志。

#### Scenario: 参团操作日志

- GIVEN 用户成功参团
- WHEN join 接口执行完成
- THEN 系统 SHALL 通过 @Log(title="拼团", businessType=INSERT) 记录操作日志
