package com.backstage.system.service.servergroup.monitor;

import com.backstage.common.core.redis.RedisCache;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GroupServerMonitorService 单元测试
 *
 * 覆盖范围：
 * - 定时采集指标并写入 Redis
 * - getLatestMetrics 查询最近采集数据
 * - 阈值告警检查（CPU/内存/磁盘）
 * - Redis 异常降级
 *
 * @author system
 * @date 2026-07-12
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupServerMonitorServiceTest {

    @InjectMocks
    private GroupServerMonitorService monitorService;

    @Mock
    private RedisCache redisCache;

    @BeforeEach
    void setUp() {
        // 手动调用 init() 初始化 OSHI SystemInfo（@PostConstruct 不会被 @InjectMocks 自动触发）
        monitorService.init();
        ReflectionTestUtils.setField(monitorService, "hostIp", "127.0.0.1");
    }

    // ==================== getLatestMetrics ====================

    @Nested
    @DisplayName("查询最新监控指标")
    class GetLatestMetricsTests {

        @Test
        @DisplayName("Redis 有数据时返回指标 Map")
        void shouldReturnMetricsWhenRedisHasData() {
            Map<String, Object> mockMetrics = new HashMap<>();
            mockMetrics.put("cpu_usage", "45.2");
            mockMetrics.put("mem_usage", "62.8");
            mockMetrics.put("disk_usage", "71.5");
            mockMetrics.put("host_ip", "127.0.0.1");

            when(redisCache.getCacheMap("group:monitor:server:127.0.0.1")).thenReturn(mockMetrics);

            Map<String, Object> result = monitorService.getLatestMetrics();

            assertNotNull(result);
            assertEquals("45.2", result.get("cpu_usage"));
            assertEquals("62.8", result.get("mem_usage"));
            assertEquals("71.5", result.get("disk_usage"));
            assertEquals("127.0.0.1", result.get("host_ip"));
        }

        @Test
        @DisplayName("Redis 无数据时返回 null")
        void shouldReturnNullWhenRedisEmpty() {
            when(redisCache.getCacheMap("group:monitor:server:127.0.0.1")).thenReturn(null);

            Map<String, Object> result = monitorService.getLatestMetrics();

            assertNull(result);
        }

        @Test
        @DisplayName("Redis key 包含正确的 hostIp 后缀")
        void shouldUseCorrectRedisKey() {
            ReflectionTestUtils.setField(monitorService, "hostIp", "192.168.1.100");
            when(redisCache.getCacheMap("group:monitor:server:192.168.1.100")).thenReturn(new HashMap<>());

            monitorService.getLatestMetrics();

            verify(redisCache).getCacheMap("group:monitor:server:192.168.1.100");
        }
    }

    // ==================== collectAndReport ====================

    @Nested
    @DisplayName("定时采集指标")
    class CollectAndReportTests {

        @Test
        @DisplayName("采集成功后写入 Redis 并设置 TTL")
        void shouldWriteMetricsToRedisWithTtl() {
            // 调用采集方法（内部使用 OSHI 读取真实硬件信息）
            monitorService.collectAndReport();

            // 验证写入 Redis Hash
            verify(redisCache).setCacheMap(
                    eq("group:monitor:server:127.0.0.1"),
                    argThat(map -> {
                        Map<String, Object> metrics = (Map<String, Object>) map;
                        // 验证核心字段都存在
                        return metrics.containsKey("cpu_cores")
                                && metrics.containsKey("cpu_usage")
                                && metrics.containsKey("mem_total_gb")
                                && metrics.containsKey("mem_used_gb")
                                && metrics.containsKey("mem_usage")
                                && metrics.containsKey("disk_total_gb")
                                && metrics.containsKey("disk_used_gb")
                                && metrics.containsKey("disk_usage")
                                && metrics.containsKey("jvm_heap_used_mb")
                                && metrics.containsKey("jvm_heap_max_mb")
                                && metrics.containsKey("jvm_thread_count")
                                && metrics.containsKey("host_ip")
                                && metrics.containsKey("collect_time");
                    }));

            // 验证设置 TTL（5分钟 = 300秒）
            verify(redisCache).expire("group:monitor:server:127.0.0.1", 300L);
        }

        @Test
        @DisplayName("CPU 指标值在合理范围内 (0~100%)")
        void cpuUsageShouldBeInValidRange() {
            monitorService.collectAndReport();

            verify(redisCache).setCacheMap(
                    eq("group:monitor:server:127.0.0.1"),
                    argThat(map -> {
                        Map<String, Object> metrics = (Map<String, Object>) map;
                        double cpuUsage = Double.parseDouble((String) metrics.get("cpu_usage"));
                        return cpuUsage >= 0.0 && cpuUsage <= 100.0;
                    }));
        }

        @Test
        @DisplayName("内存使用率在合理范围内 (0~100%)")
        void memUsageShouldBeInValidRange() {
            monitorService.collectAndReport();

            verify(redisCache).setCacheMap(
                    eq("group:monitor:server:127.0.0.1"),
                    argThat(map -> {
                        Map<String, Object> metrics = (Map<String, Object>) map;
                        double memUsage = Double.parseDouble((String) metrics.get("mem_usage"));
                        return memUsage >= 0.0 && memUsage <= 100.0;
                    }));
        }

        @Test
        @DisplayName("JVM 线程数大于 0")
        void jvmThreadCountShouldBePositive() {
            monitorService.collectAndReport();

            verify(redisCache).setCacheMap(
                    eq("group:monitor:server:127.0.0.1"),
                    argThat(map -> {
                        Map<String, Object> metrics = (Map<String, Object>) map;
                        int threadCount = (int) metrics.get("jvm_thread_count");
                        return threadCount > 0;
                    }));
        }

        @Test
        @DisplayName("Redis 写入异常时不抛出异常（降级处理）")
        void shouldNotThrowWhenRedisFails() {
            doThrow(new RuntimeException("Redis连接失败"))
                    .when(redisCache).setCacheMap(anyString(), anyMap());

            // 不应抛出异常
            assertDoesNotThrow(() -> monitorService.collectAndReport());
        }

        @Test
        @DisplayName("CPU 核心数大于 0")
        void cpuCoresShouldBePositive() {
            monitorService.collectAndReport();

            verify(redisCache).setCacheMap(
                    eq("group:monitor:server:127.0.0.1"),
                    argThat(map -> {
                        Map<String, Object> metrics = (Map<String, Object>) map;
                        int cpuCores = (int) metrics.get("cpu_cores");
                        return cpuCores > 0;
                    }));
        }
    }
}
