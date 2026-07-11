package com.backstage.system.domain.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 已经由具体支付网关完成验签和商户校验的统一通知。
 */
public class VerifiedPaymentNotification {

    private String provider;
    private String paymentNo;
    private String platformTradeNo;
    private BigDecimal amount;
    private Integer channel;
    private Map<String, Object> safePayload;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getPlatformTradeNo() {
        return platformTradeNo;
    }

    public void setPlatformTradeNo(String platformTradeNo) {
        this.platformTradeNo = platformTradeNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Map<String, Object> getSafePayload() {
        return safePayload;
    }

    public void setSafePayload(Map<String, Object> safePayload) {
        this.safePayload = safePayload;
    }
}
