package com.backstage.system.task;

import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.service.servergroup.event.GroupEventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupOrderTimeoutCancelTaskTest {

    private OshGroupServerMapper groupMapper;
    private OshPaymentMapper paymentMapper;
    private OrderService orderService;
    private GroupEventPublisher groupEventPublisher;
    private GroupOrderTimeoutCancelTask task;

    @Before
    public void setUp() {
        groupMapper = mock(OshGroupServerMapper.class);
        paymentMapper = mock(OshPaymentMapper.class);
        orderService = mock(OrderService.class);
        groupEventPublisher = mock(GroupEventPublisher.class);
        task = new GroupOrderTimeoutCancelTask();
        ReflectionTestUtils.setField(task, "groupServerMapper", groupMapper);
        ReflectionTestUtils.setField(task, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(task, "orderService", orderService);
        ReflectionTestUtils.setField(task, "groupEventPublisher", groupEventPublisher);
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

    // ==================== P1-2: 取消事件发布验证 ====================

    @Test
    public void shouldPublishCancelEventWhenOrderCancelledWithoutPayment() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        groupOrder.setGroupActivityId(100L);
        groupOrder.setSourceType("system");
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(null);
        when(groupMapper.updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any())).thenReturn(1);

        task.cancelTimeoutOrders();

        // 验证发布了 ORDER_CANCELLED 事件
        verify(groupEventPublisher).publishOrderCancelled(
                eq("GP20260710123456ABCDEF"),
                eq(100L),
                eq(20L),
                eq("system"));
    }

    @Test
    public void shouldPublishCancelEventWhenCancelledByOrderNo() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        groupOrder.setGroupActivityId(200L);
        groupOrder.setSourceType("initiated");
        when(groupMapper.selectGroupOrderByOrderNo("GP20260710123456ABCDEF")).thenReturn(groupOrder);
        when(groupMapper.updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any())).thenReturn(1);

        task.cancelPendingOrderByOrderNo("GP20260710123456ABCDEF");

        verify(groupEventPublisher).publishOrderCancelled(
                eq("GP20260710123456ABCDEF"),
                eq(200L),
                eq(20L),
                eq("initiated"));
    }

    @Test
    public void shouldNotBreakCancelFlowWhenEventPublishFails() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        groupOrder.setGroupActivityId(100L);
        groupOrder.setSourceType("system");
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(null);
        when(groupMapper.updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any())).thenReturn(1);
        // 模拟 Kafka 事件发布失败
        doThrow(new RuntimeException("Kafka broker unavailable"))
                .when(groupEventPublisher).publishOrderCancelled(anyString(), anyLong(), anyLong(), anyString());

        // 不应抛出异常，取消流程应正常完成
        task.cancelTimeoutOrders();

        // 订单状态仍然被更新
        verify(groupMapper).updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any());
    }

    @Test
    public void shouldNotPublishCancelEventWhenOrderNotCancelled() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(null);
        // 模拟更新失败
        when(groupMapper.updatePendingGroupOrderToCancelled(eq(groupOrder.getId()), any())).thenReturn(0);

        task.cancelTimeoutOrders();

        // 更新失败 → 抛出 RuntimeException → 不应发布事件
        verify(groupEventPublisher, never()).publishOrderCancelled(anyString(), anyLong(), anyLong(), anyString());
    }

    @Test
    public void shouldNotPublishCancelEventWhenPaymentIsPending() {
        OshGroupOrder groupOrder = pendingGroupOrder();
        OshPayment payment = new OshPayment();
        payment.setPaymentNo(groupOrder.getOrderNo());
        payment.setStatus(PaymentStatusEnum.PENDING.getCode());
        when(groupMapper.selectTimeoutOrders(any())).thenReturn(Collections.singletonList(groupOrder));
        when(paymentMapper.selectByPaymentNo(groupOrder.getOrderNo())).thenReturn(payment);

        task.cancelTimeoutOrders();

        // 走统一关单流程，不直接取消订单，因此不发布取消事件
        verify(groupEventPublisher, never()).publishOrderCancelled(anyString(), anyLong(), anyLong(), anyString());
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
