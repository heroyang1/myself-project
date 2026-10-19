package com.backstage.common.annotation;

import java.lang.annotation.*;

/**
 * 接口/方法性能监控注解
 *
 * 标注在方法上，由 {@link com.backstage.common.aspect.PerformanceMonitorAspect} 拦截，
 * 当方法执行耗时超过阈值时输出 WARN 日志，便于发现慢接口。
 *
 * <p>使用示例：</p>
 * <pre>
 * {@code @PerformanceMonitor(thresholdMs = 500)}
 * public Result handlePaymentSuccess(String orderNo) { ... }
 * </pre>
 *
 * @author system
 * @date 2026-07-12
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerformanceMonitor {

    /**
     * 慢接口告警阈值（毫秒）。
     * 方法执行耗时超过该值时输出 WARN 级别日志。
     * 默认 500ms。
     */
    long thresholdMs() default 500L;

    /**
     * 是否在每次调用时都记录耗时（INFO 级别）。
     * false 时只在超过阈值时输出 WARN。
     */
    boolean alwaysLog() default false;

    /**
     * 自定义方法描述，用于日志中显示可读名称。
     * 为空时使用 类名.方法名。
     */
    String description() default "";
}
