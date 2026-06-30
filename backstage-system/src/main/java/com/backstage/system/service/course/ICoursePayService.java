package com.backstage.system.service.course;

import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;

/**
 * Course payment service.
 *
 * Standalone payment entry point for the course module. It intentionally does NOT depend on
 * {@link com.backstage.system.service.order.PayService} or {@link com.backstage.system.controller.order.WxPayController},
 * so the existing shared payment files stay untouched while the course module gets a clean,
 * extensible payment flow that supports multiple channels (wxpay / alipay).
 */
public interface ICoursePayService {

    /**
     * Create a payment order for a course and request a QR code from the upstream gateway.
     *
     * @param courseId target course id (price is fetched server-side to avoid client tampering)
     * @param payType  payment channel: "wxpay" or "alipay"
     * @param clientIp end-user IP, required by the gateway
     * @return unified checkout response, with point deduction and payment information
     */
    OrderCheckoutRespVO createCoursePay(Long courseId, String payType, String clientIp, Long userId);

    /**
     * Query the upstream gateway for the current status of a given out_trade_no.
     *
     * @param outTradeNo merchant order number returned by {@link #createCoursePay}
     * @return true when the gateway reports the order as paid
     */
    boolean isCoursePaid(String outTradeNo, Long userId);
}
