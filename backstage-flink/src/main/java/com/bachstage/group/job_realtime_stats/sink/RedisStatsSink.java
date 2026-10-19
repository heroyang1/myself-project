package com.bachstage.group.job_realtime_stats.sink;

import com.bachstage.group.job_realtime_stats.config.GroupRealTimeStatsConfig;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis 统计 Sink（Commons Pool2 连接池版本）
 *
 * <p>将聚合后的统计结果写入 Redis Hash：</p>
 * <pre>
 *   Key:    group:stats:{activityId}
 *   Field:  gmv_1h, order_count_1h, last_update
 * </pre>
 *
 * <p>使用 JedisPool（底层 Commons Pool2）替代单 Jedis 连接，提供：</p>
 * <ul>
 *   <li>连接池化：避免单点连接断开导致 Sink 不可用</li>
 *   <li>自动重连：Pool 管理连接生命周期</li>
 *   <li>并发安全：每个 invoke 从 Pool 借用独立连接</li>
 * </ul>
 *
 * @author system
 * @date 2026-07-12
 */
public class RedisStatsSink extends RichSinkFunction<GroupStatsResult> {

    private static final Logger log = LoggerFactory.getLogger(RedisStatsSink.class);
    private static final long serialVersionUID = 1L;

    /** Redis key TTL: 2 hours (rolling window data expires naturally) */
    private static final int KEY_TTL_SECONDS = 7200;

    private final GroupRealTimeStatsConfig config;
    private transient JedisPool jedisPool;

    public RedisStatsSink(GroupRealTimeStatsConfig config) {
        this.config = config;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        // Commons Pool2 连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);          // 最大连接数（Flink parallelism=1 场景足够）
        poolConfig.setMaxIdle(4);           // 最大空闲连接
        poolConfig.setMinIdle(1);           // 最小空闲连接（保持热连接）
        poolConfig.setTestOnBorrow(true);   // 借用连接时检测可用性
        poolConfig.setTestWhileIdle(true);  // 空闲时定期检测
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWaitMillis(5000);  // 等待连接最大时间

        String password = config.getRedisPassword();
        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 10000, password);
        } else {
            jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 10000);
        }

        log.info("【拼团统计】Redis 连接池已初始化: {}:{}, maxTotal=8", config.getRedisHost(), config.getRedisPort());
    }

    @Override
    public void invoke(GroupStatsResult result, Context context) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "group:stats:" + result.getActivityId();
            jedis.hset(key, "gmv_1h", String.valueOf(result.getGmv()));
            jedis.hset(key, "order_count_1h", String.valueOf(result.getOrderCount()));
            jedis.hset(key, "last_update", String.valueOf(System.currentTimeMillis()));
            jedis.expire(key, KEY_TTL_SECONDS);
            log.info("【拼团统计】Redis 写入成功: activityId={}, gmv={}, orderCount={}",
                    result.getActivityId(), result.getGmv(), result.getOrderCount());
        } catch (Exception e) {
            log.error("【拼团统计】Redis 写入失败: activityId={}", result.getActivityId(), e);
        }
    }

    @Override
    public void close() {
        if (jedisPool != null) {
            try {
                jedisPool.close();
                log.info("【拼团统计】Redis 连接池已关闭");
            } catch (Exception e) {
                log.warn("【拼团统计】Redis 连接池关闭失败", e);
            }
        }
    }
}
