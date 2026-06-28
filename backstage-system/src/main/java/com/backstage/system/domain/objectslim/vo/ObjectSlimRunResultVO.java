package com.backstage.system.domain.objectslim.vo;

public class ObjectSlimRunResultVO {
    private Integer scanned = 0;
    private Integer processed = 0;
    private Integer success = 0;
    private Integer failed = 0;
    private Integer skipped = 0;
    private String message;

    public Integer getScanned() { return scanned; }
    public void setScanned(Integer scanned) { this.scanned = scanned; }
    public Integer getProcessed() { return processed; }
    public void setProcessed(Integer processed) { this.processed = processed; }
    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }
    public Integer getFailed() { return failed; }
    public void setFailed(Integer failed) { this.failed = failed; }
    public Integer getSkipped() { return skipped; }
    public void setSkipped(Integer skipped) { this.skipped = skipped; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
