---
title: 错误码参考（Error Codes）
path: docs/reference/error-codes.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-10
openspec: none          # 关联的 change-id；无则 none
---

# 错误码参考（Error Codes）

> 码值全部取自真实源码：[`HttpStatus.java`](../../backstage-common/src/main/java/com/backstage/common/constant/HttpStatus.java)、[`AjaxResult.java`](../../backstage-common/src/main/java/com/backstage/common/core/domain/AjaxResult.java)、[`R.java`](../../backstage-common/src/main/java/com/backstage/common/core/domain/R.java)。响应体约定见 [../architecture/data-flow.md](../architecture/data-flow.md)。

## 状态码定义（HttpStatus）

| 码 | 常量 | 含义 |
|---|---|---|
| 200 | SUCCESS | 操作成功 |
| 201 | CREATED | 对象创建成功 |
| 202 | ACCEPTED | 请求已被接受 |
| 204 | NO_CONTENT | 成功但无返回数据 |
| 301 | MOVED_PERM | 资源已被移除 |
| 303 | SEE_OTHER | 重定向 |
| 304 | NOT_MODIFIED | 资源未修改 |
| 400 | BAD_REQUEST | 参数错误（缺少/格式不匹配） |
| 401 | UNAUTHORIZED | 未授权（未登录） |
| 403 | FORBIDDEN | 访问受限 / 授权过期 / 无权限 |
| 404 | NOT_FOUND | 资源或服务未找到 |
| 405 | BAD_METHOD | 不允许的 HTTP 方法 |
| 409 | CONFLICT | 资源冲突或被锁 |
| 415 | UNSUPPORTED_TYPE | 不支持的数据/媒体类型 |
| 500 | ERROR | 系统内部错误 |
| 501 | NOT_IMPLEMENTED | 接口未实现 |
| 601 | WARN | 业务警告（可预期业务异常） |

## 响应体默认码

| 响应体 | 成功 | 失败 | 警告 |
|---|---|---|---|
| `AjaxResult`（B 端） | `success()` → 200 | `error()` → 500 | `warn()` → 601 |
| `R<T>`（C 端） | `R.ok()` → 200 | `R.fail()` → 500 | `R.fail(code,msg)` 自定义 |

## 响应结构（JSON 示例，字段取自真实类定义）

三种响应体**禁混用**，选用规则见 [api-spec.md](./api-spec.md)：

```json
// AjaxResult（B 端单对象/操作，HashMap 结构）
{ "code": 200, "msg": "操作成功", "data": { "userId": 1 } }

// TableDataInfo（B 端分页，字段：total/rows/code/msg）
{ "code": 200, "msg": "查询成功", "total": 100, "rows": [ { "userId": 1 } ] }

// R<T>（C 端统一，含分页时 data 内嵌 PageResponse）
{ "code": 200, "msg": "ok", "data": { "list": [], "total": 0, "pageNum": 1, "pageSize": 10 } }

// 失败态（HTTP 状态码仍 200，结果看 body code）
{ "code": 601, "msg": "库存不足，请重试", "data": null }
```

> 铁律：业务结果看 body `code`，**HTTP 状态恒 200**（仅认证层 401/403 例外，见下文）。

## 异常 → 码 映射（全局异常处理器）

### B 端：`GlobalExceptionHandler`（backstage-framework）

| 异常 | 返回码 | 说明 |
|---|---|---|
| `AccessDeniedException` | 403 | 权限校验失败 |
| `HttpRequestMethodNotSupportedException` | 500 | 请求方式不支持 |
| `ServiceException` | 携带 code 则用该 code，否则 500 | 业务异常 |
| `MissingPathVariableException` / `MethodArgumentTypeMismatchException` | 500 | 参数问题 |
| `BindException` / `MethodArgumentNotValidException` | 500 + 第一条校验消息 | `@Validated` 校验失败（取 `getAllErrors().get(0).getDefaultMessage()`） |
| `DemoModeException` | 演示模式专用处理 | 演示环境写拦截 |
| `RuntimeException` / `Exception` | 500 | 兜底，记日志 |

### C 端：`PcExceptionHandler`（backstage-admin，`@Order(1)`，限定 `/pc` 及 group/redeem/servergroup/order 包）

| 异常 | 返回码 | 说明 |
|---|---|---|
| `ServiceException` | **601** | 业务可预期错误，WARN 级日志，不打堆栈 |
| `Exception` | 500 | 系统错误，ERROR 级日志，返回通用提示"系统错误，请联系管理员" |

> ⚠️ **作用域陷阱（实测源码）**：`PcExceptionHandler` 仅覆盖 `basePackages = {web.controller.pc, system.controller.group/redeem/servergroup/order}`（`@Order(1)`），且**只处理 `ServiceException` 与 `Exception` 两类**。因此：
> - 作用域**内**的 C 端接口 `@Validated` 校验失败 → 被 `Exception` 兜底吞成"系统错误"，**校验消息丢失**；
> - 作用域**外**的 C 端接口（如 `system.controller.course`）→ 由 `GlobalExceptionHandler` 处理，校验消息正常返回。
> 新增 C 端模块若需校验消息直达用户，要么把模块包加入 `basePackages` 并补 `MethodArgumentNotValidException` 处理器，要么留在作用域外走 B 端处理器（返回体是 `AjaxResult` 非 `R`，需评估前端兼容）。

