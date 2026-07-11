package com.backstage.system.enums.internalmember;

public enum InternalMemberStageEnum {
    NOT_STARTED("not_started", "未开始", 0),
    LEARNING("learning", "学习中", 1),
    EXAM("exam", "考试阶段", 2),
    INTERVIEW("interview", "面试阶段", 3),
    EMPLOYED("employed", "已就业", 4);

    private final String code;
    private final String name;
    private final int index;

    InternalMemberStageEnum(String code, String name, int index) {
        this.code = code;
        this.name = name;
        this.index = index;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }
}
