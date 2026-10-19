# Redis KEYS 命令安全审查报告

> **审查日期**：2026-09-23  
> **审查范围**：全项目 Java 代码中涉及 Redis 键遍历的所有场景  
> **审查目标**：确保生产环境严禁使用阻塞性 KEYS 命令，统一采用 SCAN 增量遍历

---

## 一、项目环境

| 项目 | 版本 |
|------|------|
| Spring Boot | 2.5.15 |
| Spring Data Redis | 2.5.x（随 Spring Boot 2.5.15） |
| Redisson | 3.17.7 |
| Java | 1.8（Amazon Corretto 1.8.0_492） |

**关键风险点**：在 Spring Data Redis 2.5.x 中，`RedisTemplate.keys(pattern)` 底层**直接执行 Redis KEYS 命令**（非 SCAN）。当 Redis 键空间较大时，KEYS 命令会阻塞 Redis 主线程，导致所有客户端请求超时，严重影响线上服务可用性。

---

## 二、审查结果总览

### 问题统计

| 严重级别 | 数量 | 说明 |
|---------|------|------|
| **P0 严重** | 3 处 | 直接调用 `redisTemplate.keys()`，含通配符 `*` 全量匹配 |
| **P1 高危** | 2 处 | 通过 `redisCache.keys()` 在高频业务路径中遍历 |
| **P2 中危** | 3 处 | 通过 `redisCache.keys()` 在管理操作中遍历 |
| **合规** | 2 处 | 已使用 SCAN 或仅操作精确 key |

### 问题清单

| # | 严重级别 | 文件 | 行号 | 调用方式 | 触发场景 |
|---|---------|------|------|---------|---------|
| 1 | **P0** | `CacheController` | L117 | `redisTemplate.keys("*")` | 清空全部缓存，匹配**所有** key |
| 2 | **P0** | `CacheController` | L83 | `redisTemplate.keys(cacheName + "*")` | 查询缓存键列表 |
| 3 | **P0** | `CacheController` | L100 | `redisTemplate.keys(cacheName + "*")` | 按名称清除缓存 |
| 4 | **P1** | `AdminInviteController` | L318 | `redisCache.keys(INVITE_KEY_PREFIX + "*")` | 每次邀请校验都全量遍历 |
| 5 | **P1** | `SysUserOnlineController` | L45 | `redisCache.keys(LOGIN_TOKEN_KEY + "*")` | 在线用户列表查询 |
| 6 | **P2** | `DictUtils` | L225 | `redisCache.keys(SYS_DICT_KEY + "*")` | 清空字典缓存 |
| 7 | **P2** | `SysConfigServiceImpl` | L190 | `redisCache.keys(SYS_CONFIG_KEY + "*")` | 清空配置缓存 |
| 8 | **P2** | `RedisCache` | L266 | `redisTemplate.keys(pattern)` | 底层封装，直接透传 KEYS 命令 |

### 已合规实现

| 文件 | 说明 |
|------|------|
| `OshGroupServerServiceImpl.deleteKeysByPrefix()` | 已正确使用 `SCAN` + `Cursor` 增量遍历，含失败降级处理，是项目内**标杆实现** |
| `GroupServerMonitorService` | 未使用 KEYS 命令，仅操作精确 key（`setCacheMap`/`expire`/`getCacheMap`），**完全合规** |

---

## 三、问题详细分析

### 3.1 P0 严重：CacheController（3 处）

**文件路径**：`backstage-admin/src/main/java/com/backstage/web/controller/monitor/CacheController.java`

```java
// L83 - 查询缓存键列表
Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");

// L100 - 按名称清除缓存
Collection<String> cacheKeys = redisTemplate.keys(cacheName + "*");

// L117 - 清空全部缓存（最危险）
Collection<String> cacheKeys = redisTemplate.keys("*");
```

**风险分析**：
- 直接使用 `redisTemplate.keys()`，绕过 `RedisCache` 封装层
- `clearCacheAll()` 使用 `"*"` 通配符匹配**全部** key，在键空间达到百万级时，KEYS 命令可能耗时数秒
- 该 Controller 暴露为 HTTP 接口，管理员触发即可造成 Redis 阻塞

