package com.backstage.system.payment.huifu.service;

import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.PaymentProviderEnum;
import com.backstage.system.domain.payment.VerifiedPaymentNotification;
import com.backstage.system.payment.huifu.client.HuifuPayClient;
import com.backstage.system.payment.huifu.dto.HuifuNotificationEnvelope;
import com.backstage.system.payment.huifu.dto.HuifuPaymentResult;
import com.backstage.system.payment.huifu.dto.HuifuTransactionStatus;
import com.backstage.system.payment.huifu.exception.HuifuProtocolException;
import com.backstage.system.service.order.OrderService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇付异步通知适配层：原文验签、商户/状态校验后才进入统一订单事务。
 */
@Service
public class HuifuPayNotifyService {

    private final HuifuPayClient huifuPayClient;
    private final OrderService orderService;

    public HuifuPayNotifyService(HuifuPayClient huifuPayClient, OrderService orderService) {
        this.huifuPayClient = huifuPayClient;
        this.orderService = orderService;
    }

    public String handle(String responseCode,
                         String responseMessage,
                         String signature,
                         String rawResponseData) {
        HuifuNotificationEnvelope envelope = new HuifuNotificationEnvelope();
        envelope.setResponseCode(responseCode);
        envelope.setResponseMessage(responseMessage);
        envelope.setSignature(signature);
        envelope.setRawResponseData(rawResponseData);

        HuifuPaymentResult result = huifuPayClient.verifyNotification(envelope);
        String acknowledgement = result.buildAcknowledgement();

        VerifiedPaymentNotification notification = toNotification(result);

        // 失败终态也必须推进本地失败/关闭并退回积分，事务提交后才能 ACK。
        if (result.getTransactionStatus() == HuifuTransactionStatus.FAILED) {
            if (!orderService.handleVerifiedPayFailure(notification)) {
                throw new IllegalStateException("本地支付失败终态处理失败");
            }
            return acknowledgement;
        }
        if (result.getTransactionStatus() != HuifuTransactionStatus.SUCCESS) {
            throw new HuifuProtocolException("汇付支付通知不是成功终态");
        }
        if (result.getOrderStatus() != null && !"1".equals(result.getOrderStatus())) {
            throw new HuifuProtocolException("汇付订单状态与支付成功状态不一致");
        }
        if (result.getAmount() == null) {
            throw new HuifuProtocolException("汇付支付通知缺少交易金额");
        }
        if (result.getHuifuSequenceId() == null || result.getHuifuSequenceId().trim().isEmpty()) {
            throw new HuifuProtocolException("汇付支付通知缺少平台交易号");
        }

        if (!orderService.handleVerifiedPayNotify(notification)) {
            throw new IllegalStateException("本地支付订单处理失败");
        }
        return acknowledgement;
    }

    private VerifiedPaymentNotification toNotification(HuifuPaymentResult result) {
        VerifiedPaymentNotification notification = new VerifiedPaymentNotification();
        notification.setProvider(PaymentProviderEnum.HUIFU.getValue());
        notification.setPaymentNo(result.getRequestSequenceId());
        notification.setPlatformTradeNo(result.getHuifuSequenceId());
        notification.setAmount(result.getAmount());
        notification.setChannel(resolveChannel(result.getTradeType()));
        notification.setSafePayload(safePayload(result));
        return notification;
    }

    private Integer resolveChannel(String tradeType) {
        if (tradeType == null) {
            return null;
        }
        if (tradeType.startsWith("A_")) {
            return PayChannelEnum.ALIPAY.getCode();
        }
        if (tradeType.startsWith("T_")) {
            return PayChannelEnum.WXPAY.getCode();
        }
        return null;
    }

    private Map<String, Object> safePayload(HuifuPaymentResult result) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("provider", PaymentProviderEnum.HUIFU.getValue());
        payload.put("req_date", result.getRequestDate());
        payload.put("req_seq_id", result.getRequestSequenceId());
        payload.put("hf_seq_id", result.getHuifuSequenceId());
        payload.put("order_stat", result.getOrderStatus());
        payload.put("trans_stat", result.getTransactionStatus() == null
                ? null : result.getTransactionStatus().getCode());
        payload.put("trans_type", result.getTradeType());
        payload.put("trans_amt", result.getAmount());
        return payload;
    }
}
