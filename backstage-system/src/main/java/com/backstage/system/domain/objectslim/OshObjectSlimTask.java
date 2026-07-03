package com.backstage.system.domain.objectslim;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("osh_object_slim_task")
public class OshObjectSlimTask extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bucket;
    private String objectKey;
    private String backupBucket;
    private String backupKey;
    private String fileName;
    private String fileSuffix;
    private String fileType;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private Long originalSize;
    private Long slimSize;
    private Long savedSize;
    private BigDecimal savedRatio;
    private String slimMode;
    private String status;
    private String codecBefore;
    private String codecAfter;
    private String resolutionBefore;
    private String resolutionAfter;
    private BigDecimal durationBefore;
    private BigDecimal durationAfter;
    private Long bitrateBefore;
    private Long bitrateAfter;
    private Integer attemptCount;
    private String lastError;
    private LocalDateTime lastScanTime;
    private LocalDateTime lastRunTime;
    private LocalDateTime backupDeletedTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getBackupBucket() { return backupBucket; }
    public void setBackupBucket(String backupBucket) { this.backupBucket = backupBucket; }
    public String getBackupKey() { return backupKey; }
    public void setBackupKey(String backupKey) { this.backupKey = backupKey; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileSuffix() { return fileSuffix; }
    public void setFileSuffix(String fileSuffix) { this.fileSuffix = fileSuffix; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
    public Long getOriginalSize() { return originalSize; }
    public void setOriginalSize(Long originalSize) { this.originalSize = originalSize; }
    public Long getSlimSize() { return slimSize; }
    public void setSlimSize(Long slimSize) { this.slimSize = slimSize; }
    public Long getSavedSize() { return savedSize; }
    public void setSavedSize(Long savedSize) { this.savedSize = savedSize; }
    public BigDecimal getSavedRatio() { return savedRatio; }
    public void setSavedRatio(BigDecimal savedRatio) { this.savedRatio = savedRatio; }
    public String getSlimMode() { return slimMode; }
    public void setSlimMode(String slimMode) { this.slimMode = slimMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCodecBefore() { return codecBefore; }
    public void setCodecBefore(String codecBefore) { this.codecBefore = codecBefore; }
    public String getCodecAfter() { return codecAfter; }
    public void setCodecAfter(String codecAfter) { this.codecAfter = codecAfter; }
    public String getResolutionBefore() { return resolutionBefore; }
    public void setResolutionBefore(String resolutionBefore) { this.resolutionBefore = resolutionBefore; }
    public String getResolutionAfter() { return resolutionAfter; }
    public void setResolutionAfter(String resolutionAfter) { this.resolutionAfter = resolutionAfter; }
    public BigDecimal getDurationBefore() { return durationBefore; }
    public void setDurationBefore(BigDecimal durationBefore) { this.durationBefore = durationBefore; }
    public BigDecimal getDurationAfter() { return durationAfter; }
    public void setDurationAfter(BigDecimal durationAfter) { this.durationAfter = durationAfter; }
    public Long getBitrateBefore() { return bitrateBefore; }
    public void setBitrateBefore(Long bitrateBefore) { this.bitrateBefore = bitrateBefore; }
    public Long getBitrateAfter() { return bitrateAfter; }
    public void setBitrateAfter(Long bitrateAfter) { this.bitrateAfter = bitrateAfter; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getLastScanTime() { return lastScanTime; }
    public void setLastScanTime(LocalDateTime lastScanTime) { this.lastScanTime = lastScanTime; }
    public LocalDateTime getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(LocalDateTime lastRunTime) { this.lastRunTime = lastRunTime; }
    public LocalDateTime getBackupDeletedTime() { return backupDeletedTime; }
    public void setBackupDeletedTime(LocalDateTime backupDeletedTime) { this.backupDeletedTime = backupDeletedTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
