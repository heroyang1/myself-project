# Proposal: add-prod-nacos-config

## 背景

现有生产部署（149.88.92.159，green/blue 槽位）通过 `patch-149-config.py` 对打包产物中的 `bootstrap.yml` 做文本热补丁，将 Nacos `server-addr` 替换为槽位专属实例（green: `osh-g-nacos:8848`，blue: `osh-nacos:8848`），但**未修改**：

- `spring.profiles.active`（仍是 `dev`）；
- `discovery.namespace` / `config.namespace`（仍是 dev 环境 UUID `d4208eb8-261f-44a1-adf5-c12406fbd1a2`）；
- `shared-configs` 的 `data-id` 后缀（仍是 `-dev`，如 `osh-backend-mysql-dev.yaml`）。

即生产环境实际运行在名为 "dev" 的 profile 与 data-id 命名下，仅靠"指向不同 Nacos 服务器实例"区分环境。由此产生的技术债：

1. 命名与实际环境语义不一致，新人/运维易误判"-dev"后缀配置为开发环境专用，增加误操作风险；
2. Nacos 登录密码、Secret Manager token 等敏感连接信息依赖正则文本替换处理，而非标准环境变量注入，脆弱且难以审计；
3. 无法在同一 Nacos 实例内借助 namespace/profile 做更细粒度的权限隔离。

## 目标

- 建立语义清晰的 `prod` profile 配置体系：`bootstrap-prod.yml`、`application-prod.yml`，以及 8 个 `-prod` 后缀的 Nacos data-id。
- 将 Nacos 连接凭证、Jasypt 主密钥、Secret Manager token 从"硬编码 + 文本替换"改为"环境变量注入"，移除 `patch-149-config.py` 对这些敏感字段的正则替换依赖。
- 明确标注每个 data-id 中哪些字段需 Jasypt 加密或通过 OSH Secret Manager 托管。
- 产出可复用的 Nacos 配置推送脚本与验证步骤，供运维在 green/blue 两个槽位分别执行。

## 非目标

- 本次不改变业务代码逻辑、接口契约、数据库结构。
- 本次不迁移 dev/test 环境现有命名（`-dev`/`-test` 后缀保持不变）。
- 本次不引入 Actuator/Prometheus 等可观测性组件（属于 `add-observability-stack` 变更范围）。
- 本次不改变 `patch-149-config.py` 对主机 IP 替换的核心逻辑（仅移除对 Nacos `server-addr`/`osh.secret.server-url` 的文本替换，改为环境变量注入）。

## 用户影响

- 对最终用户（网站访问者）无感知，属于纯基础设施配置调整。
- 对运维/开发：需要在 green/blue 两个 Nacos 实例中分别创建 `-prod` 后缀的 namespace 与 data-id，并配置对应环境变量；过渡期新旧命名并存，需按 `tasks.md` 顺序逐步切换。

## 风险

- 兼容性风险：切换过程中若 `bootstrap-prod.yml` 与 `patch-149-config.py`/`deploy-prod.yml` 未同步更新，可能导致部署时配置加载失败；通过"新旧命名并存过渡期"降低风险。
- 数据风险：不涉及业务数据变更，仅涉及配置数据；Nacos 自带配置历史版本，误操作可回滚到上一版本。
- 性能风险：无。
- 安全风险：涉及生产数据库密码、Redis 密码、JWT 密钥、支付密钥等敏感信息迁移，若 Jasypt 主密钥或环境变量注入配置错误，可能导致应用启动时解密失败而无法启动；需先在测试 namespace 中验证。
- 回滚风险：green/blue 双槽位架构本身提供回滚缓冲（若一个槽位切换后异常，可直接切回另一槽位的旧配置），风险可控。

## 验收标准

- [ ] `bootstrap-prod.yml`、`application-prod.yml` 已创建并通过本地 `--spring.profiles.active=prod` 启动验证（连接测试用 Nacos namespace，非生产）。
- [ ] 8 个 `-prod` data-id 已在测试 Nacos namespace 中创建，内容与 `design.md` 一致，敏感字段已用 Jasypt `ENC()` 加密或标注为环境变量占位符。
- [ ] `patch-149-config.py` 已移除对 Nacos `server-addr`/`osh.secret.server-url` 的文本替换逻辑，改为在 `deploy-prod.yml` 中通过 Docker 环境变量注入。
- [ ] 在 green 或 blue 单一槽位完成灰度切换，健康检查（`/api/` 返回 200/401）通过，业务核心接口冒烟测试通过。
- [ ] 全量切换完成后，旧的 `-dev` 命名配置在生产 Nacos 实例中被标记为废弃（保留至少一个发布周期后再清理）。

## 关联信息

- 禅道任务：（待补充）
- PR：（待创建）
- 设计稿：本 change 的 `design.md`
- 线上日志：切换后需关注 Nacos 配置拉取日志与 Jasypt 解密日志
