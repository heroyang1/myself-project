# Spec: Course Pay Client IP

## MODIFIED Requirements

### Requirement: 课程支付创建订单时传递合法 clientip

系统 SHALL 在 `POST /pc/course/pay/create` 创建课程支付订单时，向支付网关传递单个合法 IPv4/IPv6 客户端地址作为 `clientip`；当请求经 nginx 或 Cloudflare 反代时，SHALL 正确解析真实客户端 IP，而非传递含逗号的多级代理链整串。

#### Scenario: 经 Cloudflare 域名访问支付成功

- GIVEN 用户已通过 `juegeresource.top`（或同类 CF 代理域名）登录
- AND 用户选择课程并进入支付页
- WHEN 调用 `POST /pc/course/pay/create` 且 `pay_type` 为 `wxpay` 或 `alipay`
- THEN 响应 `code=200`
- AND `data.qrcode` 非空
- AND 不出现「用户IP地址不合法」

#### Scenario: 经 nginx 反代时解析 X-Forwarded-For 首段

- GIVEN 请求经 osh-nginx 转发且 `X-Forwarded-For` 为 `"203.0.113.1, 172.18.0.1"`
- WHEN 创建课程支付订单
- THEN 传给网关的 `clientip` 为 `203.0.113.1`（首段公网 IP）
- AND 不传整段 `"203.0.113.1, 172.18.0.1"`

#### Scenario: Cloudflare 优先 CF-Connecting-IP

- GIVEN 请求头含 `CF-Connecting-IP: 198.51.100.10`
- WHEN 创建课程支付订单
- THEN 传给网关的 `clientip` 为 `198.51.100.10`

#### Scenario: 直连 backend 行为不退化

- GIVEN 请求直连 backend 且无代理头
- WHEN 创建课程支付订单
- THEN 仍返回有效 `qrcode`
- AND 支付流程与变更前一致
