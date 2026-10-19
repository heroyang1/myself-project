package com.backstage.system.domain.order.enums;

import java.util.Locale;

/**
 * 支付提供方。渠道（微信/支付宝）与提供方（易支付/汇付）是两个不同维度。
 */
public enum PaymentProviderEnum {

    LEGACY("legacy"),
    HUIFU("huifu"),
    INTERNAL("internal");

    private final String value;

    PaymentProviderEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PaymentProviderEnum fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LEGACY;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (PaymentProviderEnum provider : values()) {
            if (provider.value.equals(normalized)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("不支持的支付提供方: " + value);
    }
}
