# Spec: payment

## MODIFIED Requirements

### Requirement: 新建支付订单统一使用支付宝

系统 SHALL 将站内新建支付订单统一归一为支付宝扫码支付，并使用汇付支付宝正扫交易类型。

#### Scenario: 课程支付旧参数兼容

- GIVEN 前端创建课程支付时传入空支付方式、`wxpay` 或 `wechat`
- WHEN 后端创建支付订单
- THEN 后端 SHALL 归一为支付宝并请求支付宝正扫二维码

#### Scenario: 套餐支付旧参数兼容

- GIVEN 前端创建套餐支付时传入空支付方式、`wxpay` 或 `wechat`
- WHEN 后端创建支付订单
- THEN 后端 SHALL 归一为支付宝并请求支付宝正扫二维码

#### Scenario: 汇付交易类型

- GIVEN 后端调用汇付创建扫码订单
- WHEN 当前统一支付宝支付策略生效
- THEN 请求 SHALL 使用 `A_NATIVE`
