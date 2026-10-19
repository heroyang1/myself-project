# Redis 大 Key 问题深度审计报告

> **审计日期**: 2026-09-23
> **审计范围**: 全项目 Redis 操作代码（backstage-common / backstage-system / backstage-admin / backstage-framework / backstage-quartz / backstage-flink）
> **审计目标**: 识别 Redis 大 Key 风险、危险命令使用、TTL 缺失、集合无上限等问题，并给出可落地的修复方案

---

## 一、风险总览

| 风险等级 | 问题类别 | 涉及文件数 | 影响范围 |
|---------|---------|-----------|---------|
| **P0-致命** | `keys("*")` 全库扫描 | 1 | 生产 Redis 阻塞 |
| **P0-致命** | `keys()` 命令滥用 | 6 | 生产 Redis 阻塞 |
| **P1-严重** | `setCacheObject` 无 TTL | 4 | 内存泄漏 / 大 Key |
| **P1-严重** | List 集合无上限写入 | 1 | 潜在大 Key |
| **P2-中等** | 会话 Map 持续膨胀 | 3 | 单 Key 体积增长 |
| **P2-中等** | 拼团列表缓存无上限 | 1 | 潜在大 Key |
| **P3-低** | 无大 Key 检测机制 | 全局 | 无法感知 / 预防 |

---

## 二、P0 致命风险：`keys()` 命令阻塞

### 2.1 问题说明

`KEYS` 命令时间复杂度 O(N)，N 为 Redis 全库 Key 总数。Redis 是单线程模型，`KEYS` 执行期间 **所有其他请求都被阻塞**。当 Key 数量达到万级以上，阻塞时间可达数百毫秒甚至秒级，导致秒杀、拼团、支付回调等高并发业务全部超时。

### 2.2 全库清空 — 最危险

**文件**: `backstage-admin/.../CacheController.java` L113-L120

```java
@DeleteMapping("/clearCacheAll")
public AjaxResult clearCacheAll() {
    Collection<String> cacheKeys = redisTemplate.keys("*");  // ← 扫描全库所有 Key
    redisTemplate.delete(cacheKeys);
    return AjaxResult.success();
}
```

**风险评估**: 这是 **最危险的 Redis 操作**。`keys("*")` 会遍历整个键空间，生产环境若有 10 万+ Key，可直接导致 Redis 阻塞数秒。

### 2.3 按前缀查询 / 清除 — 6 处

| # | 文件 | 行号 | 代码 | 触发条件 |
|---|------|------|------|---------|
| 1 | `backstage-admin/.../CacheController.java` | L83 | `redisTemplate.keys(cacheName + "*")` | 管理员查看缓存 Key |
| 2 | `backstage-admin/.../CacheController.java` | L100 | `redisTemplate.keys(cacheName + "*")` | 管理员按缓存名前缀清除 |
| 3 | `backstage-admin/.../SysUserOnlineController.java` | L45 | `redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*")` | 查看在线用户列表 |
| 4 | `backstage-system/.../SysConfigServiceImpl.java` | L190 | `redisCache.keys(CacheConstants.SYS_CONFIG_KEY + "*")` | 清空参数缓存 |
| 5 | `backstage-common/.../DictUtils.java` | L225 | `redisCache.keys(CacheConstants.SYS_DICT_KEY + "*")` | 清空字典缓存 |
| 6 | `backstage-system/.../AdminInviteController.java` | L318 | `redisCache.keys(INVITE_KEY_PREFIX + "*")` | 查询邀请邮箱匹配 |

### 2.4 修复方案

#### 方案 A：在 RedisCache 工具类中新增 `scan()` 方法

**文件**: `backstage-common/src/main/java/com/backstage/common/core/redis/RedisCache.java`

在现有 `keys()` 方法下方新增 `scanKeys()` 方法，并标记 `keys()` 为 `@Deprecated`：

