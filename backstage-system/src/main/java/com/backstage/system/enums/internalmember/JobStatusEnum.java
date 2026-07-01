package com.backstage.system.enums.internalmember;

public enum JobStatusEnum {
    UNEMPLOYED(0, "未就业"),
    LOOKING(1, "求职中"),
    EMPLOYED(2, "已就业");

    private final Integer code;
    private final String label;

    JobStatusEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public Integer getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static boolean contains(Integer code) {
        if (code == null) {
            return false;
        }
        for (JobStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static String getLabelByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item.label;
            }
        }
        return null;
    }
}
