# Design: add-prod-nacos-config

## 方案概览

新建 `bootstrap-prod.yml` 与 `application-prod.yml`，用 `prod` profile 替代当前生产环境实际沿用的 `dev` profile 命名；Nacos 连接信息（server-addr、namespace、username/password）、Secret Manager 地址、Jasypt 主密钥全部改为通过 Docker 环境变量注入，不再依赖 `patch-149-config.py` 的正则文本替换。green/blue 槽位各自使用独立的 Nacos namespace，8 个共享配置（`osh-backend-{component}-prod.yaml`）按组件拆分，敏感字段统一用 Jasypt `ENC()` 加密或标注为 Secret Manager 托管。

## 现状分析

- 相关模块：`backstage-admin`（`bootstrap.yml`、`bootstrap-local.yml`、`bootstrap-test.yml`、`application-local.yml`）；`.github/scripts/patch-149-config.py`；`.github/workflows/deploy-prod.yml`。
- 相关类/接口：无业务代码改动，仅配置与部署脚本调整。
- 当前数据结构：不涉及。
- 当前配置：`patch-149-config.py` 的 `patch_bootstrap_text()` 仅替换 `server-addr`、`osh.secret.server-url` 和残留 IP，未替换 `spring.profiles.active`、`namespace`、`data-id` 后缀，导致生产环境实际使用 `dev` 命名。

## 详细设计

### 配置文件设计

| 文件 | 位置 | 说明 |
|---|---|---|
| `bootstrap-prod.yml` | `backstage-admin/src/main/resources/` | Nacos 连接信息全部用 `${ENV_VAR}` 占位符，不硬编码 |
| `application-prod.yml` | `backstage-admin/src/main/resources/` | 仅存放非敏感结构性默认值（端口、上传路径、日志级别、Swagger 开关等），敏感信息一律不放本地文件 |

#### bootstrap-prod.yml 关键字段

```yaml
spring:
  profiles:
    active: prod
  cloud:
    nacos:
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD}
      discovery:
        namespace: ${NACOS_NAMESPACE}
        server-addr: ${NACOS_SERVER_ADDR}
      config:
        namespace: ${NACOS_NAMESPACE}
        server-addr: ${NACOS_SERVER_ADDR}
        file-extension: yaml
        shared-configs:
          - data-id: osh-backend-mysql-prod.yaml
          - data-id: osh-backend-redis-prod.yaml
          - data-id: osh-backend-kafka-prod.yaml
          - data-id: osh-backend-mail-prod.yaml
          - data-id: osh-backend-elasticsearch-prod.yaml
          - data-id: osh-backend-jasypt-prod.yaml
          - data-id: osh-backend-xxljob-prod.yaml
osh:
  secret:
    enabled: ${OSH_SECRET_ENABLED:false}
    server-url: ${OSH_SECRET_SERVER_URL}
    access-token: ${OSH_SECRET_ACCESS_TOKEN}
```

#### 环境变量注入（替代 patch-149-config.py 的文本替换）

| 环境变量 | green 槽位 | blue 槽位 |
|---|---|---|
| `NACOS_SERVER_ADDR` | `osh-g-nacos:8848` | `osh-nacos:8848` |
| `NACOS_NAMESPACE` | green 专属 UUID | blue 专属 UUID |
| `NACOS_PASSWORD` | green 专属强密码 | blue 专属强密码 |
| `OSH_SECRET_SERVER_URL` | `http://osh-g-secret-manager:59100` | `http://osh-secret-manager:59100` |
| `SERVER_PORT` | `28081` | `58081` |
| `JASYPT_ENCRYPTOR_PASSWORD` | green 专属主密钥 | blue 专属主密钥 |

**实施约束（经代码核实，2026-09-30 补充）**：`deploy-prod.yml` 当前通过 `docker stop`/`docker start` 重启一个**已存在的容器**（并非 `docker run`/`docker-compose up` 重新创建容器），容器本身由服务器上的 `docker-compose.osh.yml`（位于 `/opt/osh-deploy-tools/bundle/`，不在本代码仓库中）创建并固定了环境变量。因此 `SPRING_PROFILES_ACTIVE=prod` 及上表所有环境变量**无法仅通过修改本仓库的 `deploy-prod.yml` 注入**，必须由运维同步修改服务器端的 `docker-compose.osh.yml`（新增 `environment:` 条目）并执行 `docker-compose up -d` 重建容器，才能真正生效。此项属于跨仓库/跨团队协作依赖，是本变更落地的**前置阻塞条件**，在 `tasks.md` 中单独标注。

在该前置条件满足之前，`bootstrap-prod.yml`/`application-prod.yml` 作为新增文件已被打包进 jar，但由于没有任何机制将 `spring.profiles.active` 切换为 `prod`，这两个文件处于**惰性未激活**状态，不会影响当前仍在运行的 dev-profile 生产部署（该部署仍通过 `patch-149-config.py` 补丁 `bootstrap.yml` 正常工作）。

