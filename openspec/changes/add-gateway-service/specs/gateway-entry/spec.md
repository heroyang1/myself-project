# Spec: gateway-entry

## ADDED Requirements

### Requirement: 统一入口透明转发

系统 SHALL 提供 gateway-service 作为全部对外流量的统一入口，Phase 0 SHALL 以透明反向代理方式将请求原样转发到老单体，且不改变对外 URL、参数与响应结构。

#### Scenario: 经网关访问与直连单体等价

- GIVEN gateway-service 已部署且灰度比例为 0
- WHEN 客户端经网关访问登录、课程详情、下单、支付回调等核心接口
- THEN 响应状态码与 body 与直连 backstage-admin 逐字段一致，前端无需任何改造。

#### Scenario: 网关注册与无状态扩展

- GIVEN 网关多实例部署
- WHEN 实例注册到 Nacos 并承接流量
- THEN 各实例行为一致，会话/鉴权不依赖实例本地状态，可水平扩展。

### Requirement: 双层鉴权

网关 SHALL 承担 JWT 有效性与登录态存在性的粗校验，服务内 SHALL 保留 @PreAuthorize 细校验作为兜底；两层 SHALL 复用同一 JWT 解析与 LoginUser 缓存 key。

#### Scenario: 无效 token 在网关拦截

- GIVEN 请求携带过期或非法 JWT
- WHEN 到达网关 AuthGlobalFilter
- THEN 网关返回 401 且响应结构对齐 AjaxResult，请求不转发到下游。

#### Scenario: 有效 token 透传下游细校验

- GIVEN 请求携带有效 JWT
- WHEN 网关粗校验通过并注入用户上下文头
- THEN 下游服务据此还原 LoginUser，@PreAuthorize 按权限字符串正常放行或拦截，语义与拆分前一致。

#### Scenario: 内部头防伪造

- GIVEN 外部请求自带 X-User-Id 等内部上下文头
- WHEN 请求进入网关
- THEN 网关清洗外部伪造的内部头，仅以自身校验结果重新注入，防止越权。

### Requirement: 灰度分流能力

网关 SHALL 支持按路径前缀路由与按用户 ID 尾号 + 配置比例的灰度分流，用于后续 split-* 切流；Phase 0 灰度比例 SHALL 默认为 0（不切真实流量）。

#### Scenario: 按比例命中新路由

- GIVEN 某路径配置了灰度比例与目标新服务
- WHEN 用户 ID 尾号命中配置比例
- THEN 该请求路由到新服务，未命中的请求仍走老单体；比例为 0 时全部走老单体。

#### Scenario: 路由级秒级回滚

- GIVEN 某新服务在灰度中出现 P0 缺陷
- WHEN 运维将该路径灰度比例调回 0 或指回老单体
- THEN 流量在秒级全部回到老单体，用户无感知，无数据丢失。

### Requirement: 全局限流与统一异常/跨域

网关 SHALL 提供全局限流，超阈值请求返回约定限流响应；SHALL 统一处理跨域与网关层异常，异常响应结构与单体 AjaxResult 兼容。

#### Scenario: 限流触发

- GIVEN 某入口配置了 QPS 阈值
- WHEN 请求速率超过阈值
- THEN 超出部分被限流并返回约定的限流响应结构，不影响阈值内请求。

#### Scenario: CORS 预检

- GIVEN 浏览器发起跨域预检请求
- WHEN 到达网关
- THEN 网关按配置返回正确的 CORS 头，预检通过。
