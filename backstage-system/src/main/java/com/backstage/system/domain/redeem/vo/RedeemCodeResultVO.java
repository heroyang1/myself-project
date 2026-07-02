package com.backstage.system.domain.redeem.vo;

public class RedeemCodeResultVO {
    private String code;
    private String orderNo;
    private Integer status;

    public RedeemCodeResultVO() {
    }

    public RedeemCodeResultVO(String code, String orderNo, Integer status) {
        this.code = code;
        this.orderNo = orderNo;
        this.status = status;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
