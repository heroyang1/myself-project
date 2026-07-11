package com.backstage.system.service.order.handler;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.service.servergroup.IOshGroupServerService;
import com.backstage.system.service.order.OrderPaidHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 服务器拼团支付成功处理器。
 * 实现 OrderPaidHandler 接口，用于处理拼团订单支付成功后的业务逻辑。
 * 
 * 该处理器会被 PaymentSuccessConsumer 调用，根据订单的 productType 分发到对应的处理器。
 * 
 * @author system
 * @date 2026-05-19
 */
@Component
public class GroupPaidHandler implements OrderPaidHandler {

    private static final Logger logger = LoggerFactory.getLogger(GroupPaidHandler.class);

    @Lazy
    @Resource
    private IOshGroupServerService groupServerService;

    /**
     * 获取拼团业务类型标识。
     *
     * @return 商品类型标识
     */
    @Override
    public String bizType() {
        return ProductTypeEnum.GROUP.getName();
    }

    /**
     * 处理拼团订单支付成功后的业务逻辑。
     * 包括：
     * 1. 更新订单状态为已支付
     * 2. 更新参团记录状态
     * 3. 判断并更新拼团状态（成团/结束）
     * 4. 设置服务器时间（成团时）
     *
     * @param orderNo 业务订单号
     */
    @Override
    public void handle(String orderNo) {
        logger.info("【GroupPaidHandler】处理拼团订单支付成功，订单号: {}", orderNo);
        
        try {
            boolean success = groupServerService.handlePaymentSuccess(orderNo);
            if (success) {
                logger.info("【GroupPaidHandler】拼团订单处理成功，订单号: {}", orderNo);
            } else {
                logger.error("【GroupPaidHandler】拼团订单处理失败，订单号: {}", orderNo);
                throw new IllegalStateException("拼团权益处理返回失败: " + orderNo);
            }
        } catch (Exception e) {
            logger.error("【GroupPaidHandler】拼团订单处理异常，订单号: {}", orderNo, e);
            throw e;
        }
    }
}
