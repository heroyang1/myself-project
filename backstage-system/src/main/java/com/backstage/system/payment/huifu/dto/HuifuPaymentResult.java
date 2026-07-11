package com.backstage.system.payment.huifu.dto;

import java.math.BigDecimal;

/** 下单、主动查单、异步通知共用的交易结果。 */
public class HuifuPaymentResult {

    private String responseCode;
    private String responseMessage;
    private String requestDate;
    private String requestSequenceId;
    private String huifuSequenceId;
    private String preOrderId;
    private String orderStatus;
    private String tradeType;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private HuifuTransactionStatus transactionStatus;
    private HuifuTransactionStatus closeStatus;
    private String jumpUrl;
    private String huifuId;
    private String partyOrderId;
    private String outsideTransactionId;
    private String bankMessage;
    private String remark;
    private String endTime;

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

    public String getHuifuSequenceId() {
        return huifuSequenceId;
    }

    public void setHuifuSequenceId(String huifuSequenceId) {
        this.huifuSequenceId = huifuSequenceId;
    }

    public String getPreOrderId() {
        return preOrderId;
    }

    public void setPreOrderId(String preOrderId) {
        this.preOrderId = preOrderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public HuifuTransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(HuifuTransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public HuifuTransactionStatus getCloseStatus() {
        return closeStatus;
    }

    public void setCloseStatus(HuifuTransactionStatus closeStatus) {
        this.closeStatus = closeStatus;
    }

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public String getHuifuId() {
        return huifuId;
    }

    public void setHuifuId(String huifuId) {
        this.huifuId = huifuId;
    }

    public String getPartyOrderId() {
        return partyOrderId;
    }

    public void setPartyOrderId(String partyOrderId) {
        this.partyOrderId = partyOrderId;
    }

    public String getOutsideTransactionId() {
        return outsideTransactionId;
    }

    public void setOutsideTransactionId(String outsideTransactionId) {
        this.outsideTransactionId = outsideTransactionId;
    }

    public String getBankMessage() {
        return bankMessage;
    }

    public void setBankMessage(String bankMessage) {
        this.bankMessage = bankMessage;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /** 主动查单只有两个状态同时满足，才允许业务侧判为支付成功。 */
    public boolean isQueryPaidSuccessfully() {
        return HuifuTransactionStatus.SUCCESS == transactionStatus
                && (orderStatus == null || orderStatus.trim().isEmpty() || "1".equals(orderStatus));
    }

    /** 异步通知处理成功并提交本地事务后，控制器应返回该 ACK。 */
    public String buildAcknowledgement() {
        if (requestSequenceId == null || requestSequenceId.trim().isEmpty()) {
            throw new IllegalStateException("缺少汇付请求流水号，无法生成异步应答");
        }
        return "RECV_ORD_ID_" + requestSequenceId;
    }
}
