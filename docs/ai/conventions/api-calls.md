---
title: 外部调用规范（Outbound HTTP Calls）
path: docs/ai/conventions/api-calls.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 外部调用规范（Outbound HTTP Calls）

> 约束对第三方/外部系统的 HTTP 调用，由 `CodingConventionArchitectureTest` 用 ArchUnit 把关。

## 本项目的真实约定（重要）

**没有统一的 `ApiClient` 类**，而是**按集成域各建 client**，把裸 HTTP 封装在 client 内：

| 集成域 | client 类 | 底层 |
|---|---|---|
| 汇付支付 | `system.payment.huifu.client.JdkHuifuHttpTransport` | `HttpURLConnection` |
| 百度网盘 | `system.service.resource.impl.BaiduPanClient` | `OkHttpClient` |
| 开源项目 | `system.service.openproject.impl.GitHubOpenProjectClientImpl` | `HttpURLConnection` |
| AI 阅卷 | `common.utils.aijudge.AiUtil` | `OkHttpClient` |

## 铁律

1. **Service/Controller 不直接 `new` 裸 HTTP 客户端**（`RestTemplate` / `OkHttpClient` / `HttpURLConnection` / Apache `HttpClient`）；应注入对应域的 client。
2. **裸 HTTP 只允许出现在 `..client..` / `..integration..` 包内**（新集成请建在 client 包，便于统一超时/重试/日志/脱敏）。
3. client 对外暴露**领域方法**（如 `fooClient.get("/endpoint", Foo.class)`），不外泄 HTTP 细节。

## 正确写法

```java
@Service
public class FooServiceImpl implements FooService {
    private final GitHubOpenProjectClientImpl gitHubClient; // 注入 client
    public FooServiceImpl(GitHubOpenProjectClientImpl gitHubClient) {
        this.gitHubClient = gitHubClient;
    }
    public Foo fetch() {
        return gitHubClient.getRepo(owner, name); // 面向领域方法，不碰 RestTemplate
    }
}
```

## ArchUnit 落地说明

- `裸HTTP客户端仅限client包`：存量有违规（如 `PayServiceImpl` 直接用 `RestTemplate`），用 `FreezingArchRule` **冻结基线**——放行现有、只拦新增。新代码若在 client/integration 之外引入裸 HTTP 会直接失败。

## 相关

- 日志规范 → [logging.md](./logging.md)
- 分层与包结构 → [../../architecture/package-structure.md](../../architecture/package-structure.md)
