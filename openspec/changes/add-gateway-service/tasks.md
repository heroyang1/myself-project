# Tasks: add-gateway-service

## 准备

- [ ] 阅读 `openspec/project.md` 与 plan-microservice-split-roadmap 总纲
- [ ] 阅读 backstage-framework 现有 TokenFilter、SecurityConfig 与 backstage-common 的 JWT 工具、LoginUser、AjaxResult
- [ ] 确认 upgrade-jdk17-springboot27 已完成（网关按总纲以 SB3 起步，或过渡期用与单体兼容的版本）

## 实现

- [ ] 新建 gateway-service 模块（Spring Cloud Gateway + WebFlux），注册到 Nacos
- [ ] 配置 Phase 0 透明路由：`/**` → lb://backstage-admin，灰度比例默认 0
- [ ] 实现 AuthGlobalFilter：JWT 有效性 + 登录态存在性粗校验，复用 common JWT 工具与 LoginUser key
- [ ] 实现用户上下文透传：注入 X-User-Id/X-User-Key 下游头；入口清洗外部伪造的内部头
- [ ] 实现按用户 ID 尾号 + Nacos 比例的灰度分流能力（仅建能力，不切实流量）
- [ ] 接入 Sentinel 网关流控（或 RequestRateLimiter + Redis）全局限流
- [ ] 实现统一全局异常处理器，异常响应结构对齐 AjaxResult
- [ ] 配置 CORS 全局跨域
- [ ] Nacos 动态路由/灰度/限流配置项落地（namespace/group 规划）

## 验证

- [ ] 本地构建通过，网关独立启动并注册 Nacos
- [ ] smoke 对比：经网关与直连单体访问登录/课程详情/下单/支付回调，响应逐字段一致
- [ ] 鉴权：无效/过期 JWT 返回 401；有效 JWT 下游 @PreAuthorize 正常
- [ ] 灰度分流：用测试路由验证按尾号+比例命中，不影响生产流量
- [ ] 限流：超阈值请求返回约定限流响应
- [ ] CORS 预检通过；C 端/B 端权限不混用回归
- [ ] 多实例水平扩展下鉴权/会话行为一致

## PR

- [ ] PR 描述关联本 OpenSpec change
- [ ] 填写 smoke 对比结果与压测 P99 增量
- [ ] 填写风险与回滚（LB/DNS 指回单体，无损秒级）
- [ ] 指派 Reviewer