### Nacos 远程配置清单（8 个 data-id）

| data-id | 主要内容 | 敏感字段处理方式 |
|---|---|---|
| `osh-backend-prod.yaml`（隐式主配置） | `token.secret`、`pay.*` | Jasypt `ENC()` |
| `osh-backend-mysql-prod.yaml` | Druid 连接池、JDBC URL、账号密码 | 密码 Jasypt `ENC()` |
| `osh-backend-redis-prod.yaml` | Redis 连接、密码、lettuce 连接池 | 密码 Jasypt `ENC()` |
| `osh-backend-kafka-prod.yaml` | bootstrap-servers、生产者/消费者参数、`acks=all` | 若启用 SASL，jaas.config 走 Secret Manager |
| `osh-backend-mail-prod.yaml` | SMTP 服务器、账号、授权码 | 密码 Jasypt `ENC()` |
| `osh-backend-elasticsearch-prod.yaml` | ES 地址、TLS、专用账号密码 | 密码 Jasypt `ENC()`，覆盖代码硬编码默认值 |
| `osh-backend-jasypt-prod.yaml` | 加密算法/前后缀配置 | **主密钥本身不存放于此**，只通过 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量注入 |
| `osh-backend-xxljob-prod.yaml` | 调度中心地址、执行器 accessToken | accessToken Jasypt `ENC()` |

各 data-id 的完整 YAML 模板见本次对话记录中"第一部分：推荐设计"章节，此处不重复粘贴，落地时以 `tasks.md` 中引用的模板为准。

### 中间件影响

- Redis：连接密码由明文（dev/test 复用）改为 Jasypt 加密，Redisson 客户端连接模式需在 `osh-backend-redis-prod.yaml` 中显式确认（single/sentinel/cluster），不能沿用默认值。
- Kafka：生产要求 `acks=all`，若启用 SASL 认证需补充 `security.protocol`/`sasl.*` 配置，本次设计预留字段但不强制启用（取决于生产 Kafka 集群实际是否已开启认证）。
- Elasticsearch：`scheme` 由 `http` 改为 `https`，密码需覆盖 `backstage-flink` 中硬编码的默认兜底值 `osh88888888`。
- Nacos：新增 prod 专属 namespace 与账号，权限最小化（只授予 prod namespace 读写权限），不复用 dev/test 账号。
- R2/S3：本次不涉及。

## 兼容性

- 老接口是否保留：不涉及业务接口。
- 老数据是否可读：不涉及业务数据。
- 前端是否需要同步：不需要。
- 灰度期间新旧版本是否兼容：兼容。过渡期内 green/blue Nacos 实例同时保留 `-dev` 与 `-prod` 两套 data-id（内容一致），先切换单一槽位验证，稳定后再清理旧命名，避免一次性切换带来的回滚困难。

## 测试计划

- 单元测试：不涉及业务代码，无需新增单元测试。
- 集成测试：在测试用 Nacos namespace（非生产）中创建全套 `-prod` data-id，用 `--spring.profiles.active=prod` 启动应用，验证 8 个配置全部成功拉取、Jasypt 解密无异常。
- 接口 smoke：启动后调用 `/api/` 确认返回 200/401；调用 1-2 个核心只读业务接口（如课程列表）确认数据库/Redis/ES 连接正常。
- 回归范围：部署流程本身（`patch-149-config.py` 简化后是否仍能正确处理主机 IP 替换）、green/blue 槽位切换流程。

## 发布与回滚

- 发布步骤：
  1. 在测试 Nacos namespace 中先行验证全套配置可正常加载；
  2. 在 green/blue 生产 Nacos 实例中分别创建 `-prod` namespace 与 8 个 data-id（内容按模板，敏感值由运维手工填入，不经过代码仓库）；
  3. 合并 `bootstrap-prod.yml`/`application-prod.yml`/`patch-149-config.py` 简化改动，走正常 CI/CD 部署到单一槽位（如 green）；
  4. 健康检查通过后，人工切换 Nginx upstream 到该槽位，观察一段时间；
  5. 确认稳定后，对另一槽位（blue）重复步骤 3-4；
  6. 两个槽位均切换完成后，标记旧 `-dev` 命名配置为废弃，保留一个发布周期后清理。
- 验证命令：
  ```bash
  docker exec <容器名> env | grep -E "SPRING_PROFILES_ACTIVE|NACOS_SERVER_ADDR|NACOS_NAMESPACE"
  curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:<nginx端口>/api/
  docker logs --tail 200 <容器名> | grep -iE "nacos|jasypt|DecryptionException"
  ```
- 回滚步骤：若某槽位切换后健康检查失败或业务异常，立即将 Nginx upstream 切回另一槽位（该槽位仍运行旧配置），并在问题槽位的 Nacos 中通过配置历史版本功能回滚到上一版本；`bootstrap-prod.yml`/`patch-149-config.py` 的代码改动可通过 Git revert 回滚，不影响另一槽位正常运行。
