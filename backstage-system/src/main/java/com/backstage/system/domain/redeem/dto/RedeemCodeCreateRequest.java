package com.backstage.system.domain.redeem.dto;

import java.math.BigDecimal;

public class RedeemCodeCreateRequest {
    private String userEmail;
    private String resourceNo;
    private BigDecimal amount;
    private Long channelId;
    private String remark;

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getResourceNo() { return resourceNo; }
    public void setResourceNo(String resourceNo) { this.resourceNo = resourceNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
