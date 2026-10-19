package com.backstage.system.domain.servergroup.enums;

/**
 * 拼团组团状态枚举
 * 
 * 适用于 osh_group_work.group_status 和 osh_group_user_initiated.group_status
 * 
 * @author system
 * @date 2026-07-09
 */
public enum GroupStatusEnum {

    /** 待支付 / 招募中 */
    PENDING(0, "待支付"),

    /** 已支付（待成团） */
    PAID(1, "已支付"),

    /** 已成团 */
    FORMED(2, "已成团"),

    /** 已结束 / 已取消 */
    ENDED(3, "已结束");

    private final int code;
    private final String desc;

    GroupStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 获取面向C端展示的状态文字（系统活动视角）
     */
    public String getDisplayText() {
        switch (this) {
            case PENDING: return "进行中";
            case PAID:    return "已支付";
            case FORMED:  return "已成团";
            case ENDED:   return "已取消/过期";
            default:      return "未知";
        }
    }

    /**
     * 获取面向C端展示的状态文字（用户发起拼团视角）
     */
    public String getInitiatedDisplayText() {
        switch (this) {
            case PENDING: return "招募中";
            case PAID:    return "已成团";
            case FORMED:  return "已成团";
            case ENDED:   return "已取消/过期";
            default:      return "未知";
        }
    }

    /**
     * 用户发起拼团 group_status → C端展示 status 映射
     * 0(招募中) → 1(进行中), 1(已成团) → 2(拼团成功), 2(已结束) → 3(已结束)
     */
    public int toInitiatedFrontStatus() {
        switch (this) {
            case PENDING: return 1; // 进行中
            case PAID:    return 2; // 拼团成功
            case FORMED:  return 2; // 拼团成功
            case ENDED:   return 3; // 已结束
            default:      return 1;
        }
    }

    public static GroupStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GroupStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
