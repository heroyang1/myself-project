package com.backstage.system.enums.internalmember;

public enum InternalMemberFollowTaskStatusEnum {
    PENDING("pending", "待跟进"),
    NOTIFIED("notified", "已提醒"),
    COMPLETED("completed", "已完成"),
    CANCELED("canceled", "已取消"),
    OVERDUE("overdue", "已逾期");

    private final String code;
    private final String label;

    InternalMemberFollowTaskStatusEnum(String code, String label) {
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
        if (code == null) {
            return false;
        }
        for (InternalMemberFollowTaskStatusEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String code) {
        if (code == null) {
            return null;
        }
        for (InternalMemberFollowTaskStatusEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value.code;
            }
        }
        return code.trim();
    }

    public static String getLabelByCode(String code) {
        if (code == null) {
            return null;
        }
        for (InternalMemberFollowTaskStatusEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value.label;
            }
        }
        return code;
    }
}
