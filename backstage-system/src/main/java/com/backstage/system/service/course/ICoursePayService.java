package com.backstage.system.service.course;

import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;

/**
 * Course payment service.
 *
 * Course payment entry point backed by the unified order checkout flow.
 */
public interface ICoursePayService {

    /**
     * Create a unified order for a course and request a QR code from the upstream gateway.
     *
     * @param courseId target course id (price is fetched server-side to avoid client tampering)
     * @param payType  payment channel; current checkout is unified to "alipay"
     * @param clientIp end-user IP, required by the gateway
     * @return unified checkout response holding orderNo / paymentNo / qrcode / payurl
     */
    OrderCheckoutRespVO createCoursePay(Long courseId, String payType, String clientIp, Long userId);

    /**
     * Query the unified order status for a given orderNo.
     *
     * @param orderNo unified business order number returned by {@link #createCoursePay}
     * @return true when the gateway reports the order as paid
     */
    boolean isCoursePaid(String orderNo, Long userId);
}
