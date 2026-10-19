package com.backstage.system.payment.huifu.dto;

/**
 * 汇付异步通知外层报文。
 * rawResponseData 必须取 HTTP 请求中原始 resp_data 字符串，不能重建 JSON。
 */
public class HuifuNotificationEnvelope {

    private String responseCode;
    private String responseMessage;
    private String signature;
    private String rawResponseData;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getRawResponseData() {
        return rawResponseData;
    }

    public void setRawResponseData(String rawResponseData) {
        this.rawResponseData = rawResponseData;
    }
}
