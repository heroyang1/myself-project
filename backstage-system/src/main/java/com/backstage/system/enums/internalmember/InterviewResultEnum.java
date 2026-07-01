package com.backstage.system.enums.internalmember;

public enum InterviewResultEnum {
    PENDING("pending", "待定"),
    PASSED("passed", "通过"),
    FAILED("failed", "未通过"),
    OFFER_SENT("offer_sent", "已发 Offer"),
    ONBOARDED("onboarded", "已入职");

    private final String code;
    private final String label;

    InterviewResultEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean contains(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        for (InterviewResultEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
