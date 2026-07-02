package com.backstage.system.domain.redeem;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("osh_redeem_invite_link")
public class OshRedeemInviteLink extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long redeemCodeId;
    private String redeemCode;
    private String linkToken;
    private String inviteCode;
    private Long inviterId;
    private Integer status;
    private LocalDateTime expireTime;
    private String registerEmail;
    private Long registerUserId;
    private LocalDateTime registerTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRedeemCodeId() { return redeemCodeId; }
    public void setRedeemCodeId(Long redeemCodeId) { this.redeemCodeId = redeemCodeId; }
    public String getRedeemCode() { return redeemCode; }
    public void setRedeemCode(String redeemCode) { this.redeemCode = redeemCode; }
    public String getLinkToken() { return linkToken; }
    public void setLinkToken(String linkToken) { this.linkToken = linkToken; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public String getRegisterEmail() { return registerEmail; }
    public void setRegisterEmail(String registerEmail) { this.registerEmail = registerEmail; }
    public Long getRegisterUserId() { return registerUserId; }
    public void setRegisterUserId(Long registerUserId) { this.registerUserId = registerUserId; }
    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }
}
