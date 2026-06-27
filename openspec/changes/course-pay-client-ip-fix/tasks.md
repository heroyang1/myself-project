# Tasks: course-pay-client-ip-fix

## 准备

- [x] 25 环境复现：`juegeresource.top` 支付报「用户IP地址不合法」，直连 `:58081` 正常。
- [x] 确认根因为 `CoursePayController.resolveClientIp` 整段 XFF 传网关。
- [x] 确认不改动公共 `IpUtils`，逻辑限定在课程 Controller。

## 实现

- [x] `CoursePayController`：`resolveClientIp` 优先 `CF-Connecting-IP`，否则 `IpUtils.getClientIp`。
- [x] 新增 OpenSpec change `course-pay-client-ip-fix`。

## 验证

- [ ] qa 部署 25 后，`juegeresource.top` 课程支付弹出二维码。
- [ ] 普通用户 / VIP 降级用户均可支付（与角色无关）。
- [ ] CI `Spec & Process Guard` 通过。

## PR

- [ ] PR 描述关联 `openspec/changes/course-pay-client-ip-fix`。
- [ ] 填写 qa 验证结果。
