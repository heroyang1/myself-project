package com.backstage.system.enums.internalmember;

public enum InternalMemberProgressStageEnum {
    SEPARATED("separated", "待求职/离职", 1),
    PREPARING("preparing", "求职准备", 2),
    DELIVERING("delivering", "投递中", 3),
    INTERVIEWING("interviewing", "面试中", 4),
    OFFER_PENDING("offer_pending", "Offer/待入职", 5),
    ONBOARDED("onboarded", "已入职", 6);

    private final String code;
    private final String name;
    private final int order;

    InternalMemberProgressStageEnum(String code, String name, int order) {
        this.code = code;
        this.name = name;
        this.order = order;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public int getTotal() {
        return values().length;
    }
}
