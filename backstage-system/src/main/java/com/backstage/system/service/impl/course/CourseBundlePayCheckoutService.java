package com.backstage.system.service.impl.course;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.order.enums.PurchaseModeEnum;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.service.order.OrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 课程/套餐结算适配器。
 *
 * <p>课程和套餐不再维护独立的 RSA 支付链路，统一进入 {@link OrderService}，
 * 与电子书、会员、工具共用同一套支付提供方、查单、回调和幂等逻辑。</p>
 */
@Service
public class CourseBundlePayCheckoutService {

    @Resource
    private OrderService orderService;

    public OrderCheckoutRespVO checkout(Long userId,
                                        ProductTypeEnum productType,
                                        Long productId,
                                        String productName,
                                        BigDecimal originalAmount,
                                        BigDecimal payableAmount,
                                        String channel,
                                        String clientIp) {
        OrderCheckoutReqVO request = new OrderCheckoutReqVO();
        request.setUserId(userId);
        request.setProductType(productType.getCode());
        request.setProductId(productId);
        request.setProductName(productName);
        request.setPurchaseMode(PurchaseModeEnum.NORMAL.getCode());
        request.setOriginalAmount(originalAmount);
        request.setDiscountAmount(calculateDiscount(originalAmount, payableAmount));
        request.setPayableAmount(payableAmount);
        request.setChannel(channel);
        request.setUsePoints(Boolean.FALSE);
        request.setClientIp(clientIp);
        return orderService.checkout(request, Boolean.FALSE);
    }

    private BigDecimal calculateDiscount(BigDecimal originalAmount, BigDecimal payableAmount) {
        if (originalAmount == null || payableAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = originalAmount.subtract(payableAmount);
        return discount.signum() > 0 ? discount : BigDecimal.ZERO;
    }
}
