package com.backstage.system.domain.order.enums;

import java.util.Objects;

/**
 * 支付渠道枚举
 */
public enum PayChannelEnum {

    WXPAY(1, "wxpay", "微信支付"),
    ALIPAY(2, "alipay", "支付宝"),

    FREE(5, "free", "免费"),

    OFFLINE(6, "offline", "线下核销");

    private final int code;
    private final String value;
    private final String desc;

    PayChannelEnum(int code, String value, String desc) {
        this.code = code;
        this.value = value;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    /** 传给支付平台的渠道标识 */
    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据前端传入的渠道字符串解析枚举
     */
    public static PayChannelEnum fromValue(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        for (PayChannelEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据code获取枚举
     * @param code
     * @return
     */
    public static PayChannelEnum fromCode(int code) {
        for (PayChannelEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