```java
/**
 * 使用 SCAN 命令按前缀获取匹配的 Key（不阻塞 Redis）
 * 替代 keys() 方法，避免 O(N) 全库扫描
 *
 * @param pattern 匹配模式（如 "sys_config:*"）
 * @return 匹配的 Key 集合
 */
public Collection<String> scanKeys(final String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
    RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
    try (Cursor<byte[]> cursor = connection.scan(options)) {
        while (cursor.hasNext()) {
            keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
        }
    } catch (Exception e) {
        // SCAN 失败时返回空集合，依赖 TTL 自然过期
        log.warn("【Redis】SCAN 命令执行失败，pattern={}", pattern, e);
    } finally {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
    return keys;
}

/**
 * @deprecated 使用 {@link #scanKeys(String)} 替代，keys() 会阻塞 Redis
 */
@Deprecated
public Collection<String> keys(final String pattern) {
    return redisTemplate.keys(pattern);
}
```

#### 方案 B：逐文件替换

**文件 1**: `backstage-system/.../SysConfigServiceImpl.java` L188-L192

```java
// 改造前
public void clearConfigCache() {
    Collection<String> keys = redisCache.keys(CacheConstants.SYS_CONFIG_KEY + "*");
    redisCache.deleteObject(keys);
}

// 改造后
public void clearConfigCache() {
    Collection<String> keys = redisCache.scanKeys(CacheConstants.SYS_CONFIG_KEY + "*");
    if (keys != null && !keys.isEmpty()) {
        redisCache.deleteObject(keys);
    }
}
```

**文件 2**: `backstage-common/.../DictUtils.java` L223-L227

```java
// 改造前
public static void clearDictCache() {
    Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(CacheConstants.SYS_DICT_KEY + "*");
    SpringUtils.getBean(RedisCache.class).deleteObject(keys);
}

// 改造后
public static void clearDictCache() {
    RedisCache redis = SpringUtils.getBean(RedisCache.class);
    Collection<String> keys = redis.scanKeys(CacheConstants.SYS_DICT_KEY + "*");
    if (keys != null && !keys.isEmpty()) {
        redis.deleteObject(keys);
    }
}
```

**文件 3**: `backstage-admin/.../SysUserOnlineController.java` L45

```java
// 改造前
Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");

// 改造后
Collection<String> keys = redisCache.scanKeys(CacheConstants.LOGIN_TOKEN_KEY + "*");
```

**文件 4**: `backstage-admin/.../CacheController.java` L83 / L100

```java
// 改造前
Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");

// 改造后：引入 RedisCache 的 scanKeys 方法
Collection<String> cacheKeys = redisCache.scanKeys(cacheName + "*");
```

> 注意：CacheController 当前注入的是 `RedisTemplate<String, String>`，需要额外注入 `RedisCache`。

**文件 5**: `backstage-system/.../AdminInviteController.java` L318

```java
// 改造前
Collection<String> keys = redisCache.keys(INVITE_KEY_PREFIX + "*");

// 改造后
Collection<String> keys = redisCache.scanKeys(INVITE_KEY_PREFIX + "*");
```

> **最佳实践参考**: 拼团模块 `OshGroupServerServiceImpl.java` L1097-L1119 的 `deleteKeysByPrefix()` 方法已正确使用 SCAN，可直接复用其模式。

---

## 三、P0 致命风险：`clearCacheAll` 接口下线

### 3.1 问题说明

**文件**: `backstage-admin/.../CacheController.java` L113-L120

```java
@DeleteMapping("/clearCacheAll")
public AjaxResult clearCacheAll() {
    Collection<String> cacheKeys = redisTemplate.keys("*");  // ← 扫描全库所有 Key
    redisTemplate.delete(cacheKeys);
    return AjaxResult.success();
}
```

`keys("*")` 在任何规模的 Redis 实例上都是危险操作。即使有 `@PreAuthorize` 权限保护，一旦管理员误触发或接口被恶意调用，后果严重。

### 3.2 修复方案

**方案 A（推荐）**: 直接删除该接口

```java
// 删除 clearCacheAll 方法
```

**方案 B**: 改为按白名单缓存名逐个清除

```java
@DeleteMapping("/clearCacheAll")
public AjaxResult clearCacheAll() {
    // 仅清除已知的业务缓存前缀
    String[] knownPrefixes = {
        CacheConstants.LOGIN_TOKEN_KEY,
        CacheConstants.SYS_CONFIG_KEY,
        CacheConstants.SYS_DICT_KEY,
        CacheConstants.CAPTCHA_CODE_KEY,
        CacheConstants.REPEAT_SUBMIT_KEY,
        CacheConstants.RATE_LIMIT_KEY,
        CacheConstants.PWD_ERR_CNT_KEY
    };
    for (String prefix : knownPrefixes) {
        Collection<String> keys = redisCache.scanKeys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    return AjaxResult.success();
}
```

