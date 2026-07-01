package com.backstage.system.enums.internalmember;

public enum InterviewStatusEnum {
    PENDING_INTERVIEW("pending_interview", "待面试"),
    INTERVIEWING("interviewing", "面试中"),
    FINISHED("finished", "已结束");

    private final String code;
    private final String label;

    InterviewStatusEnum(String code, String label) {
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
        for (InterviewStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
