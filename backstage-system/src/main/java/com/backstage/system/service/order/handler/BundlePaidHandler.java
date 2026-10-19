package com.backstage.system.service.order.handler;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.service.bundle.ProductBundleService;
import com.backstage.system.service.order.OrderPaidHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 套餐支付成功后主动发放套餐内课程/电子书权益。
 */
@Component
public class BundlePaidHandler implements OrderPaidHandler {

    @Lazy
    @Resource
    private ProductBundleService productBundleService;

    @Override
    public String bizType() {
        return ProductTypeEnum.BUNDLE.getName();
    }

    @Override
    public void handle(String orderNo) {
        productBundleService.grantBundleAccess(orderNo);
    }
}