---

## 四、P1 严重风险：无 TTL 的 Key 写入

### 4.1 问题说明

调用 `setCacheObject(key, value)` 而 **不设置过期时间**，Key 将永不过期，导致：
1. 内存持续增长，最终触发 Redis `maxmemory` 淘汰策略
2. 如果淘汰不及时，单个 Key 可能持续膨胀为大 Key

### 4.2 用户登录会话 — 无 TTL 覆盖（最严重）

以下 3 处代码在更新用户资产/积分时，重新写入整个 `LOGIN_USER` Map 但 **没有设置 TTL**：

| # | 文件 | 行号 | 代码 |
|---|------|------|------|
| 1 | `backstage-system/.../ToolPurchaseServiceImpl.java` | L355 | `redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap)` |
| 2 | `backstage-system/.../OrderServiceImpl.java` | L1337 | `redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap)` |
| 3 | `backstage-system/.../OshUserServiceImpl.java` | L718 | `redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap)` |

**影响**: 原本登录时设置了 500 分钟 TTL（见 `OshUserServiceImpl.java` L134），但积分/资产变更后变成 **永不过期**。这是一个严重的逻辑缺陷。

### 4.3 修复方案

定义统一的会话 TTL 常量，确保所有写入路径一致：

```java
// 建议在 OshUserConstants 中新增
public static final int LOGIN_EXPIRE_MINUTES = 500;
```

**文件 1**: `backstage-system/.../ToolPurchaseServiceImpl.java` L355

```java
// 改造前
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);

// 改造后
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap,
    OshUserConstants.LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
```

**文件 2**: `backstage-system/.../OrderServiceImpl.java` L1337

```java
// 改造前
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);

// 改造后
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap,
    OshUserConstants.LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
```

**文件 3**: `backstage-system/.../OshUserServiceImpl.java` L718

```java
// 改造前
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);

// 改造后
redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap,
    OshUserConstants.LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
```

### 4.4 系统配置缓存 — 无 TTL

**文件**: `backstage-system/.../SysConfigServiceImpl.java`

涉及 4 处调用（L77, L123, L146, L180），全部无 TTL：

```java
redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue()); // 无 TTL
```

**修复**: 统一添加 24 小时 TTL

```java
// 改造后
redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue(), 24, TimeUnit.HOURS);
```

### 4.5 字典缓存 — 无 TTL

**文件**: `backstage-common/.../DictUtils.java` L31

```java
// 改造前
SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas);

// 改造后
SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas, 24, TimeUnit.HOURS);
```

---

## 五、P1 严重风险：集合类型无上限写入

### 5.1 List 全量写入无分页

**文件**: `backstage-common/.../RedisCache.java` L139-L154

```java
public <T> long setCacheList(final String key, final List<T> dataList) {
    Long count = redisTemplate.opsForList().rightPushAll(key, dataList); // 无上限检查
    return count == null ? 0 : count;
}

public <T> List<T> getCacheList(final String key) {
    return redisTemplate.opsForList().range(key, 0, -1); // 读取全部元素
}
```

**影响**: 如果业务层传入的 `dataList` 包含数千甚至数万条记录，单个 List Key 将包含大量元素。`range(key, 0, -1)` 读取时会一次性将所有元素加载到内存。

### 5.2 修复方案

在 `setCacheList` 中增加元素上限检查：

```java
/** List Key 最大元素数量，超过此值拒绝写入 */
private static final int LIST_MAX_SIZE = 5000;

public <T> long setCacheList(final String key, final List<T> dataList) {
    if (dataList != null && dataList.size() > LIST_MAX_SIZE) {
        throw new IllegalArgumentException(
            "List 数据量过大（" + dataList.size() + "），存在大 Key 风险，请使用分页写入");
    }
    Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
    return count == null ? 0 : count;
}
```

---

## 六、P2 中等风险：拼团列表缓存无上限

### 6.1 问题说明

**文件**: `backstage-system/.../OshGroupServerServiceImpl.java` L240

```java
redisTemplate.opsForValue().set(cacheKey, list, ttlSeconds, TimeUnit.SECONDS);
```

