package com.backstage.framework.security.filter;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.core.domain.model.OshUserDetail;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.jwt.JwtUtil;
import com.backstage.framework.config.properties.PermitAllUrlProperties;
import com.backstage.system.domain.user.OshPermission;
import com.backstage.system.domain.user.OshRole;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.user.OshPermissionMapper;
import com.backstage.system.mapper.user.OshRoleMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OSH 前台用户认证过滤器
 * 无 Token 或 Token 无效时拒绝访问（返回 401）
 * WebSocket 握手请求放行（由 WebSocket 拦截器单独处理认证）
 */
@Component
public class OshAuthenticationFilter extends OncePerRequestFilter {
    private static final int LOGIN_EXPIRE_MINUTES = 500;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private OshUserMapper oshUserMapper;

    @Autowired
    private OshRoleMapper oshRoleMapper;

    @Autowired
    private OshPermissionMapper oshPermissionMapper;

    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean isAnonymous = isWhiteList(uri);

        // WebSocket 握手请求放行（由 WebSocket 拦截器单独处理认证）
        if (isWebSocketHandshake(request)) {
            chain.doFilter(request, response);
            return;
        }

        // 解析 Token（@Anonymous 接口也尝试解析，但失败不拒绝）
        String token = request.getHeader(OshUserConstants.TOKEN);
        Long userId = null;
        try {
            if (token != null && !token.isEmpty()) {
                userId = JwtUtil.getUserIdByToken(token);
            }
        } catch (Exception e) {
            if (!isAnonymous) {
                sendUnauthorized(response, "未登录或Token已过期");
                return;
            }
            // @Anonymous 接口 token 解析失败，静默忽略
        }

        if (userId == null) {
            if (!isAnonymous) {
                sendUnauthorized(response, "未登录或Token已过期");
                return;
            }
            // @Anonymous 接口无 token，直接放行
            chain.doFilter(request, response);
            return;
        }

        // 检查 Redis 登录态
        String loginUserKey = OshUserConstants.LOGIN_USER + userId;
        Map<String, Object> userInfoMap = redisCache.getCacheObject(loginUserKey);

        if (userInfoMap == null) {
            if (!isAnonymous) {
                sendUnauthorized(response, "登录已过期，请重新登录");
                return;
            }
            // @Anonymous 接口 Redis 登录态失效，静默放行
            chain.doFilter(request, response);
            return;
        }

        // 校验 token 是否为当前有效 token（踢掉旧设备：新登录会覆盖 token）
        String activeToken = (String) userInfoMap.get(OshUserConstants.TOKEN);
        if (activeToken != null && !activeToken.equals(token)) {
            if (!isAnonymous) {
                sendUnauthorized(response, "账号已在其他设备登录，请重新登录");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 顺延 Redis 会话 TTL
        redisCache.expire(loginUserKey, LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置 ThreadLocal 上下文
        ThreadLocalUtil.set(OshUserConstants.USER_ID, userId);
        
        // 实时查询用户有效角色（考虑角色过期），确保过期角色不再生效
        List<Integer> effectiveRoleIds = oshRoleMapper.getRoleIdsByUserId(userId);
        String effectiveLevel = "1";
        String effectiveRoleCode = "user";
        Map<String, String> effectiveRole = defaultRole();
        if (effectiveRoleIds != null && !effectiveRoleIds.isEmpty()) {
            LambdaQueryWrapper<OshRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.in(OshRole::getId, effectiveRoleIds).eq(OshRole::getDeleteFlag, 0);
            List<OshRole> roles = oshRoleMapper.selectList(roleWrapper);
            if (roles != null && !roles.isEmpty()) {
                OshRole highest = roles.get(0);
                for (OshRole r : roles) {
                    if (r.getLevel() > highest.getLevel()) highest = r;
                }
                effectiveLevel = highest.getLevel().toString();
                effectiveRoleCode = highest.getRoleCode();
                effectiveRole.put("roleName", highest.getRoleName());
                effectiveRole.put("roleCode", highest.getRoleCode());
                effectiveRole.put("level", highest.getLevel().toString());
            }
        }
        Map<String, List<String>> effectivePermission = buildPermission(effectiveRoleIds);
        userInfoMap.put(OshUserConstants.ROLE, effectiveRole);
        userInfoMap.put(OshUserConstants.PERMISSION, effectivePermission);
        userInfoMap.put(OshUserConstants.LOGINCOUNT, userInfoMap.getOrDefault(OshUserConstants.LOGINCOUNT, 1));
        userInfoMap.put(OshUserConstants.TOKEN, token);
        redisCache.setCacheObject(loginUserKey, userInfoMap, LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, effectiveLevel);
        ThreadLocalUtil.set(OshUserConstants.ROLE_CODE, effectiveRoleCode);

        LambdaQueryWrapper<OshUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshUser::getId, userId);
        OshUser oshUser = oshUserMapper.selectOne(wrapper);
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, oshUser);
        ThreadLocalUtil.set(OshUserConstants.USERNAME, oshUser.getUsername());

        // 设置 Spring Security 认证上下文
        OshUserDetail oshUserDetail = new OshUserDetail();
        oshUserDetail.setUserInfoMap(userInfoMap);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                oshUserDetail,
                null,
                oshUserDetail.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        try {
            chain.doFilter(request, response);
        } finally {
            ThreadLocalUtil.remove();
        }
    }

    /**
     * 返回 401 未认证响应
     */
    private void sendUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\"}");
    }

    /**
     * 判断是否为 WebSocket 握手请求
     */
    private boolean isWebSocketHandshake(HttpServletRequest request) {
        String upgrade = request.getHeader("Upgrade");
        return "websocket".equalsIgnoreCase(upgrade);
    }

    private boolean isWhiteList(String uri) {
        if (pathMatcher.match("/pc/pay/huifu/notify", uri)) {
            return true;
        }
        // 匹配 @Anonymous 注解收集的 URL
        for (String pattern : permitAllUrl.getUrls()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> defaultRole() {
        Map<String, String> role = new HashMap<>();
        role.put("roleName", "普通用户");
        role.put("roleCode", "user");
        role.put("level", "1");
        return role;
    }

    private Map<String, List<String>> buildPermission(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Integer> permissionIds = oshPermissionMapper.selectPermissionIdsByRoleIds(roleIds);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<OshPermission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.in(OshPermission::getId, permissionIds).eq(OshPermission::getDeleteFlag, 0);
        List<OshPermission> permissions = oshPermissionMapper.selectList(permissionWrapper);
        if (permissions == null || permissions.isEmpty()) {
            return new HashMap<>();
        }

        Map<Integer, OshPermission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(OshPermission::getId, p -> p, (left, right) -> left));
        Map<String, List<String>> result = new HashMap<>();
        for (OshPermission permission : permissions) {
            String currentCode = permission.getPermissionCode();
            if (currentCode == null || currentCode.isEmpty()) {
                continue;
            }
            Integer parentId = permission.getParentId();
            OshPermission parent = permissionMap.get(parentId);
            if (parent != null) {
                String parentCode = parent.getPermissionCode();
                if (parentCode != null && !parentCode.isEmpty()) {
                    result.computeIfAbsent(parentCode, k -> new ArrayList<>()).add(currentCode);
                }
            } else if (parentId == null || parentId == 0) {
                result.computeIfAbsent(currentCode, k -> new ArrayList<>());
            }
        }
        result.values().forEach(Collections::sort);
        return result;
    }
}
