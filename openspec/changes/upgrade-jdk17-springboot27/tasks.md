# Tasks: upgrade-jdk17-springboot27

## 准备

- [ ] 阅读 `openspec/project.md`（升级原则七要素）与 plan-microservice-split-roadmap 总纲
- [ ] 阅读项目已有 JDK 模块化 add-opens 经验记录
- [ ] 在预发环境准备 JDK 17 运行时与压测基线（JDK 8 核心链路 P99 数据）

## 实现

- [ ] pom.xml：java.version→17，maven-compiler-plugin→3.11+（release 17）
- [ ] pom.xml：spring-boot.version→2.7.18，spring-cloud.version→2021.0.x，spring-cloud-alibaba 调整到兼容版本
- [ ] 清理过时显式版本覆盖，交由 BOM 统一管理（Tomcat/Logback/Spring Security）
- [ ] 临时引入 spring-boot-properties-migrator，按报告修正废弃配置后移除
- [ ] SecurityConfig 适配 Spring Security 5.7（WebSecurityConfigurerAdapter → SecurityFilterChain Bean）
- [ ] 启动脚本 run.bat / ry.sh / 容器镜像 JAVA_HOME→17，补 add-opens 参数
- [ ] 按反射/序列化报错点逐一补 add-opens 与代码适配（不改业务逻辑）

## 验证

- [ ] 本地构建通过：`mvn clean package -pl backstage-admin -am "-Dmaven.test.skip=true"`
- [ ] 全模块 `mvn test` 通过
- [ ] 应用 JDK17 启动，Nacos/Kafka/ES/Redis 连通
- [ ] 核心链路 smoke：登录、课程详情、下单、支付回调、秒杀加锁、拼团参团
- [ ] 核心链路 P99 压测对比 JDK8 基线无明显劣化
- [ ] 依赖 CVE 扫描无新增高危漏洞
- [ ] 更新技术基线文档（openspec/project.md 当前技术基线章节 + docs/ai 相关约定）

## PR

- [ ] PR 描述关联本 OpenSpec change
- [ ] 附兼容性矩阵回归结果与压测对比数据
- [ ] 填写风险与回滚（还原 pom + JDK 运行时，无数据变更）
- [ ] 指派 Reviewer
