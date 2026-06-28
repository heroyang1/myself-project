# Proposal: course-pay-client-ip-fix

## 背景

课程详情页点击「确认支付」后，前端调用 `POST /pc/course/pay/create` 创建支付订单并获取二维码。在 25 测试环境（`juegeresource.top`，经 Cloudflare + osh-nginx 反代）及生产类似链路下，接口返回「用户IP地址不合法」，二维码无法展示。

直连 backend（`:58081`）同参数可正常返回二维码，说明支付网关与课程价格逻辑正常，问题在 **clientip 解析**。

## 目标

- 课程支付创建订单时，向后端支付网关传递合法的单 IP `clientip`。
- 支持 nginx 多级反代（`X-Forwarded-For` 逗号链取首段）及 Cloudflare（`CF-Connecting-IP`）。
- 改动限定课程支付入口，不修改公共 `IpUtils` 或其它订单支付模块。

## 非目标

- 不改支付网关 PHP、Nacos 回调地址、nginx 容器配置。
- 不改 `CoursePayServiceImpl` 网关签名与下单逻辑。
- 不在本变更中修复拼团/通用订单 `/pc/order/wxpay` 的 IP 解析（若有问题另开 change）。

## 用户影响

- 经域名（含 Cloudflare）访问的用户可正常弹出微信/支付宝扫码支付页。
- VIP/普通用户均可受益（与角色无关）。

## 风险

- 兼容性风险：低。仅修正 IP 字符串格式，接口契约不变。
- 安全风险：低。仍从标准代理头读取客户端 IP，不信任客户端自定义 body。
- 回滚风险：revert `CoursePayController` 本次 commit 即可。

## 验收标准

- [ ] `juegeresource.top` 登录普通用户 → 课程详情 → 确认支付 → 返回二维码且无「IP不合法」。
- [ ] 直连 `:58081` 与经 `:58080` nginx 行为一致。
- [ ] 仅修改 `CoursePayController.java`，无公共工具类 diff。
- [ ] CI `Spec & Process Guard` 通过。

## 关联信息

- 分支：`feature/course_hero_hope_xinghao`
- 控制器：`CoursePayController` → `ICoursePayService.createCoursePay`
- 前端配套：课程详情 `CoursePay.vue` → `/pc/course/pay/create`（osh-frontend）
