package com.backstage.system.mapper.order;

import com.backstage.system.domain.order.OshPayment;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;

public interface OshPaymentMapper {

    int insertOshPayment(OshPayment payment);

    OshPayment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    OshPayment selectByOrderNo(@Param("orderNo") String orderNo);

    java.util.List<OshPayment> selectExpiredPendingPayments(@Param("expireTime") LocalDateTime expireTime);

    int updatePayResponse(@Param("paymentNo") String paymentNo,
                          @Param("platformTradeNo") String platformTradeNo,
                          @Param("payUrl") String payUrl,
                          @Param("qrcode") String qrcode,
                          @Param("responsePayload") String responsePayload);

    int updatePendingToSuccess(@Param("paymentNo") String paymentNo,
                               @Param("platformTradeNo") String platformTradeNo,
                               @Param("paidTime") LocalDateTime paidTime,
                               @Param("channel") Integer channel);

    int updatePendingToFailed(@Param("paymentNo") String paymentNo,
                              @Param("responsePayload") String responsePayload);

    int updatePendingToClosed(@Param("paymentNo") String paymentNo);
}
