# Proposal: fix-pc-endpoint-cside-auth

## 背景

全应用唯一认证过滤器为 C 端 `OshAuthenticationFilter`（B 端 `JwtAuthenticationTokenFilter` 已在 `SecurityConfig` 中注释）。
Security 上下文 principal 为 `OshUserDetail`，而 `@PreAuthorize("@ss.hasPermi(...)")` 内部经
`SecurityUtils.getLoginUser()` 强转 B 端 `LoginUser`，必然抛异常/拒绝。

权限链路盘点（2026-08-28）发现 5 个 `/pc/` Controller 残留 12 处 `@ss.hasPermi`，这些端点当前**必然 403**：

| Controller | 端点 | 权限码 |
|---|---|---|
| OshOrderSaveController | GET /{id}、PUT | order:save:query、order:save:edit |
| OshOrderListController | export、GET /{id}、POST、PUT | system:list:export、system:list:query、system:list:add、system:list:edit |
| OshGroupOrderController | list、export、GET /{id}、PUT | system:order:list、system:order:export、system:order:query、system:order:edit |
| WxPayController | PUT | system:wxpay:edit |
| OshGroupServerController | GET /monitor/server | group:monitor:server |

且上述权限码均未注册到 C 端 `osh_permission` 表。

## 目标

- 将 12 处 `@ss.hasPermi` 替换为 C 端机制 `@PreAuthorize("hasAuthority('...')")`（权限码保持不变）。
- 新增权限初始化脚本，将 12 个权限码注册到 `osh_permission`，并按"管理端敏感操作"规则仅关联
  核心开发者（role_id=6）与创始人（role_id=7）。
- 修复后管理端角色可正常访问这些端点，普通用户被正确拦截。

## 非目标

- 不改动 `@Anonymous` 端点（其匿名可达属历史形态，Service 层权限审查另立任务）。
- 不重启 B 端 `JwtAuthenticationTokenFilter`。
- 不调整既有 `group:*` 权限关联。

## 用户影响

- 管理端（role_id=6/7）恢复对上述 12 个端点的访问（当前为全量 403）。
- 普通用户访问仍被拒绝（从"必然 403"变为"权限校验后 403"），无越权风险。

## 风险

- 权限面变化：从"所有人 403"放开为"role_id=6/7 可访问"，属恢复预期行为；上线前需执行权限脚本。
- 回滚：代码还原注解即可；权限数据可保留（NOT EXISTS 幂等，重复执行无副作用）。

## 验收标准

- [ ] 12 个端点以 role_id=6/7 登录访问不再 403。
- [ ] 普通用户（role_id=1~5）访问返回 403。
- [ ] 权限脚本可重复执行无重复记录。

## 关联信息

- 扫描依据：记忆《C端/pc接口混用B端PreAuthorize导致403》（`9eb79e0e`，提交 `81dd573a`）、《禁止注释或移除权限注解（安全红线）》
- SQL：`sql/2026/08/pc_endpoint_cside_permissions.sql`
