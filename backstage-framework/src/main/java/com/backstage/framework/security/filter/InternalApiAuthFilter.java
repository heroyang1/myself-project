package com.backstage.framework.security.filter;

import com.backstage.contract.common.InternalApiConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 内部服务接口鉴权过滤器（fail-closed）
 *
 * <p>功能描述：拦截 {@code /internal/**} 服务间内部接口，校验共享密钥请求头
 * {@link InternalApiConstants#HEADER_INTERNAL_TOKEN}，通过则向 SecurityContext 注入
 * {@link InternalApiConstants#AUTHORITY_INTERNAL_SERVICE} 权限，交由 SecurityConfig 的
 * {@code hasAuthority} 规则放行；不通过则返回 403。</p>
 *
 * <p>业务流程：① 非 /internal/** 请求直接放行，不做任何处理；② /internal/** 请求读取配置密钥，
 * 密钥未配置（空）一律拒绝（fail-closed，杜绝未配置即放开的隐患）；③ 常量时间比对请求头，
 * 匹配则注入内部服务权限并放行，请求结束后清理 SecurityContext；④ 不匹配返回 403。</p>
 *
 * <p>语法逻辑：本过滤器位于 {@link OshAuthenticationFilter} 之前，内部请求不携带用户 JWT，
 * 由 OshAuthenticationFilter 的白名单放行（见其 isWhiteList），最终授权由本过滤器注入的
 * 权限 + SecurityConfig 规则完成。密钥通过 {@code osh.internal.api-token} 由配置中心下发。</p>
 *
 * <p>实现效果：在 tool-service 等调用方与密钥配置就绪前，/internal/** 处于默认拒绝的休眠态，
 * 不影响任何现有对外路径。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    /**
     * 内部接口路径匹配模式
     */
    private static final String INTERNAL_PATH_PATTERN = InternalApiConstants.BASE_PATH + "/**";

    /**
     * 服务间共享密钥（默认空 = fail-closed，未配置时拒绝所有内部调用）
     */
    @Value("${osh.internal.api-token:}")
    private String internalApiToken;

    /**
     * 内部接口路径匹配器
     */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 【非内部请求放行】仅拦截 /internal/**，其余请求原样进入后续过滤器链
        if (!pathMatcher.match(INTERNAL_PATH_PATTERN, request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // 【密钥缺失拒绝】未配置共享密钥时一律拒绝（fail-closed），避免默认放开
        String requestToken = request.getHeader(InternalApiConstants.HEADER_INTERNAL_TOKEN);
        if (internalApiToken == null || internalApiToken.isEmpty()
                || requestToken == null || !constantTimeEquals(internalApiToken, requestToken)) {
            sendForbidden(response);
            return;
        }

        // 【注入内部服务权限】校验通过，授予 INTERNAL_SERVICE 权限供 hasAuthority 放行
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-service", null,
                Collections.singletonList(new SimpleGrantedAuthority(InternalApiConstants.AUTHORITY_INTERNAL_SERVICE)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            // 【清理上下文】无状态请求结束即清理，避免线程复用导致的身份残留
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 常量时间字符串比对，避免因提前返回而泄露密钥长度/前缀信息
     *
     * @param expected 期望密钥（配置下发）
     * @param actual   请求携带密钥
     * @return 完全一致返回 true
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.length() != actual.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }
        return result == 0;
    }

    /**
     * 返回 403 内部接口鉴权失败响应
     *
     * @param response HTTP 响应
     * @throws IOException 写出响应体异常
     */
    private void sendForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"内部接口鉴权失败\"}");
    }
}
