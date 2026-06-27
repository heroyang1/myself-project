package com.backstage.system.domain.redeem.vo;

import java.util.ArrayList;
import java.util.List;

public class RedeemCodeBatchResultVO {
    private int total;
    private int successCount;
    private int failCount;
    private List<RedeemCodeBatchItemVO> items = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    public List<RedeemCodeBatchItemVO> getItems() { return items; }
    public void setItems(List<RedeemCodeBatchItemVO> items) { this.items = items; }
}