`list` 是 `List<GroupActivityListVO>`，包含所有匹配筛选条件的活动。随着活动数量增长，单个 Key 的体积会持续增大。每个 VO 包含规格、价格、用户列表等嵌套对象，序列化后可能达到数十 KB 甚至更多。

### 6.2 修复方案

对缓存的活动列表设置元素上限，超出时不缓存：

```java
// 缓存上限常量
private static final int CACHE_LIST_MAX_SIZE = 200;

// 写入缓存时增加大小检查
if (list.isEmpty()) {
    redisTemplate.opsForValue().set(cacheKey, "NULL_LIST", CACHE_NULL_SENTINEL_TTL_SECONDS, TimeUnit.SECONDS);
} else if (list.size() > CACHE_LIST_MAX_SIZE) {
    // 列表过大，不缓存，直接返回（避免大 Key）
    logger.warn("【拼团缓存】活动列表过大，跳过缓存，size={}", list.size());
} else {
    long ttlSeconds = randomizeTtlSeconds(CACHE_ACTIVITY_LIST_TTL);
    redisTemplate.opsForValue().set(cacheKey, list, ttlSeconds, TimeUnit.SECONDS);
}
```

---

## 七、P2 中等风险：会话 Map 持续膨胀

### 7.1 LoginUser 会话对象

**文件**: `backstage-common/.../LoginUser.java`

`LoginUser` 包含 `Set<String> permissions`（权限集合）和嵌套的 `SysUser` 对象。通过 `TokenService.java` L159 存储：

```java
redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
```

如果用户权限集合较大（如管理员有数百个权限），序列化后的 JSON 可达数 KB。虽然有 TTL，但并发在线用户量大时，总内存占用可观。

### 7.2 C 端用户会话 Map

**文件**: `backstage-framework/.../OshAuthenticationFilter.java` L147-L152

每次请求都会将实时查询的角色和权限写入 Redis：

```java
Map<String, List<String>> effectivePermission = buildPermission(effectiveRoleIds);
userInfoMap.put(OshUserConstants.ROLE, effectiveRole);
userInfoMap.put(OshUserConstants.PERMISSION, effectivePermission);
redisCache.setCacheObject(loginUserKey, userInfoMap, LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
```

`userInfoMap` 包含 `userId`、`username`、`asset`、`role`、`permission`（嵌套 Map）、`token`、`loginCount` 等字段。

### 7.3 优化建议

1. **权限列表瘦身**: 只缓存权限 ID 列表而非完整的层级 Map，减少序列化体积
2. **拆分缓存**: 将静态字段（userId、username）与动态字段（role、permission）分开缓存
3. **监控告警**: 对 `LOGIN_USER:*` 前缀的 Key 设置内存监控阈值

---

## 八、高并发场景 Key 设计评估

### 8.1 秒杀模块 — 设计优秀 ✅

**文件**: `backstage-common/.../SeckillCacheConstants.java`

| Key 模式 | 类型 | 值大小 | 评估 |
|----------|------|--------|------|
| `seckill:stock:{activityId}:{itemId}` | String | 整数字符串 | ✅ 安全 |
| `seckill:bought_cnt:{activityId}:{itemId}:{userId}` | String | 整数字符串 | ✅ 安全 |
| `seckill:activity:{activityId}` | String | 单活动对象 | ✅ 安全 |
| `seckill:item:{itemId}` | String | 单明细对象 | ✅ 安全 |
| `seckill:order:{activityId}:{itemId}:{userId}` | String | seckillNo 字符串 | ✅ 安全 |
| `seckill:submit_lock:{activityId}:{itemId}:{userId}` | String | 短锁 8s TTL | ✅ 安全 |

**结论**: 秒杀模块 Key 设计粒度合理，每个 Key 只存储单一数据，TTL 动态计算且有 buffer。**不存在大 Key 风险**。

### 8.2 拼团模块 — 设计良好 ✅（除列表缓存外）

| Key 模式 | 类型 | 值大小 | 评估 |
|----------|------|--------|------|
| `group:activity:list:{hash}` | String | 活动列表 VO 集合 | ⚠️ 潜在风险（见第六章） |
| `group:activity:detail:sys:{id}` | String | 单个详情 VO | ✅ 安全 |
| `group:activity:detail:init:{id}` | String | 单个详情 VO | ✅ 安全 |
| `group:monitor:server:{ip}` | Hash | ~10 个监控字段 | ✅ 安全 |
| `group:stats:{activityId}` | Hash | 3 个统计字段 | ✅ 安全 |

