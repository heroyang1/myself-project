# Design: course-pay-client-ip-fix

## 问题定位

### 25 环境实测（2026-06-27）

| 路径 | 结果 |
|------|------|
| `127.0.0.1:58081/pc/course/pay/create` 直连 backend | ✅ 返回 qrcode |
| `127.0.0.1:58080/...` 经 osh-nginx | ✅ 正常 |
| `http://juegeresource.top/...`（Cloudflare） | ❌ `用户IP地址不合法` |

### 根因

原 `CoursePayController.resolveClientIp` 实现：

```java
String ip = request.getHeader("X-Forwarded-For");
if (ip == null || ip.isEmpty()) {
    ip = request.getRemoteAddr();
}
return ip;
```

缺陷：

1. nginx 使用 `$proxy_add_x_forwarded_for` 时，`X-Forwarded-For` 常为 `"客户端IP, 内网IP"`，整串传给拉卡拉网关 → 校验失败。
2. 未读取 Cloudflare 的 `CF-Connecting-IP`（`juegeresource.top` 经 CF 隧道）。
3. 未读取 `X-Real-IP` 等备用头。

通用订单模块使用的 `com.backstage.common.utils.ip.IpUtils.getIpAddr` 已有逗号链解析，但 **课程支付未复用**。

## 方案

在 `CoursePayController` 内实现课程支付专用 `resolveClientIp`：

1. 优先 `CF-Connecting-IP`（Cloudflare 真实客户端 IP）。
2. 否则调用已有 `com.backstage.system.utils.IpUtils.getClientIp(request)`（取 XFF 首段、X-Real-IP、RemoteAddr）。

**不修改** `backstage-system/utils/IpUtils.java` 与 `backstage-common/utils/ip/IpUtils.java`，避免影响非课程模块。

## 影响面

| 组件 | 变化 |
|------|------|
| `CoursePayController` | `create()` 的 clientIp 解析 |
| `CoursePayServiceImpl` | 无 |
| 前端 `CoursePay.vue` | 无 |

## 测试计划

1. 25 机：`a123` 登录 → `POST /pc/course/pay/create` 经 `juegeresource.top` → `code=200` 且含 `qrcode`。
2. 浏览器：课程 5 详情页确认支付 → 展示扫码 UI。
3. 回归：直连 backend 支付仍成功。

## 发布与回滚

- 合并 `feature/course_hero_hope_xinghao` → qa 分支，走 deploy-qa 部署 25。
- **不操作 149 生产**，待 qa 验证后再按发布流程上生产。
- 回滚：revert 本 change 代码 commit。
