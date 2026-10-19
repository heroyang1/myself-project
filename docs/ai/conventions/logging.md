---
title: 日志规范（Logging）
path: docs/ai/conventions/logging.md
status: active          # active | deprecated | draft
owner: @heroyang
created: 2026-10-09
last_updated: 2026-10-09
openspec: none          # 关联的 change-id；无则 none
---

# 日志规范（Logging）

> 对应《阿里巴巴 Java 开发手册（黄山版）》【强制】日志/异常规约，由 `CodingConventionArchitectureTest` 用 ArchUnit 自动把关。

## 铁律

1. **统一 SLF4J 门面**：`private static final Logger log = LoggerFactory.getLogger(X.class);`，占位符 `{}` 输出，**禁止字符串拼接**。
2. **禁止 `System.out.println` / `System.err`**：生产代码一律走日志框架（受控、可分级、可落文件）。
3. **禁止 `e.printStackTrace()`**：异常必须用 `log.error("描述, param={}", param, e)` 输出**完整堆栈**（`e` 作最后一个参数，不占位符）。

## 正确写法

```java
private static final Logger log = LoggerFactory.getLogger(OshOrderServiceImpl.class);

// 普通信息：占位符
log.info("订单创建成功, orderId={}", orderId);

// 异常：描述 + 参数 + 末尾 throwable（自动打堆栈）
try {
    ...
} catch (Exception e) {
    log.error("订单创建失败, userId={}", userId, e);
    throw new ServiceException("订单创建失败");
}
```

## 错误写法（会被 ArchUnit 拦）

| ❌ 错误 | ✅ 改成 |
|---|---|
| `System.out.println(result);` | `log.info("结果={}", result);` |
| `e.printStackTrace();` | `log.error("xxx失败, param={}", param, e);` |

## ArchUnit 落地说明

- `禁止_printStackTrace`：**零违规**，普通规则强制（存量已全注释）。
- `禁止_System_out_println`：存量有违规，用 `FreezingArchRule` **冻结基线**——放行现有、只拦新增，逐步清理。

## 相关

- 外部调用规范 → [api-calls.md](./api-calls.md)
- 分层与包结构 → [../../architecture/package-structure.md](../../architecture/package-structure.md)
