# Spec: prod-nacos-config

## ADDED Requirements

### Requirement: 生产环境使用独立的 prod profile 与 Nacos namespace

系统 SHALL 在部署到生产环境（green/blue 槽位）时，以 `spring.profiles.active=prod` 启动，并连接到对应槽位专属的 Nacos namespace，而非复用 dev 环境的 profile 名称与 namespace。

#### Scenario: green 槽位启动加载 prod 配置

- GIVEN 应用部署在 green 槽位，环境变量 `NACOS_SERVER_ADDR=osh-g-nacos:8848`、`NACOS_NAMESPACE=<green专属UUID>` 已注入
- WHEN 应用启动
- THEN 应用从 green 专属 Nacos namespace 拉取 `osh-backend-prod.yaml` 及 7 个 `osh-backend-*-prod.yaml` 共享配置，日志中不出现 dev namespace 的 UUID

#### Scenario: blue 槽位与 green 槽位配置隔离

- GIVEN green 与 blue 槽位分别使用不同的 Nacos namespace
- WHEN 运维修改 green namespace 中的某个 data-id 内容
- THEN blue 槽位运行的应用不受影响，仍读取 blue namespace 中的原配置

### Requirement: Nacos 连接凭证与 Jasypt 主密钥不得硬编码

系统 SHALL 通过环境变量注入 Nacos 登录密码（`NACOS_PASSWORD`）、Jasypt 主密钥（`JASYPT_ENCRYPTOR_PASSWORD`）、Secret Manager token（`OSH_SECRET_ACCESS_TOKEN`），这些值不得出现在 `bootstrap-prod.yml`、`application-prod.yml` 或任何提交到代码仓库的文件中。

#### Scenario: 代码仓库中不出现明文生产凭证

- GIVEN 检查 `bootstrap-prod.yml` 与 `application-prod.yml` 的完整内容
- WHEN 搜索是否存在明文密码、密钥或 token 字面量
- THEN 不存在任何明文凭证，全部以 `${ENV_VAR}` 占位符形式引用

#### Scenario: Jasypt 主密钥未存放在 Nacos 配置中

- GIVEN 检查 `osh-backend-jasypt-prod.yaml` 的内容
- WHEN 查看是否包含 `jasypt.encryptor.password` 字段
- THEN 该字段不存在，主密钥仅通过容器环境变量注入

### Requirement: 敏感配置字段必须加密存储

系统 SHALL 对数据库密码、Redis 密码、JWT 签名密钥、支付商户密钥、邮箱授权码、ES 账号密码、XXL-Job accessToken 使用 Jasypt `ENC()` 加密后存入 Nacos，不得以明文形式存储。

#### Scenario: 数据库密码以密文形式存储

- GIVEN 查看 `osh-backend-mysql-prod.yaml` 中 `spring.datasource.druid.password` 字段
- WHEN 读取其值
- THEN 值以 `ENC(` 开头、`)` 结尾，不是明文密码

#### Scenario: Jasypt 解密失败时应用拒绝启动

- GIVEN 环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 未注入或值错误
- WHEN 应用启动并尝试解密 `ENC(...)` 字段
- THEN 应用抛出 `DecryptionException` 并启动失败，而不是带着未解密的密文继续运行

### Requirement: 配置推送前必须先备份并可回滚

运维人员 SHALL 在向生产 Nacos 推送新配置前，通过 Nacos 控制台或 OpenAPI 导出当前配置作为备份，推送后若发现问题，SHALL 能通过 Nacos 配置历史版本功能回滚到上一版本。

#### Scenario: 推送失败后回滚

- GIVEN 运维向 green Nacos 实例推送了新的 `osh-backend-mysql-prod.yaml`，但内容有误导致应用启动失败
- WHEN 运维在 Nacos 控制台查看该 data-id 的历史版本
- THEN 能看到推送前的版本，并可一键回滚到该版本

## MODIFIED Requirements

### Requirement: patch-149-config.py 不再替换 Nacos 连接信息

系统 SHALL 简化 `patch-149-config.py`，移除对 Nacos `server-addr` 与 `osh.secret.server-url` 的正则文本替换逻辑，这两类信息改为通过 Docker 环境变量注入；脚本仅保留对残留主机 IP 的替换（若有）。

#### Scenario: 部署脚本不再依赖文本替换设置 Nacos 地址

- GIVEN 查看简化后的 `patch-149-config.py`
- WHEN 检查 `patch_bootstrap_text()` 函数
- THEN 函数中不再包含替换 `server-addr:` 或 `:59100` 的正则逻辑

## REMOVED Requirements

### Requirement: 生产环境复用 dev profile 与 -dev 后缀 data-id 的命名方式

移除原因：命名与实际环境语义不一致，增加误操作风险，且无法借助 namespace/profile 做细粒度权限隔离。
迁移方式：过渡期内 green/blue Nacos 实例同时保留 `-dev` 与 `-prod` 两套 data-id（内容一致），先切换单一槽位验证稳定后，再清理旧命名；清理前需确认没有任何应用实例仍在引用 `-dev` data-id。
