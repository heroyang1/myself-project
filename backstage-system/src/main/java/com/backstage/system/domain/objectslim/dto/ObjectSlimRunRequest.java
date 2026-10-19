package com.backstage.system.domain.objectslim.dto;

import java.util.List;

public class ObjectSlimRunRequest {
    private List<Long> ids;
    private Integer limit = 5;
    private Boolean retryFailed = false;
    private Boolean dryRun = false;
    private String slimMode;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Boolean getRetryFailed() { return retryFailed; }
    public void setRetryFailed(Boolean retryFailed) { this.retryFailed = retryFailed; }
    public Boolean getDryRun() { return dryRun; }
    public void setDryRun(Boolean dryRun) { this.dryRun = dryRun; }
    public String getSlimMode() { return slimMode; }
    public void setSlimMode(String slimMode) { this.slimMode = slimMode; }
}
