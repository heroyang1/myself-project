package com.backstage.system.domain.objectslim.vo;

import java.util.List;

public class ObjectSlimConfigVO {
    private String bucket;
    private String backupBucket;
    private Boolean enabled;
    private Boolean ffmpegAvailable;
    private Boolean ffprobeAvailable;
    private String xxlJobHandler;
    private String ffmpegPath;
    private String ffprobePath;
    private Integer defaultLimit;
    private Integer minSavedRatioPercent;
    private String defaultSlimMode;
    private List<SlimModeVO> slimModes;
    private String message;

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getBackupBucket() { return backupBucket; }
    public void setBackupBucket(String backupBucket) { this.backupBucket = backupBucket; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getFfmpegAvailable() { return ffmpegAvailable; }
    public void setFfmpegAvailable(Boolean ffmpegAvailable) { this.ffmpegAvailable = ffmpegAvailable; }
    public Boolean getFfprobeAvailable() { return ffprobeAvailable; }
    public void setFfprobeAvailable(Boolean ffprobeAvailable) { this.ffprobeAvailable = ffprobeAvailable; }
    public String getXxlJobHandler() { return xxlJobHandler; }
    public void setXxlJobHandler(String xxlJobHandler) { this.xxlJobHandler = xxlJobHandler; }
    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }
    public String getFfprobePath() { return ffprobePath; }
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; }
    public Integer getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(Integer defaultLimit) { this.defaultLimit = defaultLimit; }
    public Integer getMinSavedRatioPercent() { return minSavedRatioPercent; }
    public void setMinSavedRatioPercent(Integer minSavedRatioPercent) { this.minSavedRatioPercent = minSavedRatioPercent; }
    public String getDefaultSlimMode() { return defaultSlimMode; }
    public void setDefaultSlimMode(String defaultSlimMode) { this.defaultSlimMode = defaultSlimMode; }
    public List<SlimModeVO> getSlimModes() { return slimModes; }
    public void setSlimModes(List<SlimModeVO> slimModes) { this.slimModes = slimModes; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class SlimModeVO {
        private String code;
        private String name;
        private String description;
        private Integer crf;
        private String preset;
        private String audioBitrate;
        private Integer maxHeight;
        private Integer minSavedRatioPercent;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getCrf() { return crf; }
        public void setCrf(Integer crf) { this.crf = crf; }
        public String getPreset() { return preset; }
        public void setPreset(String preset) { this.preset = preset; }
        public String getAudioBitrate() { return audioBitrate; }
        public void setAudioBitrate(String audioBitrate) { this.audioBitrate = audioBitrate; }
        public Integer getMaxHeight() { return maxHeight; }
        public void setMaxHeight(Integer maxHeight) { this.maxHeight = maxHeight; }
        public Integer getMinSavedRatioPercent() { return minSavedRatioPercent; }
        public void setMinSavedRatioPercent(Integer minSavedRatioPercent) { this.minSavedRatioPercent = minSavedRatioPercent; }
    }
}