### 3.2 P1 高危：AdminInviteController（1 处）

**文件路径**：`backstage-system/src/main/java/com/backstage/system/controller/user/AdminInviteController.java`

```java
// L318 - 邀请校验时全量遍历
Collection<String> keys = redisCache.keys(INVITE_KEY_PREFIX + "*");
```

**风险分析**：
- 位于 `hasPendingInvite()` 方法中，每次邀请操作都会触发
- 除了遍历键空间，还在循环中逐个 `getCacheObject()` 读取值，形成 **O(N) 次 Redis 往返**
- 属于**算法级缺陷**，应通过反向索引彻底消除遍历需求

### 3.3 P1 高危：SysUserOnlineController（1 处）

**文件路径**：`backstage-admin/src/main/java/com/backstage/web/controller/monitor/SysUserOnlineController.java`

```java
// L45 - 在线用户列表查询
Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
```

**风险分析**：
- 在线用户监控为高频查询接口
- 登录 Token 数量可能达到数千甚至数万，KEYS 命令阻塞风险高

### 3.4 P2 中危：DictUtils / SysConfigServiceImpl（2 处）

```java
// DictUtils.java L225
Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(CacheConstants.SYS_DICT_KEY + "*");

// SysConfigServiceImpl.java L190
Collection<String> keys = redisCache.keys(CacheConstants.SYS_CONFIG_KEY + "*");
```

**风险分析**：
- 字典和配置缓存键数量通常较少（数十个），直接危害相对可控
- 但作为基础组件，应保持一致的安全规范

### 3.5 P2 中危：RedisCache.keys() 底层封装（1 处）

**文件路径**：`backstage-common/src/main/java/com/backstage/common/core/redis/RedisCache.java`

```java
// L264-267
public Collection<String> keys(final String pattern)
{
    return redisTemplate.keys(pattern);
}
```

**风险分析**：
- 作为全项目 Redis 操作的统一封装，直接透传 KEYS 命令
- **所有通过 `redisCache.keys()` 调用的代码都受影响**
- 修复此方法可一次性解决 4 个调用方的安全问题

---

## 四、重构方案

### 4.1 核心策略

**从根源修复** `RedisCache.keys()` 方法，内部改用 SCAN 实现。所有调用方自动受益，同时修复 `CacheController` 中直接使用 `redisTemplate.keys()` 的 3 处调用。

### 4.2 根源修复：RedisCache.keys()

将 `RedisCache.keys()` 内部实现从 `redisTemplate.keys()`（KEYS 命令）改为 `connection.scan()`（SCAN 游标增量遍历）：

```java
/**
 * 获得缓存的基本对象列表
 *
 * <p>内部使用 SCAN 游标增量遍历，避免 KEYS 命令阻塞 Redis 主线程。</p>
 *
 * @param pattern 字符串前缀
 * @return 对象列表
 */
public Collection<String> keys(final String pattern)
{
    Set<String> result = new HashSet<>();
    try {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        org.springframework.data.redis.connection.RedisConnection connection =
                Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                result.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } finally {
            try {
                connection.close();
            } catch (Exception ignored) {
                // 连接关闭失败不影响主流程
            }
        }
    } catch (Exception e) {
        // SCAN 失败时返回空集合，由调用方决定降级策略
    }
    return result;
}
```

**新增 import**：

```java
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
```

**关键设计点**：

| 设计点 | 说明 |
|--------|------|
| `SCAN count=200` | 每次游标迭代批量返回约 200 个 key，平衡网络往返与内存占用 |
| `try-with-resources` | 确保 Cursor 正确关闭，避免连接泄漏 |
| `Cursor<byte[]>` | 使用字节数组遍历，避免序列化/反序列化问题，兼容所有 key 编码 |
| 异常降级 | SCAN 失败返回空集合，不抛异常中断业务（与 `OshGroupServerServiceImpl` 降级策略一致） |

### 4.3 修复 CacheController

将 3 处直接使用 `redisTemplate.keys()` 改为通过 `redisCache.keys()` 统一走 SCAN 安全路径：

```java
@Autowired
private RedisCache redisCache;

// L83 修复
Collection<String> cacheKeys = redisCache.keys(cacheName + "*");

// L100 修复
Collection<String> cacheKeys = redisCache.keys(cacheName + "*");

// L117 修复
Collection<String> cacheKeys = redisCache.keys("*");
```

