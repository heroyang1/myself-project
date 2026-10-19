# Tasks: add-prod-nacos-config

## 准备

- [ ] 阅读 `openspec/project.md` 与 `docs/ai/02-workflow.md`
- [ ] 确认 proposal.md/design.md 已通过评审（评审人：运维负责人 + 后端负责人）
- [ ] 向平台团队确认 `osh-secret-manager` 组件的实际接入方式（仓库中未找到对应 Java 客户端实现代码）
- [ ] 由运维在 green/blue 两个生产 Nacos 实例中分别创建 prod 专属 namespace，并记录 UUID（不写入代码仓库）

## 实现

- [x] 创建 `backstage-admin/src/main/resources/bootstrap-prod.yml`，敏感连接信息全部用 `${ENV_VAR}` 占位符
- [x] 创建 `backstage-admin/src/main/resources/application-prod.yml`，仅包含非敏感结构性默认值
- [ ] **（阻塞项，需跨团队协作）** 运维修改服务器端 `docker-compose.osh.yml`（`/opt/osh-deploy-tools/bundle/`，不在本仓库中），为 green/blue 容器新增 `SPRING_PROFILES_ACTIVE=prod` 及 `NACOS_SERVER_ADDR`/`NACOS_NAMESPACE`/`NACOS_PASSWORD`/`OSH_SECRET_SERVER_URL`/`OSH_SECRET_ACCESS_TOKEN`/`JASYPT_ENCRYPTOR_PASSWORD`/`SERVER_PORT` 环境变量，并执行 `docker-compose up -d` 重建容器。**在此之前，本仓库内新增的 `bootstrap-prod.yml`/`application-prod.yml` 处于惰性未激活状态，不影响现有 dev-profile 生产部署。**
- [ ] （依赖上一项完成后）简化 `.github/scripts/patch-149-config.py`：移除对 `server-addr`/`osh.secret.server-url` 的正则替换逻辑，保留主机 IP 替换
- [ ] （依赖上一项完成后）修改 `.github/workflows/deploy-prod.yml`：确认容器重建后环境变量已正确注入，评估是否仍需保留 `patch-149-config.py` 步骤
- [ ] 在测试用 Nacos namespace（非生产）中按模板创建 8 个 `-prod` data-id，验证配置语法与 Jasypt 加解密流程
- [ ] 权限与安全处理：确认 prod namespace 使用独立账号，未复用 dev/test 账号；确认 `jasypt.encryptor.password` 未出现在任何 Nacos 配置文件中

## 验证

- [ ] 本地构建通过：`mvn clean package -DskipTests -pl backstage-admin -am`
- [ ] 用测试 namespace 启动应用（`--spring.profiles.active=prod`），确认 8 个 data-id 全部成功拉取，无 `DecryptionException`
- [ ] 关键接口 smoke 通过：`/api/` 返回 200/401；核心只读业务接口（如课程列表）返回正常数据
- [ ] 在 green 或 blue 单一槽位完成灰度部署，健康检查通过后再考虑另一槽位
- [ ] 回归受影响业务：确认 `patch-149-config.py` 简化后主机 IP 替换逻辑仍正常工作

## PR

- [ ] PR 描述关联本 OpenSpec change（`add-prod-nacos-config`）
- [ ] 填写测试结果（测试 namespace 验证截图/日志）
- [ ] 填写风险与回滚方案（引用 design.md"发布与回滚"章节）
- [ ] 指派 Reviewer（运维负责人 + 后端负责人）

## 生产 Nacos 配置推送（运维执行，不在代码仓库范围内）

> 以下步骤需要运维人员持有 green/blue 生产 Nacos 实例的真实凭证，在评审通过后手工执行，**不由 AI Agent 自动执行**。

- [ ] 在 green Nacos 实例（`osh-g-nacos:8848`）中，按 `design.md` 的 data-id 清单逐一创建/更新 8 个 `-prod` 配置，敏感值由运维手工填入
- [ ] 在 blue Nacos 实例（`osh-nacos:8848`）中重复上述操作
- [ ] 每次推送前先通过 Nacos 控制台导出当前配置做备份，推送后核对配置历史版本
- [ ] 推送完成后，通知后端触发一次 `deploy-prod.yml`（先 green 后 blue），完成环境变量注入方式的首次真实部署验证
