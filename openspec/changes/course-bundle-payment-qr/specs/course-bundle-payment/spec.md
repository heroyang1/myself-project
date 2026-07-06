# Spec: course-bundle-payment

## ADDED Requirements

### Requirement: Course detail shall expose related bundle recommendations

系统 SHALL 支持按资源类型和资源 ID 查询已上架套餐推荐，并仅返回包含该资源的套餐。

#### Scenario: 查询课程相关套餐

- GIVEN 某课程被配置在一个或多个已上架套餐中
- WHEN 前端请求 `/pc/bundle/recommend?resourceType=course&resourceId=<courseId>`
- THEN 系统返回包含该课程的上架套餐列表
- AND 每个套餐包含名称、价格、封面、描述和商品明细摘要

### Requirement: Course and bundle payment shall return QR-ready payment data

系统 SHALL 在课程购买和套餐购买时返回可用于二维码渲染的支付链接。

#### Scenario: 创建课程支付二维码

- GIVEN 用户已登录且课程可购买
- WHEN 用户请求 `/pc/course/pay/create`
- THEN 系统创建待支付订单和支付流水
- AND 系统返回 `qrcode` 或 `payUrl`
- AND 公共支付服务实现不被修改

#### Scenario: 创建套餐支付二维码

- GIVEN 用户已登录且套餐已上架并包含可购买内容
- WHEN 用户请求 `/pc/bundle/pay/create`
- THEN 系统创建待支付订单和支付流水
- AND 系统返回 `qrcode` 或 `payUrl`
- AND 支付创建逻辑限定在课程/套餐模块内

### Requirement: Bundle payment shall grant bundle resources after paid

系统 SHALL 在套餐订单确认支付成功后，为用户发放套餐包含的课程和电子书权益。

#### Scenario: 套餐支付成功后发放权益

- GIVEN 用户购买的套餐包含课程和电子书
- WHEN 套餐订单状态查询确认已支付
- THEN 系统为用户标记套餐内课程为已购买
- AND 系统为用户标记套餐内电子书为已购买

## MODIFIED Requirements

### Requirement: Course payment uses module-scoped QR generation

系统 SHALL 使用课程/套餐模块内的 RSA jump 支付创建服务生成课程支付二维码，而不是依赖公共旧支付创建二维码。

#### Scenario: 公共支付恢复后课程二维码仍可弹出

- GIVEN 公共支付服务保持旧链路实现
- WHEN 用户创建课程支付订单
- THEN 课程支付仍返回二维码所需链接
- AND 其他模块公共支付行为不被本次变更改变
