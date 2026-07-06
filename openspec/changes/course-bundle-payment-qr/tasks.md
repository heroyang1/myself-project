# Tasks: course-bundle-payment-qr

## 准备

- [x] 阅读 `openspec/project.md`。
- [x] 确认变更范围限定课程和套餐模块。
- [x] 确认公共支付类不纳入本次改动。

## 实现

- [x] 新增课程/套餐专用支付创建服务。
- [x] 课程购买接入专用支付创建服务。
- [x] 套餐购买接入专用支付创建服务。
- [x] 新增 C 端套餐推荐、支付创建、支付状态接口。
- [x] 套餐封面路径保存和 C 端临时访问链接处理保持在套餐模块内。
- [x] 套餐支付成功后继续发放套餐内课程/电子书权益。

## 验证

- [x] 后端编译通过：`mvn -pl backstage-admin -am -DskipTests compile`。
- [x] 支付平台 RSA `jump` 直连 smoke 返回支付链接。
- [ ] 本地 release 后端重启后完成课程购买二维码页面 smoke。
- [ ] 本地 release 后端重启后完成推荐套餐购买二维码页面 smoke。

## PR

- [x] OpenSpec change 已补充。
- [ ] PR 描述关联 OpenSpec change。
- [ ] 填写测试结果。
- [ ] 填写风险与回滚。
