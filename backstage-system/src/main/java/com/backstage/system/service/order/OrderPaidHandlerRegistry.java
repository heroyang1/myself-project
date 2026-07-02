package com.backstage.system.service.order;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderPaidHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidHandlerRegistry.class);

    private final Map<String, OrderPaidHandler> handlerMap;

    public OrderPaidHandlerRegistry(List<OrderPaidHandler> handlers) {
        this.handlerMap = new HashMap<>();
        for (OrderPaidHandler handler : handlers) {
            String bizType = handler.bizType();
            try {
                ProductTypeEnum.fromName(bizType);
            } catch (IllegalArgumentException e) {
                throw new ServiceException("支付后置处理器业务类型非法, bizType=" + bizType);
            }
            handlerMap.put(bizType, handler);
            log.info("register paid handler success, bizType={}, handler={}",
                    bizType, handler.getClass().getSimpleName());
        }
    }

    public boolean hasHandler(String bizType) {
        return handlerMap.containsKey(bizType);
    }

    public OrderPaidHandler getHandler(String bizType) {
        OrderPaidHandler handler = handlerMap.get(bizType);
        if (handler == null) {
            throw new ServiceException("未找到支付后置处理器, bizType=" + bizType);
        }
        return handler;
    }

    public void handle(String bizType, String orderNo) {
        log.info("handle paid business start, bizType={}, orderNo={}", bizType, orderNo);
        getHandler(bizType).handle(orderNo);
    }
}