**新增 import**：

```java
import com.backstage.common.core.redis.RedisCache;
```

### 4.4 自动受益文件（无需修改）

以下文件通过 `redisCache.keys()` 调用，根源修复后自动安全：

| 文件 | 行号 | 调用代码 |
|------|------|---------|
| `DictUtils` | L225 | `SpringUtils.getBean(RedisCache.class).keys(SYS_DICT_KEY + "*")` |
| `SysConfigServiceImpl` | L190 | `redisCache.keys(SYS_CONFIG_KEY + "*")` |
| `SysUserOnlineController` | L45 | `redisCache.keys(LOGIN_TOKEN_KEY + "*")` |
| `AdminInviteController` | L318 | `redisCache.keys(INVITE_KEY_PREFIX + "*")` |

---

## 五、长期优化建议

### 5.1 AdminInviteController 反向索引优化

当前 `hasPendingInvite()` 方法每次校验都遍历所有邀请键，建议创建邀请时写入反向索引：

```
创建邀请时：SET invite:email:{email} → token（带 TTL）
校验邀请时：GET invite:email:{email} → 直接获取 token（O(1)）
```

这样可将 O(N) 遍历降为 O(1) 精确查询，彻底消除遍历需求。

### 5.2 建立 Redis 操作规范

建议在项目中建立以下规范并纳入代码审查 Checklist：

| 规范 | 说明 |
|------|------|
| **禁止 KEYS** | 生产代码中禁止直接调用 `redisTemplate.keys()` 或 `connection.keys()` |
| **统一封装** | 所有键遍历操作必须通过 `RedisCache.keys()`（内部使用 SCAN） |
| **SCAN 参数** | count 建议 100~500，避免过小导致过多网络往返或过大导致单次阻塞 |
| **异常降级** | SCAN 操作应包含 try-catch，失败时降级（依赖 TTL 自然过期或返回空结果） |
| **避免循环 GET** | 禁止在遍历 key 的循环中逐个 GET 值，应使用 `pipeline` 或重新设计数据结构 |

### 5.3 CI 静态检查

可考虑在 CI 流水线中添加静态分析规则，自动检测以下模式：

```
redisTemplate.keys(
connection.keys(
```

发现匹配则构建失败，从流程上杜绝 KEYS 命令进入生产代码。

---

## 六、修复影响矩阵

| 修复策略 | 涉及文件 | 修改量 | 效果 |
|---------|---------|--------|------|
| **根源修复** | `RedisCache.keys()` | 1 个方法 | 4 个调用方自动安全 |
| **消除直接调用** | `CacheController` 3 处 | 3 行代码 + 2 行注入 | 消除最危险的 P0 问题 |
| **已合规** | `OshGroupServerServiceImpl` | 无需修改 | 标杆实现，可参考 |
| **已合规** | `GroupServerMonitorService` | 无需修改 | 仅精确 key 操作 |
| **自动受益** | `DictUtils` / `SysConfigServiceImpl` / `SysUserOnlineController` / `AdminInviteController` | 无需修改 | 根源修复后自动安全 |

---

## 七、参考：项目内标杆实现

`OshGroupServerServiceImpl.deleteKeysByPrefix()` 已正确使用 SCAN，可作为项目内参考实现：

```java
/**
 * 按前缀 SCAN 批量删除缓存键。
 * 使用 SCAN 而非 KEYS，避免大 key 空间下阻塞 Redis；SCAN 失败时降级为不删除（依赖 TTL 自然过期）。
 */
private void deleteKeysByPrefix(String prefix) {
    try {
        Set<String> matchedKeys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
        RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                matchedKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } finally {
            try {
                connection.close();
            } catch (Exception ignored) {
                // 连接关闭失败不影响删除流程
            }
        }
        if (!matchedKeys.isEmpty()) {
            redisTemplate.delete(matchedKeys);
        }
    } catch (Exception e) {
        logger.warn("【拼团缓存】按前缀清除缓存失败，将依赖 TTL 自然过期，prefix={}", prefix, e);
    }
}
```