### 8.3 分布式锁 — 设计合理 ✅

| 组件 | Key 模式 | 评估 |
|------|----------|------|
| `DistributedLockUtil` | `lock::website:submit:{key}` | ✅ UUID 短字符串，30s TTL |
| `@DistributeLock` 注解 | Redisson 自动管理 | ✅ 支持自动续期 |
| 秒杀提交锁 | `seckill:submit_lock:...` | ✅ 8s 短锁 |
| 拼团支付回调锁 | `lock:group:pay:callback:{activityId}` | ✅ Redisson 锁 |

---

## 九、配置与监控缺失

### 9.1 现状

扫描全部配置文件（`bootstrap.yml`、`application.yml` 等），**未发现任何 Redis 大 Key 检测配置**：

- ❌ 无 `spring.redis.*` 的 `maxmemory` / `maxmemory-policy` 配置（依赖 Nacos 远程配置，本地不可见）
- ❌ 无 Redis `slowlog` 配置
- ❌ 无 Redis `notify-keyspace-events` 配置
- ❌ 无代码级大 Key 检测（如自定义 `RedisSerializer` 拦截大值）

### 9.2 现有监控能力

`CacheController.java` 提供了基础的 Redis INFO 和 commandstats 查看，但 **缺少大 Key 分析能力**。

### 9.3 建议新增大 Key 检测能力

**文件**: `backstage-framework/.../RedisConfig.java`

添加自定义序列化器，在写入时检测值大小：

```java
/**
 * 大 Key 检测序列化器
 * 包装原有序列化器，在写入时检测值大小并告警
 */
public class BigKeyDetectSerializer implements RedisSerializer<Object> {

    private static final Logger log = LoggerFactory.getLogger(BigKeyDetectSerializer.class);

    /** 告警阈值：10KB */
    private static final int WARN_THRESHOLD = 10 * 1024;

    /** 拒绝阈值：100KB */
    private static final int REJECT_THRESHOLD = 100 * 1024;

    private final RedisSerializer<Object> delegate;

    public BigKeyDetectSerializer(RedisSerializer<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        byte[] data = delegate.serialize(value);
        if (data != null && data.length > WARN_THRESHOLD) {
            log.warn("【大Key告警】写入值大小 {}bytes，超过阈值 {}bytes", data.length, WARN_THRESHOLD);
        }
        if (data != null && data.length > REJECT_THRESHOLD) {
            throw new SerializationException(
                "写入值过大（" + data.length + "bytes），超过大Key拒绝阈值 " + REJECT_THRESHOLD + "bytes") {};
        }
        return data;
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        return delegate.deserialize(bytes);
    }
}
```

在 `RedisConfig` 中注册：

```java
@Bean
public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<Object, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);
    BigKeyDetectSerializer detectSerializer = new BigKeyDetectSerializer(serializer);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(detectSerializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(detectSerializer);

    template.afterPropertiesSet();
    return template;
}
```

---

## 十、修复优先级与执行计划

### 第一优先级：P0 致命（建议 1 天内完成）

| 序号 | 修复项 | 涉及文件 | 预估改动 |
|------|--------|---------|---------|
| 1 | 删除或改造 `clearCacheAll` 接口 | `CacheController.java` | ~10 行 |
| 2 | `RedisCache` 新增 `scanKeys()` 方法 | `RedisCache.java` | ~25 行 |
| 3 | 替换所有 `keys()` 调用为 `scanKeys()` | 6 个文件，8 处 | ~16 行 |

### 第二优先级：P1 严重（建议 3 天内完成）

| 序号 | 修复项 | 涉及文件 | 预估改动 |
|------|--------|---------|---------|
| 4 | 修复 3 处 `LOGIN_USER` 无 TTL 写入 | `ToolPurchaseServiceImpl`、`OrderServiceImpl`、`OshUserServiceImpl` | 3 行 |
| 5 | 系统配置缓存添加 TTL | `SysConfigServiceImpl.java` | 4 行 |
| 6 | 字典缓存添加 TTL | `DictUtils.java` | 1 行 |
| 7 | `setCacheList` 增加上限检查 | `RedisCache.java` | ~5 行 |

