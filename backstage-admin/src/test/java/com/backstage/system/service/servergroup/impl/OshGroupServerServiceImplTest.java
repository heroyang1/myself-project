package com.backstage.system.service.servergroup.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.component.CommentForbiddenWordFilter;
import com.backstage.system.domain.dto.GroupCreateDTO;
import com.backstage.system.domain.servergroup.OshGroupActivity;
import com.backstage.system.domain.vo.GroupActivityListVO;
import com.backstage.system.domain.vo.GroupDetailVO;
import com.backstage.system.domain.vo.UserSearchVO;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.mapper.servergroup.OshGroupUserInitiatedMapper;
import com.backstage.system.service.servergroup.event.GroupEventPublisher;
import com.backstage.system.utils.OssUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 拼团模块优化单元测试 (JUnit 5)
 *
 * 覆盖范围：
 * - P1: 缓存雪崩随机TTL、空值哨兵防穿透、详情分层缓存
 * - P2: 活动列表多维度筛选、用户发起列表缓存
 * - P3: 用户搜索缓存
 * - 缓存清除：延迟双删
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OshGroupServerServiceImplTest {

    @InjectMocks
    private OshGroupServerServiceImpl groupServerService;

    @Mock
    private OshGroupServerMapper groupServerMapper;

    @Mock
    private OshGroupUserInitiatedMapper userInitiatedMapper;

    @Mock
    private OshOrderMapper oshOrderMapper;

    @Mock
    private OshPaymentMapper oshPaymentMapper;

    @Mock
    private OssUtil ossUtil;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @Mock
    private CommentForbiddenWordFilter forbiddenWordFilter;

    @Mock
    private GroupEventPublisher groupEventPublisher;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== P1-1: 缓存雪崩随机TTL + 空值哨兵 ====================

    @Nested
    @DisplayName("活动列表缓存")
    class ActivityListCacheTests {

        @Test
        @DisplayName("缓存未命中时查询DB并写入缓存(随机TTL)")
        void cacheMiss_shouldQueryDbAndCacheResult() {
            when(valueOperations.get(anyString())).thenReturn(null);
            List<GroupActivityListVO> mockList = buildMockActivityList(1);
            when(groupServerMapper.selectGroupActivityList(
                    eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(mockList);

            List<GroupActivityListVO> result = groupServerService.selectGroupActivityList(1);

            assertNotNull(result);
            assertEquals(1, result.size());
            // 验证写入缓存时使用了随机TTL（基础TTL 2*60=120秒 + 30~120秒抖动）
            verify(valueOperations).set(
                    anyString(),
                    anyList(),
                    longThat(ttl -> ttl >= 120 && ttl <= 240),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存命中时直接返回缓存数据")
        void cacheHit_shouldReturnCachedList() {
            List<GroupActivityListVO> cachedList = buildMockActivityList(1);
            when(valueOperations.get(contains("group:activity:list:"))).thenReturn(cachedList);

            List<GroupActivityListVO> result = groupServerService.selectGroupActivityList(1);

            assertEquals(cachedList, result);
            verify(groupServerMapper, never()).selectGroupActivityList(
                    anyInt(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("命中NULL_LIST哨兵时返回空列表")
        void nullListSentinel_shouldReturnEmptyList() {
            when(valueOperations.get(anyString())).thenReturn("NULL_LIST");

            List<GroupActivityListVO> result = groupServerService.selectGroupActivityList(1);

            assertTrue(result.isEmpty());
            verify(groupServerMapper, never()).selectGroupActivityList(
                    anyInt(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("DB返回空列表时缓存NULL_LIST哨兵(30秒)")
        void emptyDbResult_shouldCacheNullSentinel() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(groupServerMapper.selectGroupActivityList(
                    anyInt(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            List<GroupActivityListVO> result = groupServerService.selectGroupActivityList(1);

            assertTrue(result.isEmpty());
            verify(valueOperations).set(
                    anyString(),
                    eq("NULL_LIST"),
                    eq(30L),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Redis异常时降级查询DB")
        void redisException_shouldFallbackToDb() {
            when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis连接失败"));
            List<GroupActivityListVO> mockList = buildMockActivityList(1);
            when(groupServerMapper.selectGroupActivityList(
                    eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(mockList);

            List<GroupActivityListVO> result = groupServerService.selectGroupActivityList(1);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ==================== P2-1: 多维度筛选 ====================

    @Nested
    @DisplayName("多维度筛选")
    class MultiDimFilterTests {

        @Test
        @DisplayName("不同筛选条件生成不同缓存Key")
        void shouldBuildDifferentCacheKeys() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(groupServerMapper.selectGroupActivityList(
                    anyInt(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(buildMockActivityList(1));

            groupServerService.selectGroupActivityList(1, "服务器", null, null, null, null, null);
            groupServerService.selectGroupActivityList(1, null, new BigDecimal("10"), new BigDecimal("50"), null, null, null);
            groupServerService.selectGroupActivityList(1, null, null, null, "2核", "4G", null);

            verify(valueOperations, times(3)).get(argThat(key -> {
                String k = key.toString();
                return k.startsWith("group:activity:list:");
            }));
        }

        @Test
        @DisplayName("筛选参数完整透传到Mapper")
        void passesParamsToMapper() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(groupServerMapper.selectGroupActivityList(
                    anyInt(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            BigDecimal minPrice = new BigDecimal("10");
            BigDecimal maxPrice = new BigDecimal("50");

            groupServerService.selectGroupActivityList(1, "服务器", minPrice, maxPrice, "2核", "4G", "50G");

            verify(groupServerMapper).selectGroupActivityList(
                    eq(1), eq("服务器"), eq(minPrice), eq(maxPrice), eq("2核"), eq("4G"), eq("50G"));
        }

        @Test
        @DisplayName("简单重载方法委托给多维度方法(其余参数为null)")
        void simpleOverload_delegatesToMultiDim() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(groupServerMapper.selectGroupActivityList(
                    eq(2), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(buildMockActivityList(2));

            List<GroupActivityListVO> result = groupServerService.selectGroupActivityList(2);

            verify(groupServerMapper).selectGroupActivityList(
                    eq(2), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
        }
    }

    // ==================== P1-2: 详情缓存 ====================

    @Nested
    @DisplayName("详情缓存")
    class DetailCacheTests {

        @Test
        @DisplayName("缓存未命中时查询DB并写入详情缓存")
        void cacheMiss_shouldQueryDbAndCache() {
            when(valueOperations.get(anyString())).thenReturn(null);
            OshGroupActivity activity = buildMockActivity();
            when(groupServerMapper.selectGroupActivityById(100L)).thenReturn(activity);
            when(groupServerMapper.checkUserJoined(anyLong(), anyLong())).thenReturn(0);

            GroupDetailVO result = groupServerService.selectGroupDetail(100L, 1L);

            assertNotNull(result);
            assertEquals(Long.valueOf(100L), result.getId());
            verify(valueOperations).set(
                    contains("group:activity:detail:sys:"),
                    any(GroupDetailVO.class),
                    longThat(ttl -> ttl >= 300 && ttl <= 420),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存命中时直接返回缓存VO")
        void cacheHit_shouldReturnCachedVo() {
            GroupDetailVO cachedVo = new GroupDetailVO();
            cachedVo.setId(100L);
            cachedVo.setTitle("缓存的拼团");
            when(valueOperations.get(contains("sys:100"))).thenReturn(cachedVo);
            when(groupServerMapper.checkUserJoined(anyLong(), anyLong())).thenReturn(0);

            GroupDetailVO result = groupServerService.selectGroupDetail(100L, 1L);

            assertEquals(Long.valueOf(100L), result.getId());
            verify(groupServerMapper, never()).selectGroupActivityById(anyLong());
        }

        @Test
        @DisplayName("命中NULL_DETAIL哨兵时抛出ServiceException")
        void nullDetailSentinel_shouldThrowException() {
            when(valueOperations.get(contains("sys:999"))).thenReturn("NULL_DETAIL");

            assertThrows(ServiceException.class,
                    () -> groupServerService.selectGroupDetail(999L, 1L));
        }

        @Test
        @DisplayName("活动不存在时缓存NULL_DETAIL哨兵并抛出异常")
        void activityNotFound_shouldCacheNullSentinelAndThrow() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(groupServerMapper.selectGroupActivityById(anyLong())).thenReturn(null);

            assertThrows(ServiceException.class,
                    () -> groupServerService.selectGroupDetail(999L, 1L));

            // 验证：缓存了空值哨兵
            verify(valueOperations).set(
                    contains("sys:999"),
                    eq("NULL_DETAIL"),
                    eq(30L),
                    eq(TimeUnit.SECONDS));
        }
    }

    // ==================== P3-1: 用户搜索缓存 ====================

    @Nested
    @DisplayName("用户搜索缓存")
    class UserSearchCacheTests {

        @Test
        @DisplayName("缓存未命中时查询DB并写入缓存(30秒)")
        void cacheMiss_shouldQueryDbAndCache() {
            when(valueOperations.get(anyString())).thenReturn(null);
            List<UserSearchVO> mockUsers = buildMockUserSearchList();
            when(groupServerMapper.selectUsernamesByKeyword(eq("test"), eq(20))).thenReturn(mockUsers);
            when(ossUtil.getFullFilePath(anyString())).thenAnswer(inv -> "https://cdn.example.com/" + inv.getArgument(0));

            List<UserSearchVO> result = groupServerService.searchUsernames("test", 20);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(valueOperations).set(
                    contains("group:user:search:test:20"),
                    anyList(),
                    eq(30L),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("缓存命中时直接返回缓存数据")
        void cacheHit_shouldReturnCachedList() {
            List<UserSearchVO> cachedUsers = buildMockUserSearchList();
            when(valueOperations.get(contains("group:user:search:test:20"))).thenReturn(cachedUsers);

            List<UserSearchVO> result = groupServerService.searchUsernames("test", 20);

            assertEquals(cachedUsers, result);
            verify(groupServerMapper, never()).selectUsernamesByKeyword(anyString(), anyInt());
        }

        @Test
        @DisplayName("空关键词时直接返回空列表(不查缓存不写缓存)")
        void emptyKeyword_shouldReturnEmptyWithoutCaching() {
            List<UserSearchVO> result = groupServerService.searchUsernames("", 20);

            assertTrue(result.isEmpty());
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("null关键词时直接返回空列表")
        void nullKeyword_shouldReturnEmptyWithoutCaching() {
            List<UserSearchVO> result = groupServerService.searchUsernames(null, null);

            assertTrue(result.isEmpty());
            verify(valueOperations, never()).get(anyString());
        }
    }

    // ==================== 缓存清除（延迟双删） ====================

    @Nested
    @DisplayName("延迟双删缓存清除")
    class CacheEvictionTests {

        @Test
        @DisplayName("立即删除所有10个相关缓存Key")
        void shouldDeleteAllRelatedCacheKeys() throws Exception {
            Method evictMethod = OshGroupServerServiceImpl.class.getDeclaredMethod("evictActivityCache", Long.class);
            evictMethod.setAccessible(true);

            evictMethod.invoke(groupServerService, 100L);

            // 列表缓存（4个状态key）
            verify(redisTemplate).delete("group:activity:list:all");
            verify(redisTemplate).delete("group:activity:list:1");
            verify(redisTemplate).delete("group:activity:list:2");
            verify(redisTemplate).delete("group:activity:list:3");
            // 详情缓存（sys + init 两种前缀）
            verify(redisTemplate).delete("group:activity:detail:sys:100");
            verify(redisTemplate).delete("group:activity:detail:init:100");
            // 用户发起列表缓存（4个状态key）
            verify(redisTemplate).delete("group:initiated:list:all");
            verify(redisTemplate).delete("group:initiated:list:0");
            verify(redisTemplate).delete("group:initiated:list:1");
            verify(redisTemplate).delete("group:initiated:list:2");

            // 总共 4 + 2 + 4 = 10 个 key（第一次）
            verify(redisTemplate, times(10)).delete(anyString());
        }

        @Test
        @DisplayName("500ms后执行第二次延迟删除(共20次delete)")
        void delayedDoubleDelete_shouldScheduleSecondDelete() throws Exception {
            Method evictMethod = OshGroupServerServiceImpl.class.getDeclaredMethod("evictActivityCache", Long.class);
            evictMethod.setAccessible(true);

            evictMethod.invoke(groupServerService, 200L);

            // 等待延迟双删执行（500ms + 缓冲）
            Thread.sleep(800);

            // 第二次延迟删除也执行了（总共 10 * 2 = 20 次）
            verify(redisTemplate, times(20)).delete(anyString());
        }
    }

    // ==================== 随机TTL验证 ====================

    @Nested
    @DisplayName("随机TTL")
    class RandomTtlTests {

        @Test
        @DisplayName("基础TTL=2分钟时结果在150~240秒范围内")
        void shouldReturnJitteredValue() throws Exception {
            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod("randomizeTtlSeconds", long.class);
            method.setAccessible(true);

            for (int i = 0; i < 20; i++) {
                long result = (long) method.invoke(groupServerService, 2L);
                assertTrue(result >= 150, "TTL应 >= 150, 实际=" + result);
                assertTrue(result <= 240, "TTL应 <= 240, 实际=" + result);
            }
        }

        @Test
        @DisplayName("基础TTL=5分钟时结果在330~420秒范围内")
        void differentBaseTtl_shouldScaleCorrectly() throws Exception {
            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod("randomizeTtlSeconds", long.class);
            method.setAccessible(true);

            for (int i = 0; i < 10; i++) {
                long result = (long) method.invoke(groupServerService, 5L);
                assertTrue(result >= 330, "TTL应 >= 330, 实际=" + result);
                assertTrue(result <= 420, "TTL应 <= 420, 实际=" + result);
            }
        }
    }

    // ==================== 缓存Key构建验证 ====================

    @Nested
    @DisplayName("缓存Key构建")
    class CacheKeyBuildTests {

        @Test
        @DisplayName("所有参数为null时构建最小Key")
        void allNullParams_shouldBuildMinimalKey() throws Exception {
            Method method = getBuildCacheKeyMethod();

            String key = (String) method.invoke(groupServerService,
                    1, null, null, null, null, null, null);

            assertEquals("group:activity:list:s=1", key);
        }

        @Test
        @DisplayName("所有参数非null时构建完整Key")
        void allParams_shouldBuildFullKey() throws Exception {
            Method method = getBuildCacheKeyMethod();

            String key = (String) method.invoke(groupServerService,
                    2, "服务器", new BigDecimal("10"), new BigDecimal("50"),
                    "2核", "4G", "50G");

            assertTrue(key.contains("s=2"));
            assertTrue(key.contains("kw=服务器"));
            assertTrue(key.contains("min=10"));
            assertTrue(key.contains("max=50"));
            assertTrue(key.contains("cpu=2核"));
            assertTrue(key.contains("mem=4G"));
            assertTrue(key.contains("sto=50G"));
        }

        @Test
        @DisplayName("status为null时使用'all'")
        void nullStatus_shouldUseAll() throws Exception {
            Method method = getBuildCacheKeyMethod();

            String key = (String) method.invoke(groupServerService,
                    null, null, null, null, null, null, null);

            assertEquals("group:activity:list:s=all", key);
        }

        @Test
        @DisplayName("不同参数生成不同缓存Key")
        void differentParams_shouldProduceDifferentKeys() throws Exception {
            Method method = getBuildCacheKeyMethod();

            String key1 = (String) method.invoke(groupServerService,
                    1, "服务器A", null, null, null, null, null);
            String key2 = (String) method.invoke(groupServerService,
                    1, "服务器B", null, null, null, null, null);

            assertNotEquals(key1, key2, "不同关键词应生成不同缓存Key");
        }

        private Method getBuildCacheKeyMethod() throws NoSuchMethodException {
            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "buildActivityListCacheKey", Integer.class, String.class,
                    BigDecimal.class, BigDecimal.class, String.class, String.class, String.class);
            method.setAccessible(true);
            return method;
        }
    }

    // ==================== P0-1: 敏感词校验 ====================

    @Nested
    @DisplayName("敏感词校验（Sensitive-Word）")
    class SensitiveWordTests {

        @Test
        @DisplayName("标题命中违禁词时抛出 ServiceException")
        void titleWithForbiddenWord_shouldThrowException() throws Exception {
            when(forbiddenWordFilter.matchForbiddenWord("违规标题")).thenReturn("违规");

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateSensitiveContent", String.class, String.class);
            method.setAccessible(true);

            try {
                method.invoke(groupServerService, "违规标题", null);
                fail("应抛出异常");
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertTrue(e.getCause() instanceof ServiceException);
                assertTrue(e.getCause().getMessage().contains("违禁词"));
            }
        }

        @Test
        @DisplayName("备注命中违禁词时抛出 ServiceException")
        void remarkWithForbiddenWord_shouldThrowException() throws Exception {
            when(forbiddenWordFilter.matchForbiddenWord(null)).thenReturn(null);
            when(forbiddenWordFilter.matchForbiddenWord("敏感备注内容")).thenReturn("敏感");

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateSensitiveContent", String.class, String.class);
            method.setAccessible(true);

            try {
                method.invoke(groupServerService, null, "敏感备注内容");
                fail("应抛出异常");
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertTrue(e.getCause() instanceof ServiceException);
                assertTrue(e.getCause().getMessage().contains("备注"));
            }
        }

        @Test
        @DisplayName("标题和备注均无违禁词时正常通过")
        void noForbiddenWord_shouldPassSilently() throws Exception {
            when(forbiddenWordFilter.matchForbiddenWord(anyString())).thenReturn(null);
            when(forbiddenWordFilter.matchForbiddenWord(null)).thenReturn(null);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateSensitiveContent", String.class, String.class);
            method.setAccessible(true);

            // 不应抛出异常
            assertDoesNotThrow(() -> method.invoke(groupServerService, "正常标题", "正常备注"));
        }

        @Test
        @DisplayName("备注为空时跳过备注校验")
        void emptyRemark_shouldSkipRemarkCheck() throws Exception {
            when(forbiddenWordFilter.matchForbiddenWord(null)).thenReturn(null);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateSensitiveContent", String.class, String.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> method.invoke(groupServerService, null, null));
            assertDoesNotThrow(() -> method.invoke(groupServerService, null, ""));

            // 备注为空时 matchForbiddenWord 只调用了一次（标题），备注被跳过
            verify(forbiddenWordFilter, times(2)).matchForbiddenWord(null);
            verify(forbiddenWordFilter, never()).matchForbiddenWord("");
        }
    }

    // ==================== P0-3: Commons Lang3 Validate API ====================

    @Nested
    @DisplayName("参数校验（Validate API）")
    class ValidateParamTests {

        @Test
        @DisplayName("minNum 为 null 时抛出 NullPointerException")
        void nullMinNum_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setMinNum(null);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("minNum 小于 2 时抛出 IllegalArgumentException")
        void minNumLessThan2_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setMinNum(1);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("maxNum 小于 minNum 时抛出 IllegalArgumentException")
        void maxNumLessThanMinNum_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setMinNum(5);
            dto.setMaxNum(3);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("price 为 0 时抛出 IllegalArgumentException")
        void zeroPrice_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setPrice(BigDecimal.ZERO);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("price 为负数时抛出 IllegalArgumentException")
        void negativePrice_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setPrice(new BigDecimal("-10"));

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("duration 超过 120 个月时抛出 IllegalArgumentException")
        void durationExceeds120_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setDuration(121);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("duration 为 0 时抛出 IllegalArgumentException")
        void zeroDuration_shouldThrow() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setDuration(0);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("合法参数不抛出异常")
        void validParams_shouldPass() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("边界值: minNum=2, maxNum=2, price=0.01, duration=1 均合法")
        void boundaryValues_shouldPass() throws Exception {
            GroupCreateDTO dto = new GroupCreateDTO();
            dto.setActivityId(1L);
            dto.setMinNum(2);
            dto.setMaxNum(2);
            dto.setPrice(new BigDecimal("0.01"));
            dto.setDuration(1);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> method.invoke(groupServerService, dto));
        }

        @Test
        @DisplayName("边界值: duration=120 合法")
        void duration120_shouldPass() throws Exception {
            GroupCreateDTO dto = buildValidCreateDTO();
            dto.setDuration(120);

            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "validateCreateParams", GroupCreateDTO.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> method.invoke(groupServerService, dto));
        }
    }

    // ==================== P1-2: 事务一致性 (publishAfterCommit) ====================

    @Nested
    @DisplayName("事务一致性（publishAfterCommit）")
    class TransactionConsistencyTests {

        @Test
        @DisplayName("无活跃事务时立即执行 action")
        void noActiveTransaction_shouldRunImmediately() throws Exception {
            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "publishAfterCommit", Runnable.class);
            method.setAccessible(true);

            Runnable mockAction = mock(Runnable.class);
            method.invoke(groupServerService, mockAction);

            verify(mockAction, times(1)).run();
        }

        @Test
        @DisplayName("action 异常时不影响后续调用")
        void actionThrowsException_shouldNotAffectSubsequentCalls() throws Exception {
            Method method = OshGroupServerServiceImpl.class.getDeclaredMethod(
                    "publishAfterCommit", Runnable.class);
            method.setAccessible(true);

            Runnable failingAction = mock(Runnable.class);
            doThrow(new RuntimeException("Kafka down")).when(failingAction).run();

            assertThrows(Exception.class, () -> method.invoke(groupServerService, failingAction));

            // 再次调用正常 action 应能正常执行
            Runnable normalAction = mock(Runnable.class);
            method.invoke(groupServerService, normalAction);
            verify(normalAction, times(1)).run();
        }
    }

    // ==================== 辅助方法 ====================

    private List<GroupActivityListVO> buildMockActivityList(int count) {
        List<GroupActivityListVO> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            GroupActivityListVO vo = new GroupActivityListVO();
            vo.setId((long) i);
            vo.setTitle("测试拼团" + i);
            vo.setCpu("2核");
            vo.setMemory("4G");
            vo.setStorage("50G");
            vo.setBasePrice(new BigDecimal("29.90"));
            vo.setTotalDuration(12);
            vo.setCurrentNum(1);
            vo.setGroupMinNum(3);
            vo.setGroupMaxNum(10);
            vo.setStatus(1);
            vo.setCover(null);
            list.add(vo);
        }
        return list;
    }

    private OshGroupActivity buildMockActivity() {
        OshGroupActivity activity = new OshGroupActivity();
        activity.setId(100L);
        activity.setTitle("测试拼团活动");
        activity.setCpu("2核");
        activity.setMemory("4G");
        activity.setStorage("50G");
        activity.setBasePrice(new BigDecimal("29.90"));
        activity.setTotalDuration(12);
        activity.setCurrentNum(2);
        activity.setGroupMinNum(3);
        activity.setGroupMaxNum(10);
        activity.setStatus(1);
        activity.setType("server");
        return activity;
    }

    private GroupCreateDTO buildValidCreateDTO() {
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setActivityId(100L);
        dto.setMinNum(3);
        dto.setMaxNum(10);
        dto.setPrice(new BigDecimal("29.90"));
        dto.setDuration(12);
        return dto;
    }

    private List<UserSearchVO> buildMockUserSearchList() {
        List<UserSearchVO> users = new ArrayList<>();

        UserSearchVO user1 = new UserSearchVO();
        user1.setUserId(1L);
        user1.setUsername("testuser1");
        user1.setNickname("测试用户1");
        user1.setAvatar("avatars/user1.png");
        users.add(user1);

        UserSearchVO user2 = new UserSearchVO();
        user2.setUserId(2L);
        user2.setUsername("testuser2");
        user2.setNickname("测试用户2");
        user2.setAvatar("avatars/user2.png");
        users.add(user2);

        return users;
    }
}
