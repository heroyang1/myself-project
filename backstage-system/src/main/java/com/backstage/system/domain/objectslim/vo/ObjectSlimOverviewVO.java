package com.backstage.system.domain.objectslim.vo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class ObjectSlimOverviewVO {
    private Long total = 0L;
    private Long videoCount = 0L;
    private Long successCount = 0L;
    private Long failedCount = 0L;
    private Long runningCount = 0L;
    private Long pendingCount = 0L;
    private Long originalSize = 0L;
    private Long slimSize = 0L;
    private Long savedSize = 0L;
    private BigDecimal savedRatio = BigDecimal.ZERO;
    private Map<String, Long> statusCounts = new LinkedHashMap<>();

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getVideoCount() { return videoCount; }
    public void setVideoCount(Long videoCount) { this.videoCount = videoCount; }
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    public Long getFailedCount() { return failedCount; }
    public void setFailedCount(Long failedCount) { this.failedCount = failedCount; }
    public Long getRunningCount() { return runningCount; }
    public void setRunningCount(Long runningCount) { this.runningCount = runningCount; }
    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }
    public Long getOriginalSize() { return originalSize; }
    public void setOriginalSize(Long originalSize) { this.originalSize = originalSize; }
    public Long getSlimSize() { return slimSize; }
    public void setSlimSize(Long slimSize) { this.slimSize = slimSize; }
    public Long getSavedSize() { return savedSize; }
    public void setSavedSize(Long savedSize) { this.savedSize = savedSize; }
    public BigDecimal getSavedRatio() { return savedRatio; }
    public void setSavedRatio(BigDecimal savedRatio) { this.savedRatio = savedRatio; }
    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }
}