### 第三优先级：P2 中等（建议 1 周内完成）

| 序号 | 修复项 | 涉及文件 | 预估改动 |
|------|--------|---------|---------|
| 8 | 拼团列表缓存增加大小限制 | `OshGroupServerServiceImpl.java` | ~5 行 |
| 9 | 会话 Map 瘦身评估 | `OshAuthenticationFilter.java` | 需评估 |

### 第四优先级：P3 低（建议 2 周内完成）

| 序号 | 修复项 | 涉及文件 | 预估改动 |
|------|--------|---------|---------|
| 10 | 新增大 Key 检测序列化器 | `RedisConfig.java` | ~40 行 |
| 11 | Nacos 配置 Redis `maxmemory` 策略 | 远程配置 | 配置变更 |
| 12 | 配置 Redis `slowlog` | 远程配置 | 配置变更 |

---

## 十一、正面发现：项目中的最佳实践

审计过程中也发现了多处优秀的 Redis 使用模式，值得在其他模块推广：

### 11.1 拼团模块 SCAN 用法

**文件**: `OshGroupServerServiceImpl.java` L1097-L1119

```java
private void deleteKeysByPrefix(String prefix) {
    Set<String> matchedKeys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
    RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();
    try (Cursor<byte[]> cursor = connection.scan(options)) {
        while (cursor.hasNext()) {
            matchedKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
        }
    } finally {
        try { connection.close(); } catch (Exception ignored) {}
    }
    if (!matchedKeys.isEmpty()) {
        redisTemplate.delete(matchedKeys);
    }
}
```

### 11.2 防缓存雪崩 — 随机 TTL 抖动

```java
private long randomizeTtlSeconds(long baseTtlMinutes) {
    long jitterSeconds = ThreadLocalRandom.current().nextLong(30, 120);
    return baseTtlMinutes * 60 + jitterSeconds;
}
```

### 11.3 防缓存穿透 — 空值哨兵

```java
if (list.isEmpty()) {
    redisTemplate.opsForValue().set(cacheKey, "NULL_LIST", CACHE_NULL_SENTINEL_TTL_SECONDS, TimeUnit.SECONDS);
}
```

### 11.4 秒杀模块细粒度 Key 设计

每个 Key 只存储单一数据（库存数字、用户限购数、活动对象），TTL 动态计算且有 buffer。

### 11.5 缓存降级 — Redis 异常 fallback

```java
try {
    Object cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return (List<GroupActivityListVO>) cached;
} catch (Exception e) {
    logger.warn("【拼团缓存】读取缓存失败，降级为DB查询", e);
}
return queryFromDb();
```

---

## 十二、总结

| 维度 | 评估 |
|------|------|
| **大 Key 现状** | 当前无已确认的超大 Key，但存在多个 **潜在大 Key 风险点** |
| **最紧急问题** | `keys("*")` 和 `keys(prefix+"*")` 的 6+ 处调用，是 **生产事故隐患** |
| **最隐蔽问题** | 3 处 `setCacheObject` 无 TTL 覆盖，使会话 Key 永不过期 |
| **最佳实践** | 拼团模块的 SCAN 用法、秒杀模块的细粒度 Key 设计、随机 TTL 防雪崩 |
| **监控缺口** | 无大 Key 检测、无慢查询配置、无内存淘汰策略可见性 |

**优先修复顺序**: P0 的 `keys()` 替换 > P0 的 `clearCacheAll` 下线 > P1 的 TTL 修复 > P1 的集合上限 > P2 的列表上限 > P3 的检测机制。

---

## 附录 A：根因分析 — 无 TTL 写入的结构性原因

项目中无 TTL 写入的根因在于 `RedisCache` 工具类 **同时暴露了两个 `setCacheObject` 重载**：

```java
// 无 TTL 版本 — 调用方可自由选择，容易遗漏
public <T> void setCacheObject(final String key, final T value)

// 带 TTL 版本 — 正确用法
public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
```

调用方可以自由选择前者，编译器不会强制要求传入 TTL，也没有运行时告警。这是 5 处无 TTL 写入（3 处会话 + 4 处配置 + 1 处字典）得以存在的结构性原因。

**根治方案**: 在 `RedisCache` 封装层收口，而非逐个修补调用方：

