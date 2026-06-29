package com.backstage.system.domain.redeem.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RedeemInviteLinkVO {
    private Long id;
    private Long redeemCodeId;
    private String linkToken;
    private String code;
    private String inviteCode;
    private Long inviterId;
    private String resourceType;
    private String productName;
    private BigDecimal amount;
    private String channelName;
    private Integer linkStatus;
    private Integer redeemStatus;
    private LocalDateTime expireTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRedeemCodeId() { return redeemCodeId; }
    public void setRedeemCodeId(Long redeemCodeId) { this.redeemCodeId = redeemCodeId; }
    public String getLinkToken() { return linkToken; }
    public void setLinkToken(String linkToken) { this.linkToken = linkToken; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public Integer getLinkStatus() { return linkStatus; }
    public void setLinkStatus(Integer linkStatus) { this.linkStatus = linkStatus; }
    public Integer getRedeemStatus() { return redeemStatus; }
    public void setRedeemStatus(Integer redeemStatus) { this.redeemStatus = redeemStatus; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
