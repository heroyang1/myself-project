package com.backstage.system.payment.huifu.dto;

import java.math.BigDecimal;

/** 扫码交易主动查单参数。 */
public class HuifuQueryPaymentRequest {

    private String requestDate;
    private String requestSequenceId;
    private String originalRequestDate;
    private String originalRequestSequenceId;
    private String partyOrderId;
    private BigDecimal expectedAmount;

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

    public String getPartyOrderId() {
        return partyOrderId;
    }

    public void setPartyOrderId(String partyOrderId) {
        this.partyOrderId = partyOrderId;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }
}
