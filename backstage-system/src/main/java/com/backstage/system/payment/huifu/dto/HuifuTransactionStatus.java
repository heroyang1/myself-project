package com.backstage.system.payment.huifu.dto;

/** 汇付交易状态，业务判断必须以该字段为准。 */
public enum HuifuTransactionStatus {
    PENDING("P"),
    SUCCESS("S"),
    FAILED("F"),
    /** 主动查单极少数情况下返回，必须继续查单/人工确认，绝不能视为成功。 */
    INITIAL("I");

    private final String code;

    HuifuTransactionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HuifuTransactionStatus fromCode(String code) {
        for (HuifuTransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("非法的汇付交易状态");
    }
}
