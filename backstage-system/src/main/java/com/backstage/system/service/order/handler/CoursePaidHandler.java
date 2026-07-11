package com.backstage.system.service.order.handler;

import java.math.BigDecimal;
import java.util.Objects;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.course.CoursePurchaseAnnouncementPublisher;
import com.backstage.system.service.order.OrderPaidHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Component
public class CoursePaidHandler implements OrderPaidHandler {

    private static final Logger log = LoggerFactory.getLogger(CoursePaidHandler.class);

    @Resource
    private OshOrderMapper oshOrderMapper;

    @Resource
    private CourseBuyMapper courseBuyMapper;

    @Resource
    private OshPaymentMapper oshPaymentMapper;

    @Resource
    private CoursePurchaseAnnouncementPublisher coursePurchaseAnnouncementPublisher;

    @Override
    public String bizType() {
        return ProductTypeEnum.COURSE.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(String orderNo) {
        OshOrder order = oshOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("课程订单不存在: " + orderNo);
        }
        if (!Objects.equals(order.getProductType(), ProductTypeEnum.COURSE.getCode())) {
            throw new ServiceException("订单商品类型不是课程: " + orderNo);
        }
        if (courseBuyMapper.countPaidByUserAndCourse(order.getUserId(), order.getProductId()) > 0) {
            return;
        }

        BigDecimal payPrice = safeAmount(order.getPayableAmount());
        BigDecimal originPrice = safeAmount(order.getOriginalAmount());
        courseBuyMapper.upsertPendingOrder(
                order.getUserId(),
                order.getProductId(),
                order.getOrderNo(),
                resolvePayMethod(order),
                payPrice,
                originPrice
        );
        courseBuyMapper.markPaidByOrderNoAndUserId(order.getOrderNo(), order.getUserId());
        publishCoursePurchaseNotice(order);
    }

    private String resolvePayMethod(OshOrder order) {
        BigDecimal payPrice = safeAmount(order.getPayableAmount());
        if (payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return PayChannelEnum.FREE.getValue();
        }
        OshPayment payment = oshPaymentMapper.selectByOrderNo(order.getOrderNo());
        if (payment != null && payment.getChannel() != null) {
            PayChannelEnum channel = PayChannelEnum.fromCode(payment.getChannel());
            if (channel != null) {
                return channel.getValue();
            }
        }
        return PayChannelEnum.ALIPAY.getValue();
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void publishCoursePurchaseNotice(OshOrder order) {
        try {
            coursePurchaseAnnouncementPublisher.publishPurchaseSuccess(
                    order.getUserId(), order.getProductId(), order.getOrderNo());
        } catch (Exception e) {
            log.warn("publish course purchase announcement failed, orderNo={}, userId={}, courseId={}",
                    order.getOrderNo(), order.getUserId(), order.getProductId(), e);
        }
    }
}