```java
/**
 * @deprecated 使用带 TTL 的重载方法，避免写入永不过期的 Key
 */
@Deprecated
public <T> void setCacheObject(final String key, final T value) {
    log.warn("【Redis告警】setCacheObject(key, value) 未设置 TTL，key={}，请迁移到带 TTL 的重载方法", key);
    redisTemplate.opsForValue().set(key, value);
}
```

**对照标杆**:
- 秒杀模块 `SeckillCacheConstants` 的所有键按活动 `endTime` 动态计算 TTL 并预留 buffer
- 拼团列表与详情缓存使用随机 TTL（`randomizeTtlSeconds`）
- 这两类均不存在无 TTL 问题，可作为新缓存设计的模板

---

## 附录 B：修复影响矩阵

| 修复项 | 涉及模块 | 涉及文件 | 改动行数 | 风险 | 依赖 |
|--------|---------|---------|---------|------|------|
| `RedisCache.scanKeys()` | backstage-common | `RedisCache.java` | +25 | 低 | 无 |
| `keys()` → `scanKeys()` 替换 | backstage-admin | `CacheController.java` | ~8 | 低 | 需注入 `RedisCache` |
| `keys()` → `scanKeys()` 替换 | backstage-admin | `SysUserOnlineController.java` | ~2 | 低 | 无 |
| `keys()` → `scanKeys()` 替换 | backstage-system | `SysConfigServiceImpl.java` | ~4 | 低 | 无 |
| `keys()` → `scanKeys()` 替换 | backstage-common | `DictUtils.java` | ~4 | 低 | 无 |
| `keys()` → `scanKeys()` 替换 | backstage-system | `AdminInviteController.java` | ~2 | 低 | 无 |
| `clearCacheAll` 下线/改造 | backstage-admin | `CacheController.java` | ~15 | 中 | 需通知前端 |
| 会话 TTL 补齐 | backstage-system | `ToolPurchaseServiceImpl.java` | 1 | 低 | 无 |
| 会话 TTL 补齐 | backstage-system | `OrderServiceImpl.java` | 1 | 低 | 无 |
| 会话 TTL 补齐 | backstage-system | `OshUserServiceImpl.java` | 1 | 低 | 无 |
| 配置缓存 TTL | backstage-system | `SysConfigServiceImpl.java` | 4 | 低 | 无 |
| 字典缓存 TTL | backstage-common | `DictUtils.java` | 1 | 低 | 无 |
| `setCacheList` 上限 | backstage-common | `RedisCache.java` | +5 | 中 | 需排查调用方 |
| 拼团列表缓存上限 | backstage-system | `OshGroupServerServiceImpl.java` | +5 | 低 | 无 |
| 大 Key 检测序列化器 | backstage-framework | `RedisConfig.java` | +40 | 中 | 需压测验证阈值 |

---

## 附录 C：关联文档

- [Redis KEYS 命令安全审查报告](./Redis_KEYS命令安全审查报告.md) — 针对 `KEYS` 命令的专项审查，与本报告第二章互补
- [Redis 缓存三层防护体系](../notes/全局记忆库导出.md) — 防雪崩 + 防穿透 + 降级 的设计规范

---

## 附录 D：AdminInviteController 算法级优化建议

`AdminInviteController.hasPendingInvite()` 方法（L318）当前逻辑为：遍历所有邀请 Key 逐个匹配邮箱。即使将 `keys()` 替换为 `scanKeys()`，也只消除了阻塞问题，**不消除全量扫描的 O(N) 开销**。

**长期方案**: 创建邀请时写入 `email → token` 反向索引 Key：

```java
// 创建邀请时（已有此逻辑，见 L189）
redisCache.setCacheObject(INVITE_EMAIL_KEY_PREFIX + email, inviteToken, ...);

// 查询时优先走反向索引（已有此逻辑，见 L309-L316）
String inviteToken = redisCache.getCacheObject(INVITE_EMAIL_KEY_PREFIX + email);
if (StringUtils.isNotEmpty(inviteToken)) {
    // 直接命中，O(1)
    return true;
}

// 兜底：改为 SCAN 而非 KEYS（消除阻塞但不消除扫描）
Collection<String> keys = redisCache.scanKeys(INVITE_KEY_PREFIX + "*");
```

反向索引已存在，只需确保创建路径完整覆盖，即可彻底移除兜底遍历逻辑。
