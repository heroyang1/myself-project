package com.backstage.common.aspect;

import com.backstage.common.annotation.PerformanceMonitor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 接口性能监控切面
 *
 * 拦截标注了 {@link PerformanceMonitor} 的方法，统计执行耗时。
 * <ul>
 *   <li>超过阈值时输出 WARN 日志（含类名、方法名、耗时、阈值）</li>
 *   <li>{@code alwaysLog=true} 时每次调用均输出 INFO 日志</li>
 * </ul>
 *
 * <p>切面优先级：{@code @Order(10)}，在所有业务切面（分布式锁、用户事件等）之外执行，
 * 以捕获包含 AOP 开销在内的完整耗时。</p>
 *
 * @author system
 * @date 2026-07-12
 */
@Aspect
@Component
@Order(10)
public class PerformanceMonitorAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorAspect.class);

    @Around("@annotation(pm)")
    public Object monitor(ProceedingJoinPoint pjp, PerformanceMonitor pm) throws Throwable {
        long startMs = System.currentTimeMillis();
        Throwable thrown = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            long elapsed = System.currentTimeMillis() - startMs;
            logPerformance(pjp, pm, elapsed, thrown);
        }
    }

    private void logPerformance(ProceedingJoinPoint pjp, PerformanceMonitor pm,
                                long elapsed, Throwable thrown) {
        String className = pjp.getSignature().getDeclaringType().getSimpleName();
        String methodName = pjp.getSignature().getName();
        String label = pm.description().isEmpty()
                ? className + "." + methodName
                : pm.description() + "(" + className + "." + methodName + ")";

        if (elapsed > pm.thresholdMs()) {
            logger.warn("[慢接口] {} 耗时 {}ms > 阈值 {}ms, 异常={}",
                    label, elapsed, pm.thresholdMs(), thrown != null ? thrown.getClass().getSimpleName() : "none");
        } else if (pm.alwaysLog()) {
            logger.info("[性能] {} 耗时 {}ms", label, elapsed);
        }
    }
}
