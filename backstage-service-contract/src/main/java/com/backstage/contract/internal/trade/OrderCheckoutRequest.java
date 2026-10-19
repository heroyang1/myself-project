package com.backstage.contract.internal.trade;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 内部订单结算请求
 *
 * <p>供 tool-service 等内部服务发起结算，映射到单体 {@code OrderCheckoutReqVO}。
 * 仅承载下单必需字段，不含数据库审计字段。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class OrderCheckoutRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 下单用户 ID（由内部调用透传，非空）
     */
    private Long userId;

    /**
     * 商品类型：与单体 ProductTypeEnum 保持一致（工具=TOOL 对应值）
     */
    private Integer productType;

    /**
     * 商品 ID（工具套餐 ID）
     */
    private Long productId;

    /**
     * 商品名称（用于订单展示）
     */
    private String productName;

    /**
     * 购买模式：与单体 PurchaseModeEnum 一致（普通购买=NORMAL 对应值），可空由交易域取默认
     */
    private Integer purchaseMode;

    /**
     * 原价
     */
    private BigDecimal originalAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 优惠券 ID（可空）
     */
    private Long couponId;

    /**
     * 支付渠道字符串标识，如 wxpay/alipay/bank
     */
    private String channel;

    /**
     * 客户端 IP（由调用方透传）
     */
    private String clientIp;

    /**
     * 是否使用积分抵扣：true-使用 false-不使用
     */
    private Boolean usePoints;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getProductType() {
        return productType;
    }

    public void setProductType(Integer productType) {
        this.productType = productType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getPurchaseMode() {
        return purchaseMode;
    }

    public void setPurchaseMode(Integer purchaseMode) {
        this.purchaseMode = purchaseMode;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Boolean getUsePoints() {
        return usePoints;
    }

    public void setUsePoints(Boolean usePoints) {
        this.usePoints = usePoints;
    }
}