## 认证与权限错误的实际行为（实测源码）

| 场景 | 处理者 | 实际返回 |
|---|---|---|
| B 端 token 缺失/失效 | `AuthenticationEntryPointImpl`（framework/security） | **HTTP 200** + body `{code:401, msg:"请求访问…认证失败，无法访问系统资源"}` |
| B 端 `@PreAuthorize` 拦截失败 | `GlobalExceptionHandler.handleAccessDeniedException` | HTTP 200 + body `{code:403, msg:"没有权限，请联系管理员授权"}` |
| C 端 token 无效/过期 | `OshAuthenticationFilter`（framework/security/filter） | 拒绝并返回 401（行为由 `SecurityBoundaryTest`/`OshAuthenticationFilterRiskFixTest` 用例锚定） |
| C 端 `@OshUserLevel(N)` 等级不足 | 等级校验切面 | 拒绝（错误消息直出，前端按 msg 提示升级） |
| C 端 `@Anonymous` 接口 | 过滤器直接放行 | 不解析 token；需登录态的分支自行返回 `R.fail("请先登录")` |

> 前端判断登录态失效要同时看 body `code`（401/403），不能只看 HTTP 状态码——后端业务响应 HTTP 恒 200。

## 代码中的使用示例（模式提取自真实调用点）

### 1. Service 抛业务异常（C 端秒杀真实代码）

```java
// OshFlashSaleOrderServiceImpl（backstage-admin/pc）——抛出的 msg 直达用户
throw new ServiceException("该时段门票已售罄，请刷新页面查看其他场次");
throw new ServiceException("本时段限量 " + currentLimit + " 张，还剩 " + remaining + " 张，请重试");
```

B 端接收：`{code:500, msg:"该时段门票已售罄…"}`；C 端作用域内接收：`{code:601, msg:"该时段门票已售罄…"}`。
需要自定义码值时用双参构造 `new ServiceException("用户不存在", 404)`（B 端按携带码返回；C 端处理器固定改写 601）。

### 2. Controller 参数校验（全仓 90 处实测模式）

```java
// 真实模式：@Validated @RequestBody，不写 try-catch、不用 BindingResult（全仓 0 处使用）
@PostMapping
public AjaxResult add(@Validated @RequestBody SysConfig config) {
    return toAjax(configService.insertConfig(config));
}
// DTO 字段标注解：@NotBlank(message = "名称不能为空") → 校验失败自动返回
// {code:500, msg:"名称不能为空"}（B 端取第一条消息；C 端 PcExceptionHandler 作用域内消息会丢，见上文陷阱）
```

> 陷阱：DTO 字段没有 `@NotBlank/@NotNull` 或方法没标 `@Validated` = **零校验**，脏数据直入 Service（参见新人教程常见陷阱清单）。

### 3. Controller 直接返回失败态（不抛异常）

```java
return AjaxResult.error("导出失败，请先在系统配置中设置导出路径");   // B 端，code=500
return R.fail("请先登录");                                        // C 端，code=500
return R.fail(HttpStatus.WARN, e.getMessage());                    // C 端自定义码（PcExceptionHandler L42 实测）
```

### 4. 前端消费约定

```js
// HTTP 恒 200，必须按 body code 分支
if (res.code === 200) { /* 成功 */ }
else if (res.code === 401) { /* 跳登录 */ }
else { ElementUI.message.error(res.msg) }  // 601/503/403 等直接展示 msg（msg 已按用户可理解编写）
```

## 使用约定

- 异常类层级：业务异常抛 `ServiceException`（可带 code）；`BaseException` 为领域异常基类；`GlobalException` 系统级。统一由全局处理器兜底转 `AjaxResult.error`，Controller 不逐方法 try-catch。
- 禁 `e.printStackTrace()`（ArchUnit 零违规规则），日志用 `log.error(msg, e)`，写法见 [`docs/ai/conventions/logging.md`](../ai/conventions/logging.md)。
- 业务可预期错误一律 `throw new ServiceException("提示语")`，禁止在 Controller 里 try-catch 自拼错误码。
- C 端与 B 端异常处理器分包隔离，避免互相覆盖。
- 错误消息要对用户可理解、对开发可排查，**不泄露堆栈/敏感信息**。
- 权限失败(403)、参数错误(400)、资源不存在(404)、业务冲突(409/601) 要区分。

## 相关文档

- 错误处理约定 → [`docs/ai/conventions/backend.md`](../ai/conventions/backend.md)、[`docs/ai/conventions/api.md`](../ai/conventions/api.md)
- API 总览 → [api-spec.md](./api-spec.md)
