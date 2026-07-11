package com.backstage.system.payment.huifu.dto;

/** 关单结果。 */
public class HuifuCloseResult {

    private String responseCode;
    private String responseMessage;
    private String requestDate;
    private String requestSequenceId;
    private String originalRequestDate;
    private String originalRequestSequenceId;
    private HuifuTransactionStatus originalTransactionStatus;
    private HuifuTransactionStatus closeStatus;

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

    public HuifuTransactionStatus getOriginalTransactionStatus() {
        return originalTransactionStatus;
    }

    public void setOriginalTransactionStatus(HuifuTransactionStatus originalTransactionStatus) {
        this.originalTransactionStatus = originalTransactionStatus;
    }

    public HuifuTransactionStatus getCloseStatus() {
        return closeStatus;
    }

    public void setCloseStatus(HuifuTransactionStatus closeStatus) {
        this.closeStatus = closeStatus;
    }
}
