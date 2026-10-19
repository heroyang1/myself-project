# Redis Lua 脚本与事务使用深度审计报告

> **审计日期**：2026-09-23  
> **审计范围**：backstage-common / backstage-framework / backstage-system / backstage-quartz / backstage-flink  
> **审计目标**：Lua 脚本使用场景、Redis 事务（MULTI/EXEC）使用情况、原子性保证、缺陷诊断与修复方案

---

## 目录

- [一、审计总览](#一审计总览)
- [二、Lua 脚本使用场景详解](#二lua-脚本使用场景详解)
  - [2.1 限流计数器 — RedisConfig.limitScript()](#21-限流计数器--redisconfiglimitscript)
  - [2.2 秒杀库存扣减 — SECKILL_LUA_SCRIPT](#22-秒杀库存扣减--seckill_lua_script)
  - [2.3 秒杀库存归还（超时路径）— RETURN_STOCK_SCRIPT](#23-秒杀库存归还超时路径--return_stock_script)
  - [2.4 分布式锁安全释放 — DistributedLockUtil.releaseLock()](#24-分布式锁安全释放--distributedlockutilreleaselock)
- [三、Redis 事务（MULTI/EXEC/Watch）使用分析](#三redis-事务multiexecwatch使用分析)
- [四、分布式锁实现中的原子性保证](#四分布式锁实现中的原子性保证)
- [五、秒杀场景 Lua 脚本与数据库操作的协同方式](#五秒杀场景-lua-脚本与数据库操作的协同方式)
- [六、缺陷诊断与修复方案](#六缺陷诊断与修复方案)
  - [P0：cancelOrder 用户取消路径未使用 Lua 原子归还](#p0cancelorder-用户取消路径未使用-lua-原子归还)
  - [P1：Kafka 消费者回滚路径缺乏上限保护](#p1kafka-消费者回滚路径缺乏上限保护)
  - [P2：DistributedLockUtil 每次释放创建新 RedisScript 实例](#p2distributedlockutil-每次释放创建新-redisscript-实例)
  - [P3：RedisStatsSink 的 hset + expire 非原子](#p3redisstatssink-的-hset--expire-非原子)
- [七、按模块分类汇总表](#七按模块分类汇总表)
- [八、优化路线图](#八优化路线图)

---

## 一、审计总览

### 1.1 Lua 脚本使用统计

| # | 模块 | 文件 | 脚本用途 | 风险等级 |
|---|------|------|---------|---------|
| 1 | backstage-framework | `RedisConfig.java` | 限流计数器 | ✅ 正常 |
| 2 | backstage-system | `OshSeckillOrderServiceImpl.java` | 秒杀库存原子扣减 | ✅ 正常 |
| 3 | backstage-quartz | `SeckillOrderTimeoutTask.java` | 超时库存原子归还 | ✅ 正常 |
| 4 | backstage-common | `DistributedLockUtil.java` | 分布式锁安全释放 | ✅ 正常 |

### 1.2 Redis 事务使用统计

| 事务类型 | 使用次数 | 说明 |
|---------|---------|------|
| MULTI/EXEC | **0** | 项目中未使用 |
| WATCH | **0** | 项目中未使用 |

**结论**：项目全面采用 Lua 脚本替代 Redis 事务实现原子操作，这是业界公认的更优方案。

---

## 二、Lua 脚本使用场景详解

### 2.1 限流计数器 — RedisConfig.limitScript()

**文件位置**：`backstage-framework/src/main/java/com/backstage/framework/config/RedisConfig.java` L42-L68

**调用入口**：`backstage-framework/src/main/java/com/backstage/framework/aspectj/RateLimiterAspect.java` L49-L73

**注解使用点**：

| 位置 | 限流配置 |
|------|---------|
| `SeckillUserController.java` L130 | `@RateLimiter(key = "seckill:doSeckill:", time = 5, count = 3, limitType = LimitType.IP)` |
| `OshGroupServerController.java` L220 | `@RateLimiter(count = 5, time = 60)` |
| `OshGroupServerController.java` L340 | `@RateLimiter(time = 60, count = 10)` |
| `OshGroupServerController.java` L401 | `@RateLimiter(count = 5, time = 60)` |

**Lua 脚本源码**：

```lua
local key = KEYS[1]
local count = tonumber(ARGV[1])
local time = tonumber(ARGV[2])
local current = redis.call('get', key)
if current and tonumber(current) > count then
    return tonumber(current)
end
current = redis.call('incr', key)
if tonumber(current) == 1 then
    redis.call('expire', key, time)
end
return tonumber(current)
```

**原子性分析**：

| 步骤 | 操作 | 非原子风险 |
|------|------|-----------|
| 1 | GET key | 读到值后，其他请求可能已递增 |
| 2 | 判断 current > count | 判断通过后，其他请求可能已超限 |
| 3 | INCR key | 递增后，其他请求可能同时递增 |
| 4 | EXPIRE key time | 仅首次递增时设置 TTL |

Lua 脚本将上述四步合并为单次 `EVALSHA` 调用，在 Redis 单线程模型下原子执行，彻底消除固定窗口限流的竞态窗口。

**业务价值**：基于固定窗口限流算法，保护秒杀入口和拼团接口免受突发流量冲击。

---

### 2.2 秒杀库存扣减 — SECKILL_LUA_SCRIPT

**文件位置**：`backstage-system/src/main/java/com/backstage/system/service/impl/seckill/OshSeckillOrderServiceImpl.java` L81-L97

**调用位置**：同文件 L215-L225（`doSeckill()` 方法内）

**Lua 脚本源码**：

```lua
local stock = tonumber(redis.call('get', KEYS[1]))
local qty   = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
if stock == nil or stock < qty then
    return -1
end
local bought = tonumber(redis.call('get', KEYS[2])) or 0
if bought + qty > limit then
    return -2
end
redis.call('decrby', KEYS[1], qty)
local newBought = redis.call('incrby', KEYS[2], qty)
if newBought == qty then
    redis.call('expire', KEYS[2], tonumber(ARGV[3]))
end
return 1
```

**参数说明**：

| 参数 | 含义 | 示例 |
|------|------|------|
| KEYS[1] | 库存 Key | `seckill:stock:1:100` |
| KEYS[2] | 用户已购数量 Key | `seckill:bought_cnt:1:100:2001` |
| ARGV[1] | 本次购买数量 | `1` |
| ARGV[2] | 每人限购上限 | `2` |
| ARGV[3] | 已购数量 Key 过期时间（秒） | `(endTime - now) + 7200` |

**返回值约定**：

| 返回值 | 含义 |
|-------|------|
| `1` | 扣减成功 |
| `-1` | 库存不足 |
| `-2` | 超过限购数量 |

**原子性分析**：

该脚本是整个秒杀链路的核心原子操作，保证：

1. **库存校验 + 扣减不可分割**：不会出现"校验通过但扣减时已被他人扣完"
2. **限购校验 + 累加不可分割**：不会出现"通过限购但累加时已超限"
3. **TTL 仅设置一次**：`newBought == qty` 判断首次购买时才设置过期，避免重复 EXPIRE

**业务价值**：Redis 作为权威库存计数器，在高并发场景（如 1000 QPS 抢购 10 件商品）下，Lua 脚本是防超卖的基石。后续通过 Kafka 异步落盘 DB，实现"Redis 预扣 + DB 最终一致"的经典秒杀架构。

---

### 2.3 秒杀库存归还（超时路径）— RETURN_STOCK_SCRIPT

**文件位置**：`backstage-quartz/src/main/java/com/backstage/quartz/task/SeckillOrderTimeoutTask.java` L60-L73

**调用位置**：同文件 L167-L172（`cancelTimeoutOrder()` 方法内）

**Lua 脚本源码**：

```lua
local cur = tonumber(redis.call('GET', KEYS[1])) or 0
local total = tonumber(ARGV[2])
local qty   = tonumber(ARGV[1])
local ttl   = redis.call('PTTL', KEYS[1])
local newVal = math.min(cur + qty, total)
if ttl ~= nil and ttl > 0 then
    redis.call('PSETEX', KEYS[1], ttl, tostring(newVal))
else
    redis.call('SET', KEYS[1], tostring(newVal))
end
return newVal
```

**参数说明**：

| 参数 | 含义 |
|------|------|
| KEYS[1] | 库存 Key |
| ARGV[1] | 归还数量 qty |
| ARGV[2] | totalStock 上限 |

**原子性分析**：

| 步骤 | 操作 | 为何需要原子 |
|------|------|------------|
| 1 | GET stockKey | 获取当前库存值 |
| 2 | PTTL stockKey | 获取剩余 TTL（毫秒） |
| 3 | math.min(cur + qty, total) | 计算归还后值，不超过上限 |
| 4 | PSETEX stockKey ttl newVal | 写回新值并保留原 TTL |

关键设计点：`PTTL` + `PSETEX` 组合保证归还库存不会丢失 Key 的剩余生存时间，避免 Key 变成永不过期。

**业务价值**：超时订单库存回滚时，防止因多次归还（如 DB 侧已归还但 Redis 侧重复归还）导致库存超过 `totalStock` 上限。

---

### 2.4 分布式锁安全释放 — DistributedLockUtil.releaseLock()

**文件位置**：`backstage-common/src/main/java/com/backstage/common/utils/redis/DistributedLockUtil.java` L68-L85

**Lua 脚本源码**：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

**原子性分析**：

`GET` + 值比较 + `DEL` 三步原子执行，保证"只释放自己持有的锁"。

**经典竞态场景**（若无 Lua 保护）：

```
时刻 T1: 线程 A GET lock → 值是 "uuid-A"（确认是自己的锁）
时刻 T2: 锁恰好在此刻过期（TTL 到期）
时刻 T3: 线程 B 加锁成功，lock = "uuid-B"
时刻 T4: 线程 A DEL lock → 误删了线程 B 的锁！
```

Lua 脚本将 GET 和 DEL 合并为原子操作，彻底杜绝此问题。

---

## 三、Redis 事务（MULTI/EXEC/Watch）使用分析

### 3.1 审计结果

通过全量搜索以下关键词，**未发现任何 Redis 事务 API 调用**：

- `MULTI` / `multi()` → 无匹配（搜索结果中的 `MULTI_LINE_STYLE` 为 ToStringBuilder 常量）
- `EXEC` / `exec()` → 无匹配
- `watch(` / `.watch(` → 无匹配

### 3.2 评估：是否需要引入 Redis 事务？

**结论：不需要。**

| 对比维度 | Redis 事务（MULTI/EXEC） | Lua 脚本 |
|---------|------------------------|---------|
| 原子性 | 保证命令顺序执行，但**不支持回滚**（某条命令失败其余仍执行） | 整个脚本作为原子操作，内部可做条件分支 |
| CAS 能力 | 需要 `WATCH` + 乐观锁 + 重试循环 | 天然在单线程中执行，无需额外机制 |
| 网络开销 | 每条命令仍需单独序列化发送 | 一次 `EVALSHA` 调用完成所有操作 |
| 条件执行 | 不支持（所有命令排队后全部执行） | 支持（if/else 分支） |
| 可读性 | 命令序列分散在 Java 代码中 | 脚本内聚，逻辑清晰 |

项目已选择 Lua 脚本作为 Redis 侧原子操作的实现方案，这是业界公认的更优选择。Redis 事务在功能上被 Lua 完全覆盖。

---

## 四、分布式锁实现中的原子性保证

项目采用**双轨制**分布式锁架构：

### 4.1 轻量级锁 — DistributedLockUtil

| 维度 | 实现 |
|------|------|
| **加锁** | `SET NX EX`（单命令原子） |
| **释放** | **Lua 脚本**（GET + 比较 + DEL 原子） |
| **TTL** | 默认 30 秒 |
| **使用场景** | 网站提交防重 |

### 4.2 重量级锁 — DistributeLockAspect + Redisson

| 维度 | 实现 |
|------|------|
| **加锁** | Redisson 内部 Lua 脚本（含可重入、看门狗续期） |
| **释放** | Redisson 内部 Lua 脚本 |
| **TTL** | 可配置，支持自动续期 |
| **使用场景** | 课程资源操作、拼团参团、工具删除、AI 反馈等 |

**`@DistributeLock` 注解使用点**：

| 位置 | 场景 | 配置 |
|------|------|------|
| `OshCourseController.java` L367 | resource:operation | expireTime=10s, waitTime=3s |
| `OshCourseController.java` L431 | resource:operation | expireTime=10s, waitTime=3s |
| `OshCourseController.java` L449 | course:audit:api | expireTime=60s, releaseImmediately=false |
| `OshToolController.java` L218/L242/L298 | resource:operation / tool:delete | expireTime=10s, waitTime=3s |
| `OshGroupServerServiceImpl.java` L703 | group_join | waitTime=3s, expireTime=15s |
| `OshPracticalWebsiteServiceImpl.java` L203 | website_submit | waitTime=0, includeUserId=true |
| `AssistantFeedbackEsServiceImpl.java` L134 | AI 反馈 | - |

### 4.3 秒杀防重短锁

| 维度 | 实现 |
|------|------|
| **加锁** | `setIfAbsent(key, "1", 8, SECONDS)` |
| **释放** | 直接 `delete`（无需值校验，因 TTL 仅 8 秒） |
| **使用场景** | 秒杀提交防重（`seckill:submit_lock:` 前缀） |

---

## 五、秒杀场景 Lua 脚本与数据库操作的协同方式

### 5.1 整体流程

```
用户请求
   │
   ▼
┌──────────────────────────────────────────┐
│  ① Lua 脚本（Redis 内原子执行）            │
│     - 校验库存 (GET stockKey)              │
│     - 校验限购 (GET boughtCntKey)          │
│     - 扣减库存 (DECRBY stockKey qty)       │
│     - 累加已购 (INCRBY boughtCntKey qty)   │
└──────────────┬───────────────────────────┘
               │ 返回 1 = 成功
               ▼
┌──────────────────────────────────────────┐
│  ② 生成 seckillNo（雪花ID）               │
│  ③ 写入 orderKey（Redis，TTL=2min）       │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  ④ 发送 Kafka 消息                        │
│     Topic: SECKILL_ORDER_CREATE_TOPIC     │
│     Body: SeckillOrderMessage             │
└──────────────┬───────────────────────────┘
               │
    ┌──────────┴──────────┐
    │  Kafka Consumer      │
    ▼                      │
┌──────────────────────┐  │
│ checkout() 创建统一订单│  │ 失败 → rollbackRedis()
│ decrStock() DB扣库存  │  │        (increment 归还)
│ insertOrder() 写库    │  │
└──────────────────────┘  │
               │          │
               ▼          │
    延长 orderKey TTL     │
    到支付超时时间         │
```

### 5.2 协同模式总结

| 组件 | 角色 | 一致性保证 |
|------|------|-----------|
| **Redis Lua** | 权威库存计数器，"预扣减" | 原子扣减，防超卖 |
| **Kafka** | 解耦"抢资格"与"建订单" | 削峰填谷，异步落盘 |
| **DB** | 最终一致落盘 | 消费者失败时 rollbackRedis() 归还 |
| **定时任务** | 超时兜底 | 每分钟扫描超时订单，Lua 原子归还库存 |

### 5.3 库存生命周期全景

```
                    ┌───────────────────────────────────┐
                    │         库存状态机                  │
                    └───────────────────────────────────┘

  [初始化]          [扣减]             [归还]
  warmUpCache()     Lua DECRBY         三条路径：
  SET stockKey      ──────────→        ① 用户取消 cancelOrder()
  = available_stock                    ② 超时取消 cancelTimeoutOrder()
                                       ③ 消费者失败 rollbackRedis()
```

---

## 六、缺陷诊断与修复方案

### P0：cancelOrder 用户取消路径未使用 Lua 原子归还

**严重程度**：🔴 高

**文件位置**：`backstage-system/src/main/java/com/backstage/system/service/impl/seckill/OshSeckillOrderServiceImpl.java` L401-L409

**问题代码**：

```java
// 回滚 Redis 库存、已购数量、流程状态 Key
itemMapper.incrStock(order.getItemId(), qty);                          // ① DB 归还
stringRedisTemplate.opsForValue().increment(stockKey, qty);            // ② Redis 归还（无上限！）
Long boughtAfter = stringRedisTemplate.opsForValue().increment(boughtCntKey, -qty); // ③ 已购减少
redisTemplate.delete(orderKey);                                        // ④ 删除流程 Key
```

**缺陷分析**：

| 风险点 | 说明 |
|-------|------|
| **无上限保护** | `increment(stockKey, qty)` 是简单递增，不检查是否超过 totalStock，可能导致库存"膨胀" |
| **DB 与 Redis 非原子** | 步骤 ① 成功后若步骤 ② 失败（Redis 连接超时），DB 库存已加回但 Redis 计数器未加回 |
| **路径不一致** | 超时取消路径 `cancelTimeoutOrder()` 已使用 `RETURN_STOCK_SCRIPT` 带上限原子归还，同一业务两条实现 |

**修复方案**：

```java
// 修复后：复用 RETURN_STOCK_SCRIPT，统一两条路径
// 1. 先从 DB 查询 totalStock 上限
OshSeckillActivityItem item = itemMapper.selectItemById(order.getItemId());

// 2. Redis 原子归还库存（带上限保护）
if (item != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
    stringRedisTemplate.execute(
        RETURN_STOCK_SCRIPT,
        Collections.singletonList(stockKey),
        String.valueOf(qty),
        String.valueOf(item.getTotalStock())
    );
}

// 3. 归还 DB 库存（Redis 成功后再落 DB）
itemMapper.incrStock(order.getItemId(), qty);

// 4. 减少已购数量 + 删除流程 Key
stringRedisTemplate.opsForValue().increment(boughtCntKey, -qty);
redisTemplate.delete(orderKey);
```

**建议**：将 `RETURN_STOCK_SCRIPT` 从 `SeckillOrderTimeoutTask` 提取到公共位置（如 `SeckillCacheConstants` 或新建 `SeckillLuaScripts` 工具类），供两条路径共同引用。

---

### P1：Kafka 消费者回滚路径缺乏上限保护

**严重程度**：🟡 中

**文件位置**：`backstage-framework/src/main/java/com/backstage/framework/consumer/SeckillOrderConsumer.java` L178-L187

**问题代码**：

```java
private void rollbackRedis(SeckillOrderMessage msg, int quantity) {
    stringRedisTemplate.opsForValue().increment(msg.getStockKey(), quantity);    // 无上限
    stringRedisTemplate.opsForValue().increment(msg.getBoughtCntKey(), -quantity);
    redisTemplate.delete(msg.getOrderKey());
}
```

**缺陷分析**：

与 P0 相同，`increment` 归还库存不检查上限。消费者在 checkout 失败或 DB 扣库存失败时调用此方法，若多次重试归还，可能导致 Redis 库存超过 totalStock。

**修复方案**：

在 `SeckillOrderMessage` 消息体中增加 `totalStock` 字段，消费者回滚时使用 Lua 脚本归还：

```java
private void rollbackRedis(SeckillOrderMessage msg, int quantity) {
    // 使用 Lua 脚本原子归还，不超过 totalStock 上限
    if (msg.getTotalStock() != null) {
        stringRedisTemplate.execute(
            RETURN_STOCK_SCRIPT,
            Collections.singletonList(msg.getStockKey()),
            String.valueOf(quantity),
            String.valueOf(msg.getTotalStock())
        );
    } else {
        // 兜底：消息体无 totalStock 时仍用 increment（兼容旧消息）
        stringRedisTemplate.opsForValue().increment(msg.getStockKey(), quantity);
    }
    stringRedisTemplate.opsForValue().increment(msg.getBoughtCntKey(), -quantity);
    redisTemplate.delete(msg.getOrderKey());
}
```

---

### P2：DistributedLockUtil 每次释放创建新 RedisScript 实例

**严重程度**：🟢 低

**文件位置**：`backstage-common/src/main/java/com/backstage/common/utils/redis/DistributedLockUtil.java` L73-L74

**问题代码**：

```java
public boolean releaseLock(String lockKey, String value) {
    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then ...";
    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();  // 每次 new
    redisScript.setScriptText(script);
    redisScript.setResultType(Long.class);
    ...
}
```

**缺陷分析**：

虽然 Spring Data Redis 会缓存脚本 SHA（通过 `EVALSHA` 避免重复传输），但每次调用都创建 `DefaultRedisScript` Java 对象仍有不必要的内存分配和 GC 压力。

**修复方案**：

```java
// 提取为 static final 常量
private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;
static {
    RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();
    RELEASE_LOCK_SCRIPT.setScriptText(
        "if redis.call('get', KEYS[1]) == ARGV[1] " +
        "then return redis.call('del', KEYS[1]) " +
        "else return 0 end"
    );
    RELEASE_LOCK_SCRIPT.setResultType(Long.class);
}

public boolean releaseLock(String lockKey, String value) {
    String key = LOCK_PREFIX + lockKey;
    Long result = stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), value);
    return Long.valueOf(1).equals(result);
}
```

---

### P3：RedisStatsSink 的 hset + expire 非原子

**严重程度**：🟢 低

**文件位置**：`backstage-flink/src/main/java/com/bachstage/group/job_realtime_stats/sink/RedisStatsSink.java` L73-L77

**问题代码**：

```java
jedis.hset(key, "gmv_1h", String.valueOf(result.getGmv()));
jedis.hset(key, "order_count_1h", String.valueOf(result.getOrderCount()));
jedis.hset(key, "last_update", String.valueOf(System.currentTimeMillis()));
jedis.expire(key, KEY_TTL_SECONDS);
```

**缺陷分析**：

| 风险点 | 说明 |
|-------|------|
| **非原子写入** | 4 条独立命令，若 `expire` 调用失败，Key 将无 TTL 成为永久 Key |
| **网络开销** | 4 次网络往返，可优化为 Pipeline 或 Lua |
| **中间状态可见** | 前 3 个 hset 执行后、expire 执行前，其他客户端可能读到不完整的数据 |

**修复方案**：

使用 Lua 脚本合并为原子操作：

```java
private static final String SINK_LUA =
    "redis.call('HMSET', KEYS[1], " +
    "  'gmv_1h', ARGV[1], " +
    "  'order_count_1h', ARGV[2], " +
    "  'last_update', ARGV[3]) " +
    "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4])) " +
    "return 1";

// invoke() 中：
jedis.eval(SINK_LUA,
    Collections.singletonList(key),
    Arrays.asList(
        String.valueOf(result.getGmv()),
        String.valueOf(result.getOrderCount()),
        String.valueOf(System.currentTimeMillis()),
        String.valueOf(KEY_TTL_SECONDS)
    ));
```

---

## 七、按模块分类汇总表

### 7.1 backstage-framework（框架层）

| 文件 | 行号 | 类型 | 用途 | 状态 |
|------|------|------|------|------|
| `RedisConfig.java` | L42-L68 | Lua 脚本 | 限流计数器 Bean 注册 | ✅ 正常 |
| `RateLimiterAspect.java` | L49-L73 | Lua 调用 | AOP 切面执行限流 | ✅ 正常 |
| `SeckillOrderConsumer.java` | L178-L187 | 库存归还 | 消费者回滚（无上限） | ⚠️ P1 |

### 7.2 backstage-common（公共层）

| 文件 | 行号 | 类型 | 用途 | 状态 |
|------|------|------|------|------|
| `DistributedLockUtil.java` | L68-L85 | Lua 脚本 | 分布式锁安全释放 | ⚠️ P2 |

### 7.3 backstage-system（业务层 — 秒杀）

| 文件 | 行号 | 类型 | 用途 | 状态 |
|------|------|------|------|------|
| `OshSeckillOrderServiceImpl.java` | L81-L97 | Lua 脚本 | 原子扣减库存+限购校验 | ✅ 正常 |
| `OshSeckillOrderServiceImpl.java` | L401-L409 | 库存归还 | 用户取消（无上限） | 🔴 P0 |

### 7.4 backstage-quartz（定时任务层）

| 文件 | 行号 | 类型 | 用途 | 状态 |
|------|------|------|------|------|
| `SeckillOrderTimeoutTask.java` | L60-L73 | Lua 脚本 | 超时库存原子归还 | ✅ 正常 |

### 7.5 backstage-flink（流计算层）

| 文件 | 行号 | 类型 | 用途 | 状态 |
|------|------|------|------|------|
| `RedisStatsSink.java` | L73-L77 | 非原子写入 | hset + expire | ⚠️ P3 |

### 7.6 Redis 事务

| 模块 | 使用情况 |
|------|---------|
| 全项目 | **未使用** MULTI / EXEC / WATCH |

---

## 八、优化路线图

```
优先级    修复项                                      预估工作量    影响范围
─────────────────────────────────────────────────────────────────────────────
P0       cancelOrder 对齐 Lua 原子归还                0.5 天       秒杀模块
         - 提取 RETURN_STOCK_SCRIPT 到公共类
         - cancelOrder() 改用 Lua 归还
         - 调整 DB 与 Redis 写入顺序

P1       Kafka 消费者回滚路径 Lua 化                   0.5 天       秒杀模块
         - SeckillOrderMessage 增加 totalStock 字段
         - rollbackRedis() 改用 Lua 归还
         - 兼容旧消息的兜底逻辑

P2       DistributedLockUtil 脚本常量化                0.5 小时     公共模块
         - Lua 脚本提取为 static final 常量
         - 消除每次调用的对象创建

P3       RedisStatsSink 原子写入                      0.5 天       Flink 模块
         - 改用 Lua 脚本 HMSET + EXPIRE
         - 或改用 Pipeline
─────────────────────────────────────────────────────────────────────────────
```

### 验证策略

| 修复项 | 验证方式 |
|-------|---------|
| P0 | 单元测试：模拟 cancelOrder 并发调用，断言 Redis 库存不超过 totalStock |
| P1 | 集成测试：模拟 checkout 失败后回滚，断言库存值正确 |
| P2 | 代码审查：确认 static final 常量正确初始化 |
| P3 | Flink 本地测试：验证 Redis Hash 写入后 TTL 正确设置 |

---

## 附录：Redis Key 前缀速查表

| Key 前缀 | 用途 | 数据类型 | TTL | 所在模块 |
|----------|------|---------|-----|---------|
| `seckill:stock:{activityId}:{itemId}` | 库存计数器 | String (数字) | endTime + 2h | 秒杀 |
| `seckill:bought_cnt:{activityId}:{itemId}:{userId}` | 用户已购数量 | String (数字) | endTime + 2h | 秒杀 |
| `seckill:activity:{activityId}` | 活动信息缓存 | String (JSON) | endTime + 24h | 秒杀 |
| `seckill:item:{itemId}` | 明细信息缓存 | String (JSON) | endTime + 24h | 秒杀 |
| `seckill:order:{activityId}:{itemId}:{userId}` | 未完成订单锁 | String (seckillNo) | 2min → 支付超时 | 秒杀 |
| `seckill:submit_lock:{activityId}:{itemId}:{userId}` | 防重复提交短锁 | String ("1") | 8s | 秒杀 |
| `lock::website:submit:*` | 网站提交分布式锁 | String (UUID) | 30s | 公共 |
| `group:stats:{activityId}` | 拼团实时统计 | Hash | 2h | Flink |
| `rate_limit:*` | 限流计数器 | String (数字) | 配置值 | 框架 |
