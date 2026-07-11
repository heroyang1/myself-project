package com.backstage.system.service.order;

import com.backstage.system.domain.vo.order.PayResponse;

import java.util.Map;
import java.math.BigDecimal;

public interface PayService {

    /** 当前新订单使用的支付提供方。 */
    String currentProvider();

    /**
     * 向支付平台发起支付请求。
     *
     * @param outTradeNo 支付流水号
     * @param name 商品名称
     * @param money 支付金额
     * @param clientIp 客户端IP
     * @param channel 支付渠道标识（wxpay/alipay/bank等）
     * @return 支付平台响应
     */
    PayResponse createPay(String outTradeNo, String name, String money, String clientIp, String channel);

    /**
     * 按支付流水已保存的 provider 发起支付，供仍未迁入统一下单的历史业务链路使用。
     */
    PayResponse createPay(String provider,
                          String outTradeNo,
                          String name,
                          String money,
                          String clientIp,
                          String channel);

    /**
     * 主动查询支付平台订单状态。
     *
     * @param outTradeNo 支付流水号
     * @return 支付平台返回的订单信息，查询失败返回 null
     */
    Map<String, String> queryPay(String outTradeNo, String provider, BigDecimal expectedAmount);

    /**
     * 关闭指定支付平台上的待支付订单。
     *
     * @return true 表示平台确认关闭或订单已经处于不可支付终态
     */
    boolean closePay(String outTradeNo, String provider);
}
