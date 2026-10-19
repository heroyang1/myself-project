# Proposal: upgrade-jdk17-springboot27

## 背景

`openspec/project.md` 当前技术基线为 Java 8 运行基线、CI 用 JDK 17 构建，Spring Boot 2.5.15。运行基线与构建基线不一致本身是隐患，且 Spring Boot 2.5 已停止维护。微服务拆分总纲（plan-microservice-split-roadmap）要求新拆服务直接以 Spring Boot 3.2.x 起步，而 SB3 强制 JDK 17+ 与 jakarta 命名空间。为让老单体与新服务能长期并存、共享依赖 BOM 与工具链，必须先把老单体的运行基线统一到 JDK 17，并把 Spring Boot 升到 2.7.18 作为通往 3.x 的过渡跳板（2.7 是官方指定的 3.x 迁移中转版本，提供 spring-boot-properties-migrator 与迁移指引）。

## 目标

- 全模块统一 JDK 17 运行基线（消除运行/构建 JDK 不一致）。
- Spring Boot 2.5.15 → 2.7.18，Spring Cloud 2020.0.6 → 2021.0.x，Spring Cloud Alibaba 2021.1 → 2021.0.x 兼容版本。
- 保持 javax 命名空间不动（javax→jakarta 迁移留给未来单独的 SB3 change，不在本次范围）。
- 保持所有对外接口行为、数据库结构、缓存/ES/Kafka 使用方式不变，纯运行时/依赖版本升级。

## 非目标

- 本次不升级到 Spring Boot 3.x（那是新服务的事，老单体在绞杀完成前停在 2.7）。
- 本次不做 javax→jakarta 包迁移。
- 本次不改动业务代码逻辑、SQL、接口契约。
- 本次不拆分任何服务。

## 用户影响

- 对终端用户：无感知，接口行为不变。
- 对运维：需将生产/测试运行时 JDK 从 8 切换为 17；启动脚本（run.bat/ry.sh）与容器基础镜像需同步；沿用项目已有 add-opens JVM 参数经验处理 JDK 模块化反射限制。
- 对开发者：本地 IDE/编译目标切到 17；部分依赖版本随 BOM 抬升（Tomcat、Logback、Spring Security 等），需回归。
- 对历史数据/缓存/权限：无影响。

## 风险

- 兼容性风险：SB2.7 对配置项、自动装配、Spring Security 5.7 的 API 有破坏性变更（如 WebSecurityConfigurerAdapter 废弃）。缓解：按官方迁移指南逐项核对，SecurityConfig 重点回归。
- 数据风险：无（不动数据库）。
- 性能风险：JDK 17 GC/ JIT 行为变化可能影响吞吐。缓解：预发环境压测对比秒杀/拼团核心链路 P99。
- 安全风险：依赖版本抬升可能引入新的传递依赖漏洞。缓解：升级后跑 OWASP 依赖检查，比对 CVE 清单。
- 回滚风险：中。缓解：版本升级集中在 pom.xml，回滚即还原 pom + JDK 运行时；不涉及数据变更，回滚无损。

## 验收标准

- [ ] 全模块 `mvn clean package -pl backstage-admin -am "-Dmaven.test.skip=true"` 在 JDK 17 下构建通过。
- [ ] 应用在 JDK 17 运行时正常启动，Nacos 注册、Kafka 消费、ES 查询、Redis/Redisson 锁全部连通。
- [ ] 核心链路 smoke 通过：登录、课程浏览、下单、支付回调、秒杀/拼团加锁。
- [ ] 兼容性矩阵（design.md）中列出的受影响模块均已回归。
- [ ] add-opens 等 JVM 参数固化到启动脚本与文档。
- [ ] 依赖 CVE 扫描无新增高危漏洞。

## 关联信息

- 禅道任务：待创建
- PR：待提交
- 设计稿：design.md（本 change 内）
- 参考：`openspec/project.md` 升级原则七要素；plan-microservice-split-roadmap 总纲；项目已有 JDK 模块化 add-opens 经验
