package com.backstage.system.service.order;

import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.PlatformPayQueryResult;
import com.backstage.system.domain.payment.VerifiedPaymentNotification;
import com.backstage.system.domain.vo.order.PayResponse;

import java.util.Map;

public interface OrderService {

    /**
     * 统一下单
     * @param reqVO
     * @return
     */
    OrderCheckoutRespVO checkout(OrderCheckoutReqVO reqVO, Boolean enablePointDeduction);

    /** 为已由业务模块创建的统一订单/支付流水发起平台支付。 */
    PayResponse createPaymentForExistingOrder(String orderNo,
                                              String paymentNo,
                                              String clientIp,
                                              String channel);

    /**
     * 查询订单状态
     * @param orderNo
     * @return
     */
    OrderStatusResult getOrderStatus(String orderNo);

    /**
     * 查询当前用户自己的订单状态
     * @param orderNo
     * @param userId
     * @return
     */
    OrderStatusResult getOrderStatusForUser(String orderNo, Long userId);

    /**
     * 查询支付状态
     * @param paymentNo
     * @return
     */
    OrderStatusResult getPaymentStatus(String paymentNo);

    /**
     * 处理支付回调
     * @param params
     * @return
     */
    boolean handlePayNotify(Map<String, String> params);

    /** 处理已由网关适配层验签的支付成功通知。 */
    boolean handleVerifiedPayNotify(VerifiedPaymentNotification notification);

    /** 处理已由网关适配层验签的支付失败终态通知。 */
    boolean handleVerifiedPayFailure(VerifiedPaymentNotification notification);

    /** 主动查平台并在已支付时幂等补单。 */
    PlatformPayQueryResult reconcilePaymentFromPlatform(String paymentNo);

    /** 已经查出的支付流水版本，供关单任务复用。 */
    PlatformPayQueryResult reconcilePaymentFromPlatform(OshPayment payment);

    /**
     * 取消支付
     * @param paymentNo
     */
    void cancelPayment(String paymentNo);

    /**
     * 按订单号取消支付
     * @param orderNo
     */
    void cancelPaymentByOrderNo(String orderNo);

    /**
     * 按订单号取消当前用户自己的支付
     * @param orderNo
     * @param userId
     */
    void cancelPaymentByOrderNoForUser(String orderNo, Long userId);
}
