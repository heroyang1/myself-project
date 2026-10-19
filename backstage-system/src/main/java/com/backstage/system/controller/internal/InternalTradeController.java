package com.backstage.system.controller.internal;

import com.backstage.contract.common.InternalApiConstants;
import com.backstage.contract.common.InternalResult;
import com.backstage.contract.internal.trade.OrderCancelRequest;
import com.backstage.contract.internal.trade.OrderCheckoutRequest;
import com.backstage.contract.internal.trade.OrderCheckoutResponse;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部交易（订单）接口
 *
 * <p>trade 域对其它服务暴露的结算与关单能力，仅供服务间内网调用。tool-service 点数购买 SAGA
 * 通过本端点创建订单/发起支付，并在补偿时关单。委托单体现有 OrderCheckoutService / OrderService，
 * 不改动其内部逻辑。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@RestController
@RequestMapping(InternalApiConstants.TRADE_BASE)
public class InternalTradeController {

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Autowired
    private OrderService orderService;

    /**
     * 订单结算（创建订单并发起支付）
     *
     * @param request 结算请求
     * @return 结算结果（订单号、支付流水号、是否需支付等）
     */
    @PostMapping("/checkout")
    public InternalResult<OrderCheckoutResponse> checkout(@RequestBody OrderCheckoutRequest request) {
        OrderCheckoutRespVO respVO = orderCheckoutService.checkout(toReqVO(request));
        return InternalResult.ok(toResponse(respVO));
    }

    /**
     * 按订单号关单（用于取消/补偿）
     *
     * @param request 关单请求
     * @return 空结果，成功即代表关单指令已受理
     */
    @PostMapping("/cancel")
    public InternalResult<Void> cancel(@RequestBody OrderCancelRequest request) {
        orderService.cancelPaymentByOrderNo(request.getOrderNo());
        return InternalResult.<Void>ok(null);
    }

    /**
     * 契约请求映射为单体结算入参
     *
     * @param request 契约请求
     * @return 单体结算入参
     */
    private OrderCheckoutReqVO toReqVO(OrderCheckoutRequest request) {
        OrderCheckoutReqVO reqVO = new OrderCheckoutReqVO();
        reqVO.setUserId(request.getUserId());
        reqVO.setProductType(request.getProductType());
        reqVO.setProductId(request.getProductId());
        reqVO.setProductName(request.getProductName());
        reqVO.setPurchaseMode(request.getPurchaseMode());
        reqVO.setOriginalAmount(request.getOriginalAmount());
        reqVO.setDiscountAmount(request.getDiscountAmount());
        reqVO.setPayableAmount(request.getPayableAmount());
        reqVO.setCouponId(request.getCouponId());
        reqVO.setChannel(request.getChannel());
        reqVO.setClientIp(request.getClientIp());
        // 【保留默认语义】调用方未显式指定积分抵扣标识时，沿用 OrderCheckoutReqVO 的默认值，不覆盖为 null
        if (request.getUsePoints() != null) {
            reqVO.setUsePoints(request.getUsePoints());
        }
        return reqVO;
    }

    /**
     * 单体结算结果映射为契约响应（扁平化，不含嵌套支付对象）
     *
     * @param respVO 单体结算结果
     * @return 契约响应
     */
    private OrderCheckoutResponse toResponse(OrderCheckoutRespVO respVO) {
        OrderCheckoutResponse response = new OrderCheckoutResponse();
        if (respVO == null) {
            return response;
        }
        response.setNeedPay(respVO.isNeedPay());
        response.setOrderNo(respVO.getOrderNo());
        response.setPaymentNo(respVO.getPaymentNo());
        response.setPayStatus(respVO.getPayStatus());
        response.setPrice(respVO.getPrice());
        response.setPointsUsed(respVO.getPointsUsed());
        response.setPointsDeductAmount(respVO.getPointsDeductAmount());
        response.setRemainingPoints(respVO.getRemainingPoints());
        response.setExpireTime(respVO.getExpireTime());
        response.setCloseExpireMinutes(respVO.getCloseExpireMinutes());
        return response;
    }
}
