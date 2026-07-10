package com.backstage.system.service.order;

import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshOrderEntitlementTask;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderEntitlementTaskMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.behavior.ContributionService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderEntitlementTaskServiceTest {

    private OshOrderEntitlementTaskMapper taskMapper;
    private OshOrderMapper orderMapper;
    private OrderPaidHandlerRegistry handlerRegistry;
    private OrderEntitlementTaskService service;

    @Before
    public void setUp() {
        taskMapper = mock(OshOrderEntitlementTaskMapper.class);
        orderMapper = mock(OshOrderMapper.class);
        handlerRegistry = mock(OrderPaidHandlerRegistry.class);
        ContributionService contributionService = mock(ContributionService.class);
        PaymentSuccessEventPublisher eventPublisher = mock(PaymentSuccessEventPublisher.class);
        service = new OrderEntitlementTaskService();
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "paidHandlerRegistry", handlerRegistry);
        ReflectionTestUtils.setField(service, "contributionService", contributionService);
        ReflectionTestUtils.setField(service, "paymentSuccessEventPublisher", eventPublisher);
    }

    @Test
    public void shouldClaimGrantAndCompleteTask() {
        String orderNo = "O202607100001";
        when(taskMapper.claim(eq(orderNo), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectByOrderNo(orderNo)).thenReturn(task(orderNo, 1));
        when(orderMapper.selectByOrderNo(orderNo)).thenReturn(paidOrder(orderNo));
        when(handlerRegistry.hasHandler(ProductTypeEnum.COURSE.getName())).thenReturn(true);
        when(taskMapper.markSuccess(eq(orderNo), any(LocalDateTime.class))).thenReturn(1);

        service.processOrder(orderNo);

        verify(handlerRegistry).handle(ProductTypeEnum.COURSE.getName(), orderNo);
        verify(taskMapper).markSuccess(eq(orderNo), any(LocalDateTime.class));
    }

    @Test
    public void shouldPersistRetryWhenGrantFails() {
        String orderNo = "O202607100002";
        when(taskMapper.claim(eq(orderNo), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectByOrderNo(orderNo)).thenReturn(task(orderNo, 2));
        when(orderMapper.selectByOrderNo(orderNo)).thenReturn(paidOrder(orderNo));
        when(handlerRegistry.hasHandler(ProductTypeEnum.COURSE.getName())).thenReturn(true);
        doThrow(new IllegalStateException("grant failed"))
                .when(handlerRegistry).handle(ProductTypeEnum.COURSE.getName(), orderNo);

        try {
            service.processOrder(orderNo);
        } catch (IllegalStateException expected) {
            // 任务状态必须已回到待重试；上层安全入口会记录异常但不丢任务。
        }

        verify(taskMapper).markRetry(
                eq(orderNo), any(LocalDateTime.class), eq("grant failed"));
    }

    private OshOrderEntitlementTask task(String orderNo, int attempts) {
        OshOrderEntitlementTask task = new OshOrderEntitlementTask();
        task.setOrderNo(orderNo);
        task.setProductType(ProductTypeEnum.COURSE.getCode());
        task.setAttempts(attempts);
        return task;
    }

    private OshOrder paidOrder(String orderNo) {
        OshOrder order = new OshOrder();
        order.setOrderNo(orderNo);
        order.setProductType(ProductTypeEnum.COURSE.getCode());
        order.setStatus(OrderStatusEnum.PAID.getCode());
        return order;
    }
}
