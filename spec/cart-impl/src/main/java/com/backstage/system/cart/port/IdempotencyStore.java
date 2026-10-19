package com.backstage.system.cart.port;

import java.util.function.Supplier;

/**
 * 幂等端口（R5）。以客户端 token 为键，同一 token 的重复提交只执行一次真实写入，
 * 后续调用直接返回首次结果。生产由 Redis SETNX + TTL 实现（TTL 时长属开放问题 Q3）。
 */
public interface IdempotencyStore {

    <T> T executeOnce(String token, Supplier<T> action);
}
