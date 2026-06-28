package com.backstage.system.service.objectslim.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.backstage.common.config.OssProperties;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.SysConfig;
import com.backstage.system.domain.objectslim.ObjectSlimStatus;
import com.backstage.system.domain.objectslim.OshObjectSlimTask;
import com.backstage.system.domain.objectslim.dto.ObjectSlimPageQuery;
import com.backstage.system.domain.objectslim.dto.ObjectSlimRunRequest;
import com.backstage.system.domain.objectslim.dto.ObjectSlimScanRequest;
import com.backstage.system.domain.objectslim.vo.ObjectSlimConfigVO;
import com.backstage.system.domain.objectslim.vo.ObjectSlimOverviewVO;
import com.backstage.system.domain.objectslim.vo.ObjectSlimRunResultVO;
import com.backstage.system.mapper.SysConfigMapper;
import com.backstage.system.mapper.objectslim.OshObjectSlimTaskMapper;
import com.backstage.system.service.ISysConfigService;
import com.backstage.system.service.objectslim.ObjectSlimService;
import com.backstage.system.utils.OssUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ObjectSlimServiceImpl implements ObjectSlimService {
    private static final Logger log = LoggerFactory.getLogger(ObjectSlimServiceImpl.class);

    private static final String CONFIG_ENABLED = "object.slim.enabled";
    private static final String HANDLER_NAME = "osh-object-storage-slimmer";
    private static final List<String> VIDEO_SUFFIXES = Arrays.asList("mp4", "mov", "m4v", "webm", "mkv", "avi");
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_RUN_LIMIT = 20;
    private static final int MAX_SCAN_LIMIT = 1000;
    private static final BigDecimal MIN_SAVED_RATIO = new BigDecimal("0.10");
    private static final BigDecimal MAX_DURATION_DIFF_SECONDS = new BigDecimal("2.0");
    private static final String DEFAULT_SLIM_MODE = "balanced";
    private static final List<SlimProfile> SLIM_PROFILES = Arrays.asList(
            new SlimProfile("quality", "高画质", "适合付费客户和核心课程，优先保留画质，不降分辨率，节省空间较温和。",
                    21, "medium", "160k", null, new BigDecimal("0.05")),
            new SlimProfile("balanced", "均衡默认", "默认策略，不降分辨率，只压码率，兼顾画质和体积。",
                    23, "veryfast", "128k", null, MIN_SAVED_RATIO),
            new SlimProfile("storage_saver", "节省优先", "适合成本敏感场景，可把超大分辨率视频限制到 1080p。",
                    27, "veryfast", "96k", 1080, new BigDecimal("0.15")),
            new SlimProfile("low_cost", "极省空间", "适合低成本客户，可降到 720p，体积优先，画质会更激进。",
                    30, "faster", "80k", 720, new BigDecimal("0.20"))
    );

    @Value("${object.slim.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${object.slim.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${object.slim.temp-dir:${java.io.tmpdir}/osh-object-slim}")
    private String tempDir;

    @Value("${object.slim.ffmpeg-timeout-seconds:7200}")
    private long ffmpegTimeoutSeconds;

    @Value("${object.slim.ffprobe-timeout-seconds:120}")
    private long ffprobeTimeoutSeconds;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private OssProperties ossProperties;

    @Autowired
    private OshObjectSlimTaskMapper taskMapper;

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public ObjectSlimConfigVO getConfig() {
        ObjectSlimConfigVO vo = new ObjectSlimConfigVO();
        vo.setBucket(defaultBucket());
        vo.setBackupBucket(defaultBackupBucket(defaultBucket()));
        vo.setEnabled(isEnabled());
        vo.setFfmpegPath(resolveCommandPath(ffmpegPath));
        vo.setFfprobePath(resolveCommandPath(ffprobePath));
        vo.setFfmpegAvailable(commandAvailable(ffmpegPath));
        vo.setFfprobeAvailable(commandAvailable(ffprobePath));
        vo.setXxlJobHandler(HANDLER_NAME);
        vo.setDefaultLimit(DEFAULT_LIMIT);
        vo.setMinSavedRatioPercent(MIN_SAVED_RATIO.multiply(new BigDecimal("100")).intValue());
        vo.setDefaultSlimMode(DEFAULT_SLIM_MODE);
        vo.setSlimModes(buildSlimModeVos());
        if (!Boolean.TRUE.equals(vo.getFfmpegAvailable()) || !Boolean.TRUE.equals(vo.getFfprobeAvailable())) {
            vo.setMessage("ffmpeg 或 ffprobe 不可用，任务会跳过转码");
        }
        return vo;
    }

    @Override
    public ObjectSlimConfigVO setEnabled(boolean enabled) {
        SysConfig query = new SysConfig();
        query.setConfigKey(CONFIG_ENABLED);
        SysConfig existing = sysConfigMapper.selectConfig(query);
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigName("对象存储视频瘦身开关");
            config.setConfigKey(CONFIG_ENABLED);
            config.setConfigValue(String.valueOf(enabled));
            config.setConfigType("N");
            config.setCreateBy("system");
            config.setRemark("false=关闭，true=开启；关闭时 XXL-JOB handler 直接退出");
            sysConfigService.insertConfig(config);
        } else {
            existing.setConfigValue(String.valueOf(enabled));
            existing.setUpdateBy("system");
            sysConfigService.updateConfig(existing);
        }
        return getConfig();
    }

    @Override
    public ObjectSlimOverviewVO overview() {
        String bucket = requireCurrentBucket(null);
        List<OshObjectSlimTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<OshObjectSlimTask>()
                .eq(OshObjectSlimTask::getBucket, bucket)
                .eq(OshObjectSlimTask::getDeleteFlag, (byte) 0));
        ObjectSlimOverviewVO vo = new ObjectSlimOverviewVO();
        Map<String, Long> statusCounts = new HashMap<>();
        long originalSize = 0L;
        long slimSize = 0L;
        long savedSize = 0L;
        for (OshObjectSlimTask task : tasks) {
            vo.setTotal(vo.getTotal() + 1);
            if ("video".equals(task.getFileType())) {
                vo.setVideoCount(vo.getVideoCount() + 1);
            }
            String status = StringUtils.defaultString(task.getStatus(), ObjectSlimStatus.PENDING);
            statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
            if (ObjectSlimStatus.SUCCESS.equals(status)) {
                vo.setSuccessCount(vo.getSuccessCount() + 1);
            } else if (ObjectSlimStatus.FAILED.equals(status)) {
                vo.setFailedCount(vo.getFailedCount() + 1);
            } else if (ObjectSlimStatus.RUNNING.equals(status)) {
                vo.setRunningCount(vo.getRunningCount() + 1);
            } else if (ObjectSlimStatus.PENDING.equals(status)) {
                vo.setPendingCount(vo.getPendingCount() + 1);
            }
            originalSize += nullToZero(task.getOriginalSize());
            slimSize += nullToZero(task.getSlimSize());
            savedSize += nullToZero(task.getSavedSize());
        }
        vo.setOriginalSize(originalSize);
        vo.setSlimSize(slimSize);
        vo.setSavedSize(savedSize);
        if (originalSize > 0 && savedSize > 0) {
            vo.setSavedRatio(new BigDecimal(savedSize).divide(new BigDecimal(originalSize), 4, RoundingMode.HALF_UP));
        }
        vo.setStatusCounts(statusCounts);
        return vo;
    }

    @Override
    public PageInfo<OshObjectSlimTask> page(ObjectSlimPageQuery query) {
        ObjectSlimPageQuery actual = normalizePageQuery(query);
        String bucket = requireCurrentBucket(actual.getBucket());
        LambdaQueryWrapper<OshObjectSlimTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshObjectSlimTask::getDeleteFlag, (byte) 0);
        wrapper.eq(OshObjectSlimTask::getBucket, bucket);
        if (StringUtils.isNotEmpty(actual.getStatus())) {
            wrapper.eq(OshObjectSlimTask::getStatus, actual.getStatus());
        }
        if (StringUtils.isNotEmpty(actual.getFileType())) {
            wrapper.eq(OshObjectSlimTask::getFileType, actual.getFileType());
        }
        if (StringUtils.isNotEmpty(actual.getKeyword())) {
            wrapper.and(w -> w.like(OshObjectSlimTask::getObjectKey, actual.getKeyword())
                    .or()
                    .like(OshObjectSlimTask::getSourceTitle, actual.getKeyword()));
        }
        wrapper.orderByDesc(OshObjectSlimTask::getUpdateTime).orderByDesc(OshObjectSlimTask::getId);
        PageHelper.startPage(actual.getPageNum(), actual.getPageSize());
        return new PageInfo<>(taskMapper.selectList(wrapper));
    }

    @Override
    public ObjectSlimRunResultVO scan(ObjectSlimScanRequest request) {
        ObjectSlimScanRequest actual = normalizeScanRequest(request);
        ObjectSlimRunResultVO result = new ObjectSlimRunResultVO();
        List<S3ObjectSummary> summaries = ossUtil.listObjects(actual.getBucket(), actual.getPrefix(), actual.getLimit());
        LocalDateTime now = LocalDateTime.now();
        for (S3ObjectSummary summary : summaries) {
            String objectKey = summary.getKey();
            if (StringUtils.isEmpty(objectKey) || objectKey.endsWith("/")) {
                continue;
            }
            upsertTask(actual.getBucket(), objectKey, summary.getSize(), now);
            result.setScanned(result.getScanned() + 1);
        }
        result.setMessage("扫描完成");
        return result;
    }

    @Override
    public ObjectSlimRunResultVO runSlim(ObjectSlimRunRequest request) {
        ObjectSlimRunRequest actual = normalizeRunRequest(request);
        ObjectSlimRunResultVO result = new ObjectSlimRunResultVO();
        if (!commandAvailable(ffmpegPath) || !commandAvailable(ffprobePath)) {
            result.setMessage("ffmpeg 或 ffprobe 不可用，未执行");
            return result;
        }
        List<OshObjectSlimTask> tasks = selectRunnableTasks(actual);
        for (OshObjectSlimTask task : tasks) {
            if (Boolean.TRUE.equals(actual.getDryRun())) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            result.setProcessed(result.getProcessed() + 1);
            boolean ok = slimOne(task, actual.getSlimMode());
            if (ok) {
                result.setSuccess(result.getSuccess() + 1);
            } else {
                result.setFailed(result.getFailed() + 1);
            }
        }
        result.setMessage("执行完成");
        return result;
    }

    @Override
    public ObjectSlimRunResultVO runFromJob(String jobParam) {
        ObjectSlimRunResultVO result = new ObjectSlimRunResultVO();
        if (!isEnabled()) {
            result.setMessage("定时瘦身开关已关闭，XXL-JOB 未执行");
            return result;
        }
        ObjectSlimRunRequest request = new ObjectSlimRunRequest();
        String mode = null;
        if (StringUtils.isNotEmpty(jobParam)) {
            try {
                JSONObject json = JSON.parseObject(jobParam);
                request.setLimit(json.getInteger("limit"));
                request.setRetryFailed(json.getBoolean("retryFailed"));
                request.setDryRun(json.getBoolean("dryRun"));
                request.setSlimMode(firstNotBlank(json.getString("slimMode"), json.getString("qualityMode")));
                mode = json.getString("mode");
                if ("scan".equalsIgnoreCase(mode) || "scan-and-slim".equalsIgnoreCase(mode)) {
                    ObjectSlimScanRequest scanRequest = new ObjectSlimScanRequest();
                    scanRequest.setPrefix(json.getString("prefix"));
                    scanRequest.setLimit(json.getInteger("scanLimit"));
                    result = scan(scanRequest);
                }
            } catch (Exception e) {
                log.warn("解析对象瘦身 XXL-JOB 参数失败: {}", e.getMessage());
            }
        }
        if ("scan".equalsIgnoreCase(mode)) {
            return result;
        }
        return runSlim(request);
    }

    @Override
    public void retry(Long id) {
        OshObjectSlimTask task = requireTask(id);
        task.setStatus(ObjectSlimStatus.PENDING);
        task.setLastError(null);
        taskMapper.updateById(task);
    }

    @Override
    public void rollback(Long id) {
        OshObjectSlimTask task = requireTask(id);
        if (StringUtils.isEmpty(task.getBackupBucket()) || StringUtils.isEmpty(task.getBackupKey())) {
            throw new ServiceException("该任务没有备份对象，无法回滚");
        }
        if (!ossUtil.objectExists(task.getBackupBucket(), task.getBackupKey())) {
            throw new ServiceException("备份对象不存在，无法回滚");
        }
        ossUtil.copyObject(task.getBackupBucket(), task.getBackupKey(), task.getBucket(), task.getObjectKey());
        task.setStatus(ObjectSlimStatus.ROLLED_BACK);
        task.setLastError(null);
        task.setLastRunTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    public void deleteBackup(Long id) {
        OshObjectSlimTask task = requireTask(id);
        // 备份只能在管理员确认瘦身后视频可播放时手动删除，定时任务不自动清理备份。
        if (!ObjectSlimStatus.SUCCESS.equals(task.getStatus())) {
            throw new ServiceException("只有成功瘦身且确认可播放的任务允许删除备份");
        }
        if (StringUtils.isEmpty(task.getBackupBucket()) || StringUtils.isEmpty(task.getBackupKey())) {
            throw new ServiceException("该任务没有备份对象");
        }
        ossUtil.deleteObject(task.getBackupBucket(), task.getBackupKey());
        task.setStatus(ObjectSlimStatus.BACKUP_DELETED);
        task.setBackupDeletedTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    public String getPreviewUrl(Long id, Integer minute) {
        OshObjectSlimTask task = requireTask(id);
        int expire = minute == null ? 30 : Math.max(1, Math.min(minute, 360));
        return ossUtil.getSignedUrl(task.getBucket(), task.getObjectKey(), expire);
    }

    private void upsertTask(String bucket, String objectKey, long size, LocalDateTime now) {
        OshObjectSlimTask existing = taskMapper.selectOne(new LambdaQueryWrapper<OshObjectSlimTask>()
                .eq(OshObjectSlimTask::getBucket, bucket)
                .eq(OshObjectSlimTask::getObjectKey, objectKey)
                .last("limit 1"));
        String suffix = suffix(objectKey);
        String fileType = fileType(suffix);
        if (existing == null) {
            OshObjectSlimTask task = new OshObjectSlimTask();
            task.setBucket(bucket);
            task.setObjectKey(objectKey);
            task.setFileName(fileName(objectKey));
            task.setFileSuffix(suffix);
            task.setFileType(fileType);
            task.setSourceType("r2_scan");
            task.setOriginalSize(size);
            task.setStatus("video".equals(fileType) ? ObjectSlimStatus.PENDING : ObjectSlimStatus.SKIPPED);
            task.setLastScanTime(now);
            taskMapper.insert(task);
            return;
        }
        existing.setFileSuffix(suffix);
        existing.setFileType(fileType);
        existing.setLastScanTime(now);
        if (!ObjectSlimStatus.SUCCESS.equals(existing.getStatus())
                && !ObjectSlimStatus.BACKUP_DELETED.equals(existing.getStatus())
                && !ObjectSlimStatus.ROLLED_BACK.equals(existing.getStatus())) {
            existing.setOriginalSize(size);
        }
        if (!"video".equals(fileType) && ObjectSlimStatus.PENDING.equals(existing.getStatus())) {
            existing.setStatus(ObjectSlimStatus.SKIPPED);
            existing.setLastError("非视频文件不执行瘦身");
        }
        taskMapper.updateById(existing);
    }

    private boolean slimOne(OshObjectSlimTask rawTask, String slimMode) {
        SlimProfile profile = resolveProfile(slimMode);
        OshObjectSlimTask task = requireTask(rawTask.getId());
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(ObjectSlimStatus.RUNNING);
        task.setSlimMode(profile.code);
        task.setAttemptCount(nullToZero(task.getAttemptCount()) + 1);
        task.setLastRunTime(now);
        task.setLastError(null);
        taskMapper.updateById(task);

        File workDir = null;
        try {
            if (!"video".equals(task.getFileType())) {
                markSkipped(task, "非视频文件不执行瘦身");
                return false;
            }
            if (!ossUtil.objectExists(task.getBucket(), task.getObjectKey())) {
                markFailed(task, "原对象不存在");
                return false;
            }
            ObjectMetadata metadata = ossUtil.getObjectMetadata(task.getBucket(), task.getObjectKey());
            long originalSize = metadata.getContentLength();
            if (originalSize <= 0) {
                markFailed(task, "原对象大小无效");
                return false;
            }
            task.setOriginalSize(originalSize);

            String backupBucket = StringUtils.defaultIfEmpty(task.getBackupBucket(), defaultBackupBucket(task.getBucket()));
            String backupKey = StringUtils.defaultIfEmpty(task.getBackupKey(), buildBackupKey(task.getObjectKey()));
            ossUtil.ensureBucket(backupBucket);
            // 先复制到备份桶，再覆盖原 key，保证现有播放地址不变且失败时可回滚。
            if (!ossUtil.objectExists(backupBucket, backupKey)) {
                ossUtil.copyObject(task.getBucket(), task.getObjectKey(), backupBucket, backupKey);
            }
            task.setBackupBucket(backupBucket);
            task.setBackupKey(backupKey);
            taskMapper.updateById(task);

            workDir = Files.createDirectories(new File(tempDir, UUID.randomUUID().toString()).toPath()).toFile();
            File input = new File(workDir, "input." + StringUtils.defaultIfEmpty(task.getFileSuffix(), "mp4"));
            File output = new File(workDir, "output.mp4");
            ossUtil.downloadObject(task.getBucket(), task.getObjectKey(), input);

            MediaInfo before = probe(input);
            task.setCodecBefore(before.codec);
            task.setResolutionBefore(before.resolution());
            task.setDurationBefore(before.duration);
            task.setBitrateBefore(before.bitrate);
            taskMapper.updateById(task);

            runFfmpeg(input, output, profile, before);
            if (!output.exists() || output.length() <= 0) {
                markFailed(task, "ffmpeg 未生成有效输出");
                return false;
            }

            MediaInfo after = probe(output);
            BigDecimal durationDiff = abs(nullToZero(before.duration).subtract(nullToZero(after.duration)));
            if (durationDiff.compareTo(MAX_DURATION_DIFF_SECONDS) > 0) {
                markFailed(task, "输出视频时长差过大: " + durationDiff + "秒");
                return false;
            }

            long slimSize = output.length();
            long savedSize = originalSize - slimSize;
            if (savedSize <= 0) {
                markNoNeed(task, before, after, slimSize, "输出文件未变小");
                return false;
            }
            BigDecimal savedRatio = new BigDecimal(savedSize).divide(new BigDecimal(originalSize), 4, RoundingMode.HALF_UP);
            if (savedRatio.compareTo(profile.minSavedRatio) < 0) {
                markNoNeed(task, before, after, slimSize, "节省比例不足 " + profile.minSavedRatio.multiply(new BigDecimal("100")) + "%");
                return false;
            }

            ossUtil.uploadObject(task.getBucket(), task.getObjectKey(), output, "video/mp4");
            task.setCodecAfter(after.codec);
            task.setResolutionAfter(after.resolution());
            task.setDurationAfter(after.duration);
            task.setBitrateAfter(after.bitrate);
            task.setSlimSize(slimSize);
            task.setSavedSize(savedSize);
            task.setSavedRatio(savedRatio);
            task.setStatus(ObjectSlimStatus.SUCCESS);
            task.setLastError(null);
            taskMapper.updateById(task);
            return true;
        } catch (Exception e) {
            log.warn("对象瘦身失败, id={}, key={}, error={}", task.getId(), task.getObjectKey(), e.getMessage(), e);
            markFailed(task, truncate(e.getMessage(), 1000));
            return false;
        } finally {
            if (workDir != null) {
                FileUtils.deleteQuietly(workDir);
            }
        }
    }

    private void markSkipped(OshObjectSlimTask task, String reason) {
        task.setStatus(ObjectSlimStatus.SKIPPED);
        task.setLastError(reason);
        taskMapper.updateById(task);
    }

    private void markFailed(OshObjectSlimTask task, String reason) {
        task.setStatus(ObjectSlimStatus.FAILED);
        task.setLastError(truncate(reason, 1000));
        taskMapper.updateById(task);
    }

    private void markNoNeed(OshObjectSlimTask task, MediaInfo before, MediaInfo after, long slimSize, String reason) {
        long originalSize = nullToZero(task.getOriginalSize());
        long savedSize = Math.max(0, originalSize - slimSize);
        task.setCodecAfter(after.codec);
        task.setResolutionAfter(after.resolution());
        task.setDurationAfter(after.duration);
        task.setBitrateAfter(after.bitrate);
        task.setSlimSize(slimSize);
        task.setSavedSize(savedSize);
        if (originalSize > 0) {
            task.setSavedRatio(new BigDecimal(savedSize).divide(new BigDecimal(originalSize), 4, RoundingMode.HALF_UP));
        }
        if (StringUtils.isEmpty(task.getCodecBefore())) {
            task.setCodecBefore(before.codec);
        }
        task.setStatus(ObjectSlimStatus.NO_NEED);
        task.setLastError(reason);
        taskMapper.updateById(task);
    }

    private List<OshObjectSlimTask> selectRunnableTasks(ObjectSlimRunRequest request) {
        LambdaQueryWrapper<OshObjectSlimTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshObjectSlimTask::getDeleteFlag, (byte) 0);
        wrapper.eq(OshObjectSlimTask::getBucket, requireCurrentBucket(null));
        wrapper.eq(OshObjectSlimTask::getFileType, "video");
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            wrapper.in(OshObjectSlimTask::getId, request.getIds());
        } else if (Boolean.TRUE.equals(request.getRetryFailed())) {
            wrapper.in(OshObjectSlimTask::getStatus, ObjectSlimStatus.FAILED, ObjectSlimStatus.NO_NEED, ObjectSlimStatus.SKIPPED);
        } else {
            wrapper.in(OshObjectSlimTask::getStatus, ObjectSlimStatus.PENDING, ObjectSlimStatus.FAILED);
        }
        wrapper.orderByAsc(OshObjectSlimTask::getLastRunTime).orderByAsc(OshObjectSlimTask::getId);
        wrapper.last("limit " + Math.max(1, Math.min(request.getLimit(), MAX_RUN_LIMIT)));
        return taskMapper.selectList(wrapper);
    }

    private void runFfmpeg(File input, File output, SlimProfile profile, MediaInfo before) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");
        command.add("-i");
        command.add(input.getAbsolutePath());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("0:a?");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(profile.preset);
        command.add("-crf");
        command.add(String.valueOf(profile.crf));
        command.add("-pix_fmt");
        command.add("yuv420p");
        if (profile.maxHeight != null && before.height != null && before.height > profile.maxHeight) {
            command.add("-vf");
            command.add("scale=-2:" + profile.maxHeight);
        }
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(profile.audioBitrate);
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.getAbsolutePath());
        CommandResult result = runCommand(command, ffmpegTimeoutSeconds);
        if (result.exitCode != 0) {
            throw new ServiceException("ffmpeg 执行失败: " + truncate(result.output, 800));
        }
    }

    private SlimProfile resolveProfile(String slimMode) {
        String code = StringUtils.defaultIfBlank(slimMode, DEFAULT_SLIM_MODE);
        for (SlimProfile profile : SLIM_PROFILES) {
            if (profile.code.equalsIgnoreCase(code)) {
                return profile;
            }
        }
        return resolveProfile(DEFAULT_SLIM_MODE);
    }

    private List<ObjectSlimConfigVO.SlimModeVO> buildSlimModeVos() {
        List<ObjectSlimConfigVO.SlimModeVO> modes = new ArrayList<>();
        for (SlimProfile profile : SLIM_PROFILES) {
            ObjectSlimConfigVO.SlimModeVO vo = new ObjectSlimConfigVO.SlimModeVO();
            vo.setCode(profile.code);
            vo.setName(profile.name);
            vo.setDescription(profile.description);
            vo.setCrf(profile.crf);
            vo.setPreset(profile.preset);
            vo.setAudioBitrate(profile.audioBitrate);
            vo.setMaxHeight(profile.maxHeight);
            vo.setMinSavedRatioPercent(profile.minSavedRatio.multiply(new BigDecimal("100")).intValue());
            modes.add(vo);
        }
        return modes;
    }

    private MediaInfo probe(File file) throws Exception {
        List<String> command = Arrays.asList(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name,width,height,bit_rate,duration:format=duration,bit_rate",
                "-of", "json",
                file.getAbsolutePath()
        );
        CommandResult result = runCommand(command, ffprobeTimeoutSeconds);
        if (result.exitCode != 0) {
            throw new ServiceException("ffprobe 执行失败: " + truncate(result.output, 800));
        }
        JSONObject json = JSON.parseObject(result.output);
        JSONArray streams = json.getJSONArray("streams");
        JSONObject stream = streams != null && !streams.isEmpty() ? streams.getJSONObject(0) : new JSONObject();
        JSONObject format = json.getJSONObject("format");
        MediaInfo info = new MediaInfo();
        info.codec = stream.getString("codec_name");
        info.width = stream.getInteger("width");
        info.height = stream.getInteger("height");
        info.duration = decimal(firstNotBlank(stream.getString("duration"), format == null ? null : format.getString("duration")));
        info.bitrate = longValue(firstNotBlank(stream.getString("bit_rate"), format == null ? null : format.getString("bit_rate")));
        if (info.width == null || info.height == null || info.duration == null) {
            throw new ServiceException("ffprobe 未读取到有效视频信息");
        }
        return info;
    }

    private CommandResult runCommand(List<String> command, long timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        final StringBuffer output = new StringBuffer();
        Thread outputReader = new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                } catch (Exception e) {
                    output.append("读取命令输出失败: ").append(e.getMessage()).append('\n');
                }
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished;
        if (timeoutSeconds > 0) {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(1000);
                CommandResult timeoutResult = new CommandResult();
                timeoutResult.exitCode = -1;
                timeoutResult.output = "命令执行超时 " + timeoutSeconds + " 秒: " + command + "\n" + output;
                return timeoutResult;
            }
        } else {
            process.waitFor();
        }
        outputReader.join(1000);
        CommandResult result = new CommandResult();
        result.exitCode = process.exitValue();
        result.output = output.toString();
        return result;
    }

    private boolean isEnabled() {
        return Boolean.parseBoolean(sysConfigService.selectConfigByKey(CONFIG_ENABLED));
    }

    private String defaultBucket() {
        return ossProperties.getBucketName();
    }

    private String defaultBackupBucket(String bucket) {
        return bucket + "-backup";
    }

    private String buildBackupKey(String objectKey) {
        return "object-slim-backup/" + DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now())
                + "/" + UUID.randomUUID() + "/" + objectKey;
    }

    private ObjectSlimPageQuery normalizePageQuery(ObjectSlimPageQuery query) {
        ObjectSlimPageQuery actual = query == null ? new ObjectSlimPageQuery() : query;
        actual.setPageNum(Math.max(1, nullToZero(actual.getPageNum(), 1)));
        actual.setPageSize(Math.max(1, Math.min(nullToZero(actual.getPageSize(), 20), 100)));
        return actual;
    }

    private ObjectSlimScanRequest normalizeScanRequest(ObjectSlimScanRequest request) {
        ObjectSlimScanRequest actual = request == null ? new ObjectSlimScanRequest() : request;
        actual.setBucket(requireCurrentBucket(actual.getBucket()));
        actual.setLimit(Math.max(1, Math.min(nullToZero(actual.getLimit(), 200), MAX_SCAN_LIMIT)));
        return actual;
    }

    private ObjectSlimRunRequest normalizeRunRequest(ObjectSlimRunRequest request) {
        ObjectSlimRunRequest actual = request == null ? new ObjectSlimRunRequest() : request;
        actual.setLimit(Math.max(1, Math.min(nullToZero(actual.getLimit(), DEFAULT_LIMIT), MAX_RUN_LIMIT)));
        actual.setSlimMode(resolveProfile(actual.getSlimMode()).code);
        return actual;
    }

    private OshObjectSlimTask requireTask(Long id) {
        if (id == null) {
            throw new ServiceException("任务ID不能为空");
        }
        OshObjectSlimTask task = taskMapper.selectById(id);
        if (task == null || (task.getDeleteFlag() != null && task.getDeleteFlag() == 1)) {
            throw new ServiceException("任务不存在");
        }
        requireCurrentBucket(task.getBucket());
        return task;
    }

    private String requireCurrentBucket(String requestedBucket) {
        String configuredBucket = defaultBucket();
        if (StringUtils.isBlank(configuredBucket)) {
            throw new ServiceException("当前环境对象存储桶未配置");
        }
        String actualBucket = StringUtils.defaultIfBlank(requestedBucket, configuredBucket);
        // 只允许操作当前环境配置桶，避免 QA/生产共用代码时误扫或误写其他桶。
        if (!StringUtils.equals(actualBucket, configuredBucket)) {
            throw new ServiceException("只能操作当前环境配置桶: " + configuredBucket);
        }
        return actualBucket;
    }

    private String fileType(String suffix) {
        if (StringUtils.isEmpty(suffix)) {
            return "other";
        }
        String lower = suffix.toLowerCase(Locale.ROOT);
        if (VIDEO_SUFFIXES.contains(lower)) {
            return "video";
        }
        if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(lower)) {
            return "image";
        }
        if (Arrays.asList("mp3", "wav", "aac", "flac", "m4a").contains(lower)) {
            return "audio";
        }
        if (Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md").contains(lower)) {
            return "doc";
        }
        return "other";
    }

    private String suffix(String objectKey) {
        if (StringUtils.isEmpty(objectKey) || !objectKey.contains(".")) {
            return "";
        }
        return objectKey.substring(objectKey.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String fileName(String objectKey) {
        if (objectKey == null) {
            return "";
        }
        int idx = objectKey.lastIndexOf('/');
        return idx >= 0 ? objectKey.substring(idx + 1) : objectKey;
    }

    private boolean commandAvailable(String command) {
        try {
            CommandResult result = runCommand(Arrays.asList("sh", "-c", "command -v " + shellQuote(command)), 0);
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveCommandPath(String command) {
        try {
            CommandResult result = runCommand(Arrays.asList("sh", "-c", "command -v " + shellQuote(command)), 0);
            if (result.exitCode == 0) {
                return result.output.trim();
            }
        } catch (Exception ignored) {
        }
        return command;
    }

    private String shellQuote(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\\''") + "'";
    }

    private String firstNotBlank(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private BigDecimal decimal(String value) {
        if (!NumberUtils.isCreatable(value)) {
            return null;
        }
        return new BigDecimal(value).setScale(3, RoundingMode.HALF_UP);
    }

    private Long longValue(String value) {
        if (!NumberUtils.isCreatable(value)) {
            return null;
        }
        return new BigDecimal(value).longValue();
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int nullToZero(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal abs(BigDecimal value) {
        return value.signum() < 0 ? value.negate() : value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static class SlimProfile {
        private final String code;
        private final String name;
        private final String description;
        private final int crf;
        private final String preset;
        private final String audioBitrate;
        private final Integer maxHeight;
        private final BigDecimal minSavedRatio;

        private SlimProfile(String code, String name, String description, int crf, String preset,
                            String audioBitrate, Integer maxHeight, BigDecimal minSavedRatio) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.crf = crf;
            this.preset = preset;
            this.audioBitrate = audioBitrate;
            this.maxHeight = maxHeight;
            this.minSavedRatio = minSavedRatio;
        }
    }

    private static class MediaInfo {
        private String codec;
        private Integer width;
        private Integer height;
        private BigDecimal duration;
        private Long bitrate;

        private String resolution() {
            if (width == null || height == null) {
                return null;
            }
            return width + "x" + height;
        }
    }

    private static class CommandResult {
        private int exitCode;
        private String output;
    }
}
