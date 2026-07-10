package com.backstage.system.payment.huifu.exception;

/** 汇付已返回报文，但业务响应码不是成功/处理中。 */
public class HuifuApiException extends HuifuPayException {

    private final String responseCode;

    public HuifuApiException(String responseCode, String responseMessage) {
        super("汇付请求失败[" + safe(responseCode) + "]: " + safe(responseMessage));
        this.responseCode = responseCode;
    }

    public String getResponseCode() {
        return responseCode;
    }

    private static String safe(String value) {
        if (value == null) {
            return "未知";
        }
        return value.length() <= 256 ? value : value.substring(0, 256);
    }
}
