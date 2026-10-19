package com.backstage.common.utils.redis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author xuanqing
 * @create 2026-04-15 15:40
 */
/**
 * 分布式锁工具类
 * 基于 Redis SETNX 实现，保证同一用户同一时刻只有一个请求能执行业务逻辑
 */
@Component
public class DistributedLockUtil {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockUtil.class);

    /** 锁统一前缀，防止key冲突 */
    private static final String LOCK_PREFIX = "lock::website:submit:";
    // 锁的默认过期时间（秒），防止业务异常导致锁永远不释放
    private static final long DEFAULT_EXPIRE_SECONDS = 30L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey       锁的 key（格式：lock:website:submit:userId）
     * @param value         锁的值（用 UUID，释放时校验归属，防止误删别人的锁）
     * @param expireSeconds 锁的过期时间（秒）
     * @return true=获取成功  false=锁已被占用
     */
    public boolean tryLock(String lockKey, String value, long expireSeconds) {
        try {
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, value, expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        }catch (Exception e){
            // Redis异常不影响业务，仅记录日志便于排查
            log.warn("获取分布式锁异常，lockKey={}", lockKey, e);
            return false;
        }

    }

    /**
     * 尝试获取分布式锁（默认 30 秒过期）
     */
    public boolean tryLock(String lockKey, String value) {
        return tryLock(lockKey, value, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 释放分布式锁
     * 只释放自己持有的锁，防止误删其他请求的锁
     *
     * @param lockKey 锁的 key
     * @param value   加锁时设置的值
     * @return true=释放成功  false=锁不存在或不属于自己
     */
    public boolean releaseLock(String lockKey, String value) {
        String key = LOCK_PREFIX + lockKey;
        //Lua脚本：判断值相等，再删除，原子执行
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            // 执行脚本，返回1=删除成功，0=失败
            Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), value);
            return Long.valueOf(1).equals(result);
        }catch (Exception e){
            // 释放锁异常不影响业务，仅记录日志便于排查
            log.warn("释放分布式锁异常，lockKey={}", lockKey, e);
            return false;
        }

    }
}
