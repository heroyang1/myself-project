package com.backstage.system.task;

import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.service.order.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupOrderTimeoutCancelTaskTest {

    private OshGroupServerMapper groupMapper;
    private OshPaymentMapper paymentMapper;
    private OrderService orderService;
    private GroupOrderTimeoutCancelTask task;

    @Before
    public void setUp() {
        groupMapper = mock(OshGroupServerMapper.class);
        paymentMapper = mock(OshPaymentMapper.class);
        orderService = mock(OrderService.class);
        task = new GroupOrderTimeoutCancelTask();
        ReflectionTestUtils.setField(task, "groupServerMapper", groupMapper);
        ReflectionTestUtils.setField(task, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(task, "orderService", orderService);
    }

    @Test
    public void shouldDirectlyReleaseReservationWhenPaymentWasNeverCreated() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(null);
        when(groupMapper.updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any())).thenReturn(1);

        task.cancelTimeoutOrders();

        verify(groupMapper).updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any());
        verify(groupMapper).updatePendingGroupWorkToCancelled(groupOrder.getGroupWorkId());
        verify(orderService, never()).cancelPayment(any());
    }

    @Test
    public void shouldUseUnifiedQueryAndCloseWhenPaymentExists() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        OshPayment payment = new OshPayment();
        payment.setPaymentNo(groupOrder.getOrderNo());
        payment.setStatus(PaymentStatusEnum.PENDING.getCode());
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(payment);

        task.cancelTimeoutOrders();

        verify(orderService).cancelPayment(payment.getPaymentNo());
        verify(groupMapper, never()).updatePendingGroupOrderToCancelled(any(), any());
    }

    @Test
    public void shouldNeverCancelPaidGroupOrder() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        OshPayment payment = new OshPayment();
        payment.setPaymentNo(groupOrder.getOrderNo());
        payment.setStatus(PaymentStatusEnum.SUCCESS.getCode());
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(payment);

        task.cancelTimeoutOrders();

        verify(orderService, never()).cancelPayment(any());
        verify(groupMapper, never()).updatePendingGroupOrderToCancelled(any(), any());
    }

    private OshGroupOrder pendingGroupOrder() {
        OshGroupOrder order = new OshGroupOrder();
        order.setId(10L);
        order.setUserId(20L);
        order.setOrderNo("GP20260710123456ABCDEF");
        order.setGroupWorkId(30L);
        order.setStatus("pending");
        return order;
    }
}
