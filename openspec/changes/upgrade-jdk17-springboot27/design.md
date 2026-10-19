# Design: upgrade-jdk17-springboot27

## 方案概览

分两步走：第一步统一 JDK 17 运行基线（改 pom 的 java.version、maven-compiler-plugin source/target、启动脚本 JAVA_HOME 与 add-opens 参数）；第二步将 Spring Boot 从 2.5.15 升到 2.7.18，并同步抬升 Spring Cloud 到 2021.0.x、Spring Cloud Alibaba 到与 2021.x 兼容的版本。保持 javax 命名空间与全部业务代码不变，重点回归 Spring Security 5.7（WebSecurityConfigurerAdapter 废弃）、配置属性迁移、以及随 BOM 抬升的 Tomcat/Logback 等传递依赖。

## 现状分析

- 相关模块：全部 8 个 Maven 模块，pom.xml 根 `<properties>` 定义 java.version=1.8、spring-boot.version=2.5.15、spring-cloud.version=2020.0.6、spring-cloud-alibaba.version=2021.1。
- 相关类/接口：backstage-framework 的 SecurityConfig（Spring Security 配置）、各 Config 装配类、Druid/Redis/ES/Kafka 配置。
- 当前数据结构：不变。
- 当前配置：maven-compiler-plugin 3.1（source/target 1.8）；启动脚本 run.bat / ry.sh；Nacos bootstrap.yml；已有 add-opens 反射适配经验（见记忆：JDK 模块化与序列化冲突）。

## 详细设计

### 兼容性矩阵（对齐 project.md 升级原则）

| 组件 | 当前 | 目标 | 破坏性变更 | 受影响模块 | 处理 |
|---|---|---|---|---|---|
| JDK | 8（运行）/17（CI） | 17（统一） | 模块化反射限制、部分内部 API 移除 | 全模块 | 加 add-opens；核对反射/序列化点 |
| Spring Boot | 2.5.15 | 2.7.18 | 配置属性迁移、自动装配调整 | framework/admin | properties-migrator 校验 |
| Spring Security | 5.7.12 | 5.7.x（随 SB2.7） | WebSecurityConfigurerAdapter 废弃 | framework SecurityConfig | 改 SecurityFilterChain Bean 式配置 |
| Spring Cloud | 2020.0.6 | 2021.0.x | bootstrap 处理变化 | admin | 核对 spring-cloud-starter-bootstrap |
| Spring Cloud Alibaba | 2021.1 | 2021.0.x 兼容版 | Nacos 客户端版本 | admin/framework | 核对注册/配置连通 |
| Tomcat | 9.0.106 | 随 SB2.7 BOM | 小版本抬升 | admin | 回归 |
| Logback | 1.2.13 | 随 SB2.7 BOM | 配置兼容 | common/framework | 回归日志输出 |

### 配置迁移

- pom.xml：`<java.version>17</java.version>`；maven-compiler-plugin 升到 3.11+ 并设 `<release>17</release>`；spring-boot.version=2.7.18；spring-cloud.version=2021.0.9；spring-cloud-alibaba 调整到官方与 2021.x 匹配版本；移除对已过时的显式版本覆盖（让 BOM 统一管理 Tomcat/Logback/Spring Security）。
- 引入 `spring-boot-properties-migrator`（仅 dev/启动期）扫描废弃配置项，按报告修正后移除该依赖。
- 启动脚本 run.bat / ry.sh / 容器镜像 JAVA_HOME 切 17，追加 add-opens（如 `--add-opens java.base/java.lang=ALL-UNNAMED` 等按反射报错点补全）。

### 数据设计

| 表/索引/缓存 | 变化 | 迁移方式 | 回滚方式 |
|---|---|---|---|
| MySQL 全部表 | 无 | 不涉及 | 不涉及 |
| ES 索引 | 无 | 不涉及 | 不涉及 |
| Redis 缓存/锁 | 无（仅客户端版本随 BOM 抬升） | 回归 Redisson 加锁 | 还原 pom |

### 中间件影响

- Redis：Redisson 版本随 SB2.7 BOM 抬升，回归 @DistributeLock 与 RLock.tryLock。
- Kafka：spring-kafka 随 BOM 抬升，回归 framework 3 个消费者的消费确认+查单去重。
- Elasticsearch：ES 客户端版本本次不强制升（6.8 客户端在 SB2.7/javax 下仍可用），回归查询连通；ES→7.17 升级留到总纲 P2 单独 change。
- Nacos：客户端随 SCA 版本变化，回归注册与配置拉取；地址禁止带 http:// 前缀。
- R2/S3：x-file-storage/aws-sdk-s3 本次不动，回归签名上传。

## 兼容性

- 老接口是否保留：全部保留，行为不变。
- 老数据是否可读：是，无数据结构变更。
- 前端是否需要同步：不需要。
- 灰度期间新旧版本是否兼容：本升级为整体替换（非灰度），预发验证通过后一次性上生产；如需灰度可先在预发/单节点 JDK17 运行观察。

## 测试计划

- 单元测试：全模块 `mvn test`（Windows+PowerShell 下按项目 Maven 测试执行规范，参数位置注意）。
- 集成测试：应用启动 + Nacos/Kafka/ES/Redis 连通性；SecurityConfig 鉴权链路。
- 接口 smoke：登录、课程详情、下单、支付回调、秒杀加锁、拼团参团。
- 回归范围：兼容性矩阵列出的全部受影响模块；核心链路 P99 压测对比 JDK8 基线。

## 发布与回滚

- 发布步骤：预发部署 JDK17+新 pom → 全量 smoke + 压测 → 生产切 JDK17 运行时 → 部署新构建产物 → 观察 24h。
- 验证命令：`mvn clean package -pl backstage-admin -am "-Dmaven.test.skip=true"`；启动后 `curl` 健康检查 + 核心接口 smoke。
- 回滚步骤：还原 pom.xml 版本 + 运行时 JDK 切回 8（或保留 17 但用旧构建产物）；因无数据变更，回滚无损；Nacos 配置若有改动用版本历史回退。
