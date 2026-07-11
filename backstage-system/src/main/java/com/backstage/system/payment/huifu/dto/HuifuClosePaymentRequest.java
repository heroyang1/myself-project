package com.backstage.system.payment.huifu.dto;

/** 扫码交易关单参数。 */
public class HuifuClosePaymentRequest {

    private String requestDate;
    private String requestSequenceId;
    private String originalRequestDate;
    private String originalRequestSequenceId;

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getRequestSequenceId() {
        return requestSequenceId;
    }

    public void setRequestSequenceId(String requestSequenceId) {
        this.requestSequenceId = requestSequenceId;
    }

    public String getOriginalRequestDate() {
        return originalRequestDate;
    }

    public void setOriginalRequestDate(String originalRequestDate) {
        this.originalRequestDate = originalRequestDate;
    }

    public String getOriginalRequestSequenceId() {
        return originalRequestSequenceId;
    }

    public void setOriginalRequestSequenceId(String originalRequestSequenceId) {
        this.originalRequestSequenceId = originalRequestSequenceId;
    }

}
