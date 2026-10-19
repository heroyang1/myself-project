package com.backstage.framework.security;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.utils.jwt.JwtUtil;
import com.backstage.framework.config.properties.PermitAllUrlProperties;
import com.backstage.framework.security.filter.OshAuthenticationFilter;
import com.backstage.system.domain.user.OshPermission;
import com.backstage.system.domain.user.OshRole;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.user.OshPermissionMapper;
import com.backstage.system.mapper.user.OshRoleMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P0-C：双权限体系边界安全测试。
 * <p>
 * 基于等价类划分 + 边界值分析，覆盖项目特有的双权限体系（B 端 RuoYi + C 端自定义 Filter）：
 * <ul>
 *   <li>等价类 1：C 端登录用户（有效 Token + Redis 会话）→ 200</li>
 *   <li>等价类 2：无 Token 访问 /pc 接口 → 401</li>
 *   <li>等价类 3：Token 过期/被踢出 → 401</li>
 *   <li>边界值：角色过期瞬间 → 降级为默认角色</li>
 *   <li>边界值：多角色用户 → 取全部角色权限合集</li>
 *   <li>场景法：会话滑动续期 → 持续操作不被踢出</li>
 * </ul>
 * <p>
 * 历史 Bug 关联：81dd573a（/pc 接口误用 B 端注解 403）、
 * e688778b（公开接口落入 /pc 路径 401）、3eb4e258（会话不续期被踢出）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityBoundaryTest {

    private static final Long USER_ID = 3001L;
    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz1234567890";

    @InjectMocks
    private TestableFilter filter;

    @Mock private RedisCache redisCache;
    @Mock private OshUserMapper oshUserMapper;
    @Mock private OshRoleMapper oshRoleMapper;
    @Mock private OshPermissionMapper oshPermissionMapper;
    @Mock private PermitAllUrlProperties permitAllUrl;

    @BeforeEach
    public void setUp() {
        new JwtUtil().setSecret(SECRET);
        when(permitAllUrl.getUrls()).thenReturn(Collections.emptyList());
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 等价类 1：有效 Token + Redis 会话 → 200 ====================

    /**
     * 正常 C 端用户请求 → 认证成功 → 200 + Security 上下文已设置
     */
    @Test
    public void shouldAuthenticateValidTokenAndSetSecurityContext() throws Exception {
        String token = createToken();
        mockValidSession(token);
        mockRolesAndPermissions(
                Collections.singletonList(role(1, "普通用户", "user", 1)),
                Collections.singletonList(permission(10, "user:info:view", 0))
        );

        MockHttpServletResponse response = doFilter(token, "/pc/user/getinfo");

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ==================== 等价类 2：无 Token 访问 /pc → 401 ====================

    /**
     * 无 Token 访问 /pc 路径 → Filter 拦截 → 401
     */
    @Test
    public void shouldReturn401WhenNoTokenOnPcPath() throws Exception {
        MockHttpServletResponse response = doFilter(null, "/pc/user/getinfo");

        assertEquals(401, response.getStatus());
    }

    // ==================== 等价类 3：Token 不匹配（被踢出）→ 401 ====================

    /**
     * Token 与 Redis 中存储的不一致 → 设备被踢出 → 401
     * 历史 Bug：用户同账号另一设备登录，旧设备应被踢出
     */
    @Test
    public void shouldReturn401WhenTokenMismatchKicksOutPreviousDevice() throws Exception {
        String requestToken = createToken();
        Map<String, Object> cache = new HashMap<>();
        cache.put(OshUserConstants.TOKEN, "different-token-on-other-device");
        when(redisCache.getCacheObject(OshUserConstants.LOGIN_USER + USER_ID)).thenReturn(cache);

        MockHttpServletResponse response = doFilter(requestToken, "/pc/user/getinfo");

        assertEquals(401, response.getStatus());
        verify(oshRoleMapper, never()).getRoleIdsByUserId(any());
    }

    // ==================== 边界值：角色过期 → 降级 ====================

    /**
     * 用户只有一个高级角色（如 VIP），过期后 → 降级为默认普通用户（role_id=1）
     * 权限刷新从 Redis 实时查，不缓存过期角色
     */
    @Test
    public void shouldRefreshPermissionsExcludingExpiredRoles() throws Exception {
        String token = createToken();
        mockValidSession(token);

        // 只有普通用户角色（高级角色已过期不在结果中）
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(Collections.singletonList(1));
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(
                Collections.singletonList(role(1, "普通用户", "user", 1)));
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(Collections.singletonList(1)))
                .thenReturn(Collections.singletonList(10));
        when(oshPermissionMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(permission(10, "user:info:view", 0)));
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(buildUser());

        MockHttpServletResponse response = doFilter(token, "/pc/user/getinfo");

        assertEquals(200, response.getStatus());
        List<String> authorities = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString).collect(Collectors.toList());
        assertTrue("应包含普通用户权限", authorities.contains("user:info:view"));
        assertFalse("不应包含已过期VIP权限", authorities.contains("vip:course:watch"));
    }

    // ==================== 边界值：多角色用户 → 权限合集 ====================

    /**
     * 用户同时有 role_id=1（普通用户）和 role_id=3（VIP）→ 权限为两者合集
     * 历史 Bug：e663560d - 只查单角色导致权限误拦
     */
    @Test
    public void shouldMergePermissionsFromAllRoles() throws Exception {
        String token = createToken();
        mockValidSession(token);

        List<Integer> roleIds = Arrays.asList(1, 3);
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(roleIds);
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                role(1, "普通用户", "user", 1),
                role(3, "VIP用户", "vip", 3)));
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(roleIds))
                .thenReturn(Arrays.asList(10, 20));
        when(oshPermissionMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                permission(10, "user:info:view", 0),
                permission(20, "vip:course:watch", 0)));
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(buildUser());

        MockHttpServletResponse response = doFilter(token, "/pc/user/getinfo");

        assertEquals(200, response.getStatus());
        List<String> authorities = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString).collect(Collectors.toList());
        assertTrue("应包含普通用户权限", authorities.contains("user:info:view"));
        assertTrue("应包含VIP权限", authorities.contains("vip:course:watch"));
    }

    // ==================== 场景：会话滑动续期 ====================

    /**
     * 每次有效请求后 Redis TTL 重置为 500 分钟
     * 历史 Bug：3eb4e258 - 只在登录时设 TTL，持续操作超 500 分钟被踢
     */
    @Test
    public void shouldRenewSessionTTLonEveryValidRequest() throws Exception {
        String token = createToken();
        mockValidSession(token);
        mockRolesAndPermissions(
                Collections.singletonList(role(1, "普通用户", "user", 1)),
                Collections.singletonList(permission(10, "user:info:view", 0))
        );

        doFilter(token, "/pc/user/getinfo");

        // 验证 TTL 被续期（500 分钟）
        verify(redisCache).setCacheObject(
                eq(OshUserConstants.LOGIN_USER + USER_ID),
                any(),
                eq(500),
                eq(TimeUnit.MINUTES));
    }

    // ==================== 场景：Redis 中无会话 → 401 ====================

    /**
     * Token 有效但 Redis 中 LoginUser 已被删除（如管理员删除用户）→ 401
     */
    @Test
    public void shouldReturn401WhenRedisSessionDeleted() throws Exception {
        String token = createToken();
        when(redisCache.getCacheObject(OshUserConstants.LOGIN_USER + USER_ID)).thenReturn(null);

        MockHttpServletResponse response = doFilter(token, "/pc/user/getinfo");

        assertEquals(401, response.getStatus());
    }

    // ==================== 场景：跨角色切换边界（权限实时收窄） ====================

    /**
     * 用户第一次请求时持有 VIP 角色 → 权限包含 vip:course:watch。
     * 管理员实时降级（数据库角色被移除）→ 第二次请求权限应同步收窄。
     * 验证：权限不是登录时缓存的，而是每次请求实时从 DB 刷新。
     * 历史关联：角色降级后用户仍能访问 VIP 内容的安全漏洞。
     */
    @Test
    public void shouldNarrowPermissionsWhenRoleDowngradedBetweenRequests() throws Exception {
        String token = createToken();
        mockValidSession(token);

        // === 第一次请求：持有 VIP 角色（role 1 + role 3） ===
        List<Integer> vipRoleIds = Arrays.asList(1, 3);
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(vipRoleIds);
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                role(1, "普通用户", "user", 1),
                role(3, "VIP用户", "vip", 3)));
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(vipRoleIds))
                .thenReturn(Arrays.asList(10, 20));
        when(oshPermissionMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                permission(10, "user:info:view", 0),
                permission(20, "vip:course:watch", 0)));
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(buildUser());

        MockHttpServletResponse response1 = doFilter(token, "/pc/vip/course");
        assertEquals(200, response1.getStatus());
        List<String> authorities1 = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString).collect(Collectors.toList());
        assertTrue("第一次请求应包含VIP权限", authorities1.contains("vip:course:watch"));

        // === 模拟管理员降级：清除 SecurityContext，DB 中 VIP 角色已移除 ===
        SecurityContextHolder.clearContext();

        // === 第二次请求：仅剩普通角色（role 1） ===
        List<Integer> basicRoleIds = Collections.singletonList(1);
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(basicRoleIds);
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(
                Collections.singletonList(role(1, "普通用户", "user", 1)));
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(basicRoleIds))
                .thenReturn(Collections.singletonList(10));
        when(oshPermissionMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(permission(10, "user:info:view", 0)));

        MockHttpServletResponse response2 = doFilter(token, "/pc/vip/course");
        assertEquals(200, response2.getStatus());
        List<String> authorities2 = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString).collect(Collectors.toList());
        assertTrue("降级后仍应有基础权限", authorities2.contains("user:info:view"));
        assertFalse("降级后 VIP 权限必须消失", authorities2.contains("vip:course:watch"));
    }

    /**
     * 用户角色被完全移除（如封号）→ 第二次请求应仅有空权限集或默认权限。
     * 验证：不会因为缓存残留而保持旧权限。
     */
    @Test
    public void shouldReturnEmptyPermissionsWhenAllRolesRemoved() throws Exception {
        String token = createToken();
        mockValidSession(token);

        // 第一次有角色
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(Collections.singletonList(1));
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(
                Collections.singletonList(role(1, "普通用户", "user", 1)));
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(Collections.singletonList(1)))
                .thenReturn(Collections.singletonList(10));
        when(oshPermissionMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(permission(10, "user:info:view", 0)));
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(buildUser());

        doFilter(token, "/pc/user/getinfo");
        assertNotNull("第一次请求应有认证信息", SecurityContextHolder.getContext().getAuthentication());

        // 模拟封号：角色全部移除
        SecurityContextHolder.clearContext();
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(Collections.emptyList()))
                .thenReturn(Collections.emptyList());
        when(oshPermissionMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        MockHttpServletResponse response = doFilter(token, "/pc/user/getinfo");
        assertEquals(200, response.getStatus());
        List<String> authorities = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(Object::toString).collect(Collectors.toList());
        assertTrue("封号后权限集应为空", authorities.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private void mockValidSession(String token) {
        Map<String, Object> cache = new HashMap<>();
        cache.put(OshUserConstants.TOKEN, token);
        when(redisCache.getCacheObject(OshUserConstants.LOGIN_USER + USER_ID)).thenReturn(cache);
    }

    private void mockRolesAndPermissions(List<OshRole> roles, List<OshPermission> permissions) {
        List<Integer> roleIds = new ArrayList<>();
        for (OshRole r : roles) {
            roleIds.add(r.getId());
        }
        when(oshRoleMapper.getRoleIdsByUserId(USER_ID)).thenReturn(roleIds);
        when(oshRoleMapper.selectList(any(Wrapper.class))).thenReturn(roles);
        when(oshPermissionMapper.selectPermissionIdsByRoleIds(roleIds))
                .thenReturn(new ArrayList<>());
        for (int i = 0; i < permissions.size(); i++) {
            roleIds.add(permissions.get(i).getId());
        }
        when(oshPermissionMapper.selectList(any(Wrapper.class))).thenReturn(permissions);
        when(oshUserMapper.selectOne(any(Wrapper.class))).thenReturn(buildUser());
    }

    private MockHttpServletResponse doFilter(String token, String uri) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (token != null) {
            request.addHeader(OshUserConstants.TOKEN, token);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());
        return response;
    }

    private String createToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(OshUserConstants.USER_ID, USER_ID);
        claims.put(OshUserConstants.USERNAME, "security_tester");
        return JwtUtil.createToken(claims);
    }

    private OshUser buildUser() {
        OshUser user = new OshUser();
        user.setId(USER_ID);
        user.setUsername("security_tester");
        return user;
    }

    private OshRole role(Integer id, String name, String code, Integer level) {
        OshRole role = new OshRole();
        role.setId(id);
        role.setRoleName(name);
        role.setRoleCode(code);
        role.setLevel(level);
        return role;
    }

    private OshPermission permission(Integer id, String code, Integer parentId) {
        OshPermission p = new OshPermission();
        p.setId(id);
        p.setPermissionCode(code);
        p.setParentId(parentId);
        return p;
    }

    static class TestableFilter extends OshAuthenticationFilter {
        @Override
        public void doFilterInternal(javax.servlet.http.HttpServletRequest request,
                                     javax.servlet.http.HttpServletResponse response,
                                     javax.servlet.FilterChain chain) throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
        }
    }
}
