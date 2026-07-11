package com.backstage.system.domain.servergroup.enums;

/**
 * 拼团订单状态枚举
 * 
 * 适用于 osh_group_order.status
 * 
 * @author system
 * @date 2026-07-09
 */
public enum GroupOrderStatusEnum {

    /** 待支付 */
    PENDING("pending", "待支付"),

    /** 已支付 */
    PAID("paid", "已支付"),

    /** 拼团成功 */
    SUCCESS("success", "拼团成功"),

    /** 已退款 */
    REFUNDED("refunded", "已退款"),

    /** 已取消 */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String desc;

    GroupOrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static GroupOrderStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (GroupOrderStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断订单是否已完成支付（paid 或 success）
     */
    public boolean isPaidOrSuccess() {
        return this == PAID || this == SUCCESS;
    }
}
