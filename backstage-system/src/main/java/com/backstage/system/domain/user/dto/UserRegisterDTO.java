package com.backstage.system.domain.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/7
 * Time: 17:19
 */
@ApiModel(value = "账号注册参数", description = "前端传递的账号注册请求实体类")
public class UserRegisterDTO {
    @ApiModelProperty(
            value = "用户名",
            required = true
    )
    private String username;
    @ApiModelProperty(
            value = "用户密码",
            example = "admin123",
            required = true
    )
    private String password;
    @ApiModelProperty(
            value = "确认密码",
            example = "admin123",
            required = true
    )
    private String repassword;
    @ApiModelProperty(
            value = "邮箱",
            required = true
    )
    private String email;

    @ApiModelProperty(
            value = "邀请码（选填）",
            required = false
    )
    private String inviteCode;

    @ApiModelProperty(
            value = "核销邀请链接Token（选填）",
            required = false
    )
    private String redeemInviteToken;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepassword() {
        return repassword;
    }

    public void setRepassword(String repassword) {
        this.repassword = repassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getRedeemInviteToken() {
        return redeemInviteToken;
    }

    public void setRedeemInviteToken(String redeemInviteToken) {
        this.redeemInviteToken = redeemInviteToken;
    }

    @Override
    public String toString() {
        return "UserRegisterDTO{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", repassword='" + repassword + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
