package com.backstage.system.service.servergroup.monitor;

import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.utils.ip.IpUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 拼团服务器资源监控 Service（基于 OSHI 6.8.2）
 *
 * <p>定时采集服务器 CPU / 内存 / 磁盘 / JVM 指标，写入 Redis Hash 供管理端实时看板消费。</p>
 *
 * <pre>
 * Redis Key:  group:monitor:server:{hostIp}
 * Hash Field: cpu_cores, cpu_usage, mem_total_gb, mem_used_gb, mem_usage,
 *             disk_total_gb, disk_used_gb, disk_usage,
 *             jvm_heap_used_mb, jvm_heap_max_mb, jvm_thread_count,
 *             os_name, host_ip, collect_time
 * TTL:        5 min（防止僵尸数据）
 * </pre>
 *
 * <p>告警阈值（可配置）：</p>
 * <ul>
 *   <li>CPU 使用率 &gt; 80% → WARN</li>
 *   <li>内存使用率 &gt; 85% → WARN</li>
 *   <li>磁盘使用率 &gt; 90% → WARN</li>
 * </ul>
 *
 * @author system
 * @date 2026-07-12
 */
@Service
public class GroupServerMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(GroupServerMonitorService.class);

    /** Redis key 前缀 */
    private static final String REDIS_KEY_PREFIX = "group:monitor:server:";

    /** Redis key TTL: 5 min */
    private static final int KEY_TTL_MINUTES = 5;

    /** CPU 采样等待时间（ms），缩短到 500ms 降低采集延迟 */
    private static final int CPU_SAMPLE_WAIT_MS = 500;

    // ==================== 告警阈值 ====================

    /** CPU 使用率告警阈值（%） */
    private static final double CPU_ALERT_THRESHOLD = 80.0;

    /** 内存使用率告警阈值（%） */
    private static final double MEM_ALERT_THRESHOLD = 85.0;

    /** 磁盘使用率告警阈值（%） */
    private static final double DISK_ALERT_THRESHOLD = 90.0;

    /** GB 换算常量 */
    private static final long GB = 1024L * 1024L * 1024L;

    /** MB 换算常量 */
    private static final long MB = 1024L * 1024L;

    @Autowired
    private RedisCache redisCache;

    /** OSHI SystemInfo 单例（重量级对象，复用避免重复初始化） */
    private SystemInfo systemInfo;

    /** 主机 IP（作为 Redis key 后缀） */
    private String hostIp;

    @PostConstruct
    public void init() {
        systemInfo = new SystemInfo();
        hostIp = IpUtils.getHostIp();
        logger.info("【拼团监控】OSHI 监控服务已初始化，hostIp={}", hostIp);
    }

    /**
     * 定时采集服务器资源指标（每 60 秒执行一次）
     */
    @Scheduled(fixedDelayString = "${group.monitor.interval-ms:60000}",
            initialDelayString = "${group.monitor.initial-delay-ms:30000}")
    public void collectAndReport() {
        try {
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            OperatingSystem os = systemInfo.getOperatingSystem();

            Map<String, Object> metrics = new HashMap<>();

            // ---- CPU ----
            CentralProcessor processor = hal.getProcessor();
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            Util.sleep(CPU_SAMPLE_WAIT_MS);
            long[] ticks = processor.getSystemCpuLoadTicks();
            double cpuUsage = calcCpuUsage(prevTicks, ticks);
            metrics.put("cpu_cores", processor.getLogicalProcessorCount());
            metrics.put("cpu_usage", String.format("%.1f", cpuUsage));

            // ---- 内存 ----
            GlobalMemory memory = hal.getMemory();
            long memTotal = memory.getTotal();
            long memUsed = memTotal - memory.getAvailable();
            double memUsage = memTotal > 0 ? (double) memUsed / memTotal * 100 : 0;
            metrics.put("mem_total_gb", String.format("%.1f", (double) memTotal / GB));
            metrics.put("mem_used_gb", String.format("%.1f", (double) memUsed / GB));
            metrics.put("mem_usage", String.format("%.1f", memUsage));

            // ---- 磁盘（汇总所有挂载点）----
            FileSystem fileSystem = os.getFileSystem();
            List<OSFileStore> fsList = fileSystem.getFileStores();
            long diskTotal = 0, diskUsed = 0;
            for (OSFileStore fs : fsList) {
                long total = fs.getTotalSpace();
                long free = fs.getUsableSpace();
                diskTotal += total;
                diskUsed += (total - free);
            }
            double diskUsage = diskTotal > 0 ? (double) diskUsed / diskTotal * 100 : 0;
            metrics.put("disk_total_gb", String.format("%.1f", (double) diskTotal / GB));
            metrics.put("disk_used_gb", String.format("%.1f", (double) diskUsed / GB));
            metrics.put("disk_usage", String.format("%.1f", diskUsage));

            // ---- JVM ----
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadMXBean.getThreadCount();
            metrics.put("jvm_heap_used_mb", String.format("%.0f", (double) heapUsed / MB));
            metrics.put("jvm_heap_max_mb", String.format("%.0f", (double) heapMax / MB));
            metrics.put("jvm_thread_count", threadCount);

            // ---- 系统信息 ----
            Properties props = System.getProperties();
            metrics.put("os_name", props.getProperty("os.name"));
            metrics.put("host_ip", hostIp);
            metrics.put("collect_time", String.valueOf(System.currentTimeMillis()));

            // ---- 写入 Redis ----
            String redisKey = REDIS_KEY_PREFIX + hostIp;
            redisCache.setCacheMap(redisKey, metrics);
            redisCache.expire(redisKey, KEY_TTL_MINUTES * 60L);

            // ---- 告警检查 ----
            checkAlerts(cpuUsage, memUsage, diskUsage);

            logger.debug("【拼团监控】指标采集完成: cpu={}%, mem={}%, disk={}%",
                    metrics.get("cpu_usage"), metrics.get("mem_usage"), metrics.get("disk_usage"));

        } catch (Exception e) {
            logger.warn("【拼团监控】指标采集异常", e);
        }
    }

    /**
     * 获取当前服务器监控快照（供 Controller 查询接口调用）
     *
     * @return 最近一次采集的指标 Map，若 Redis 无数据则返回 null
     */
    public Map<String, Object> getLatestMetrics() {
        String redisKey = REDIS_KEY_PREFIX + hostIp;
        return redisCache.getCacheMap(redisKey);
    }

    // ==================== 内部方法 ====================

    /**
     * 计算 CPU 使用率（%）
     */
    private double calcCpuUsage(long[] prevTicks, long[] ticks) {
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];

        long total = user + nice + sys + idle + iowait + irq + softirq + steal;
        if (total <= 0) {
            return 0.0;
        }
        // CPU 使用率 = (total - idle) / total * 100
        return (double) (total - idle) / total * 100;
    }

    /**
     * 阈值告警检查（仅日志告警，可扩展接入钉钉/企微通知）
     */
    private void checkAlerts(double cpuUsage, double memUsage, double diskUsage) {
        if (cpuUsage > CPU_ALERT_THRESHOLD) {
            logger.warn("【拼团监控告警】CPU 使用率过高: {}%, 阈值 {}%, hostIp={}",
                    String.format("%.1f", cpuUsage), CPU_ALERT_THRESHOLD, hostIp);
        }
        if (memUsage > MEM_ALERT_THRESHOLD) {
            logger.warn("【拼团监控告警】内存使用率过高: {}%, 阈值 {}%, hostIp={}",
                    String.format("%.1f", memUsage), MEM_ALERT_THRESHOLD, hostIp);
        }
        if (diskUsage > DISK_ALERT_THRESHOLD) {
            logger.warn("【拼团监控告警】磁盘使用率过高: {}%, 阈值 {}%, hostIp={}",
                    String.format("%.1f", diskUsage), DISK_ALERT_THRESHOLD, hostIp);
        }
    }
}
