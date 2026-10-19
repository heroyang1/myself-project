# Spec: runtime-upgrade

## MODIFIED Requirements

### Requirement: 运行时与框架基线

系统 SHALL 以 JDK 17 作为统一运行基线，Spring Boot 版本 SHALL 为 2.7.18，且 SHALL 保持 javax 命名空间与全部对外接口行为不变。

#### Scenario: JDK17 构建与启动

- GIVEN 全模块源码与升级后的 pom.xml
- WHEN 在 JDK 17 下执行 `mvn clean package -pl backstage-admin -am "-Dmaven.test.skip=true"` 并启动应用
- THEN 构建成功，应用正常启动并完成 Nacos 注册，无因模块化反射限制导致的启动失败（add-opens 已补全）。

#### Scenario: 接口行为不变

- GIVEN 升级前后的同一批核心接口（登录、课程详情、下单、支付回调、秒杀加锁、拼团参团）
- WHEN 以相同请求分别调用升级前（JDK8/SB2.5）与升级后（JDK17/SB2.7）版本
- THEN 响应结构、状态码、业务结果逐字段一致。

#### Scenario: 中间件连通性回归

- GIVEN 升级后的应用
- WHEN 触发涉及 Redis 锁、Kafka 消费、ES 查询、Nacos 配置拉取的业务
- THEN 各中间件连通正常，分布式锁加解锁、消费去重、查询结果与升级前一致。

#### Scenario: Spring Security 鉴权不回归

- GIVEN Spring Security 5.7 配置改造后的 SecurityFilterChain
- WHEN 已登录用户访问受 @PreAuthorize 保护的接口、未登录用户访问同一接口
- THEN 已登录用户按权限放行、未登录用户被拦截，鉴权语义与升级前一致。

#### Scenario: 性能无明显劣化

- GIVEN 核心链路（秒杀/拼团）在 JDK8 基线下的 P99 数据
- WHEN 在 JDK17 下以同等压力压测
- THEN P99 无显著劣化（在约定阈值内），否则需定位并优化后方可上线。

#### Scenario: 回滚无损

- GIVEN 升级已部署但需回滚
- WHEN 还原 pom.xml 版本并切回 JDK8 运行时（或用旧构建产物）
- THEN 系统恢复到升级前状态，因无数据结构变更，回滚不产生数据不一致。
