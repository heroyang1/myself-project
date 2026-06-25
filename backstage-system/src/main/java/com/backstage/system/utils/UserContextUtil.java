package com.backstage.system.utils;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.ServletUtils;
import com.backstage.common.utils.jwt.JwtUtil;
import com.backstage.system.domain.user.OshUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前用户上下文工具类。
 * <p>
 * 统一封装"读取当前登录用户"的入口。常规流程依赖 {@code OshAuthenticationFilter}
 * 在过滤链阶段写入的 ThreadLocal；对于标注 {@code @Anonymous} 的接口，过滤器会直接放行
 * 而不解析 Token，此时通过 {@link #getCurrentUserIdSafely()} 进行 Token 软认证回退识别。
 * </p>
 *
 * @author 九转苍翎
 */
@Component
public class UserContextUtil {

    /**
     * 静态 RedisCache 引用。
     * <p>通过 {@link #injectRedisCache(RedisCache)} 在 Spring 容器装配时注入，
     * 保留全部方法的静态调用语义，避免影响既有调用点。</p>
     */
    private static RedisCache redisCache;

    @Autowired
    public void injectRedisCache(RedisCache redisCache) {
        UserContextUtil.redisCache = redisCache;
    }

    public static OshUser getCurrentUser() {
        return ThreadLocalUtil.get(OshUserConstants.USER_INFO, OshUser.class);
    }

    public static Long getCurrentUserId() {
        return ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
    }

    public static Integer getCurrentLevel() {
        Object level = ThreadLocalUtil.getLocalMap().get(OshUserConstants.LEVEL);
        if (level == null) {
            return null;
        }
        if (level instanceof Integer) {
            return (Integer) level;
        }
        if (level instanceof Number) {
            return ((Number) level).intValue();
        }
        String levelText = String.valueOf(level);
        if (StrUtil.isBlank(levelText)) {
            return null;
        }
        return Integer.parseInt(levelText);
    }

    /**
     * 安全地获取当前用户 ID。
     * <p>
     * 两级回退：
     * <ol>
     *   <li>优先读取 ThreadLocal（认证过滤器命中时已写入）</li>
     *   <li>若 ThreadLocal 为空（如 {@code @Anonymous} 路径），则从请求头解析 Token
     *       并校验 Redis 登录态，作为软认证回退</li>
     * </ol>
     * 任一环节异常或缺失均静默返回 {@code null}，保证匿名访问不受影响。
     * </p>
     *
     * @return 当前用户 ID，未登录或上下文不可用时返回 {@code null}
     */
    public static Long getCurrentUserIdSafely() {
        Long userId = readFromThreadLocal();
        return userId != null ? userId : softAuthenticate();
    }

    /**
     * 安全地获取当前用户等级，未登录或上下文不可用时返回 {@code 0}。
     */
    public static Integer getCurrentLevelSafely() {
        try {
            return getCurrentLevel();
        } catch (Exception ignore) {
            return 0;
        }
    }

    private static Long readFromThreadLocal() {
        try {
            return getCurrentUserId();
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 基于请求头 Token 的软认证。
     * <p>
     * 与 {@code OshAuthenticationFilter} 的"硬认证"行为对齐：解析 JWT 拿到 userId 后，
     * 还需校验 Redis 登录态依然有效，才视为有效用户。任何异常一律视为未登录。
     * </p>
     */
    private static Long softAuthenticate() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String token = request.getHeader(OshUserConstants.TOKEN);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        Long userId;
        try {
            userId = JwtUtil.getUserIdByToken(token);
        } catch (Exception ignore) {
            return null;
        }
        if (userId == null || redisCache == null) {
            return null;
        }
        Object cachedLogin = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        return cachedLogin == null ? null : userId;
    }

    /**
     * 获取当前请求对象，非 Web 上下文（如定时任务、消息消费）下返回 {@code null}。
     */
    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = ServletUtils.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
