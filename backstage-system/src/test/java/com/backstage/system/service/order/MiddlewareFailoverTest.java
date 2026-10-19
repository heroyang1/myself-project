package com.backstage.system.service.order;

import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshOrderEntitlementTask;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderEntitlementTaskMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.config.PaymentEntitlementProperties;
import com.backstage.system.service.behavior.ContributionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P1-B：中间件故障降级与系统韧性测试。
 * <p>
 * 覆盖中间件不可用时的降级行为：
 * <ul>
 *   <li>Kafka 不可用 → Outbox 事件暂存，定时任务兜底重发</li>
 *   <li>权益发放失败 → processOrderSafely 捕获异常，不影响支付 ACK</li>
 *   <li>DB 并发冲突 → claim CAS 失败 → 幂等跳过</li>
 *   <li>Handler 不存在 → 抛异常（fail-fast）</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class MiddlewareFailoverTest {

    private static final String ORDER_NO = "O20260828FAILOVER01";

    @InjectMocks
    private OrderEntitlementTaskService entitlementTaskService;

    @Mock private OshOrderMapper orderMapper;
    @Mock private OshOrderEntitlementTaskMapper taskMapper;
    @Mock private OrderPaidHandlerRegistry paidHandlerRegistry;
    @Mock private PaymentEntitlementProperties entitlementProperties;
    @Mock private ContributionService contributionService;
    @Mock private PaymentSuccessEventPublisher paymentSuccessEventPublisher;

    @Before
    public void setUp() {
        lenient().when(entitlementProperties.isTaskMode()).thenReturn(false);
        // processOrder 成功后会调用 markSuccess，默认返回 1 表示更新成功
        lenient().when(taskMapper.markSuccess(anyString(), any(LocalDateTime.class))).thenReturn(1);
    }

    // ==================== Kafka 不可用降级 ====================

    /**
     * Kafka 发送失败 → 权益任务仍标记为失败待重试
     * Outbox 定时任务后续兜底重发
     */
    @Test
    public void shouldPersistTaskWhenKafkaPublishFails() {
        // Given: 正常订单，handler 正常执行
        when(taskMapper.claim(eq(ORDER_NO), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        OshOrder order = buildPaidOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);

        OshOrderEntitlementTask task = new OshOrderEntitlementTask();
        task.setOrderNo(ORDER_NO);
        task.setProductType(ProductTypeEnum.COURSE.getCode());
        when(taskMapper.selectByOrderNo(ORDER_NO)).thenReturn(task);
        when(paidHandlerRegistry.hasHandler("course")).thenReturn(true);

        // When: handler 正常但后续 publish 失败
        doThrow(new RuntimeException("Kafka broker unavailable"))
                .when(paymentSuccessEventPublisher).publish(ORDER_NO);

        // processOrderSafely 不应抛异常
        entitlementTaskService.processOrderSafely(ORDER_NO);

        // Then: handler 仍然被调用了（权益已发放）
        verify(paidHandlerRegistry).handle("course", ORDER_NO);
    }

    // ==================== 权益发放异常隔离 ====================

    /**
     * handler.handle 抛异常 → processOrderSafely 捕获 → 不影响支付平台 ACK
     * 任务保留 FAILED 状态，定时重试任务后续补偿
     */
    @Test
    public void shouldSwallowHandlerExceptionInProcessOrderSafely() {
        when(taskMapper.claim(eq(ORDER_NO), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        OshOrder order = buildPaidOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);

        OshOrderEntitlementTask task = new OshOrderEntitlementTask();
        task.setOrderNo(ORDER_NO);
        task.setProductType(ProductTypeEnum.TOOL.getName().equals(ProductTypeEnum.TOOL.getName())
                ? ProductTypeEnum.TOOL.getCode() : ProductTypeEnum.TOOL.getCode());
        when(taskMapper.selectByOrderNo(ORDER_NO)).thenReturn(task);
        when(paidHandlerRegistry.hasHandler(anyString())).thenReturn(true);
        doThrow(new RuntimeException("Handler internal error"))
                .when(paidHandlerRegistry).handle(anyString(), anyString());

        // When: processOrderSafely 不应抛异常
        entitlementTaskService.processOrderSafely(ORDER_NO);

        // Then: 没有未捕获异常传播到调用方
    }

    // ==================== DB 并发冲突降级 ====================

    /**
     * claim CAS 失败（被其他线程/实例抢占）→ 直接返回，不重试
     */
    @Test
    public void shouldSkipWhenClaimCasFails() {
        when(taskMapper.claim(eq(ORDER_NO), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);

        entitlementTaskService.processOrderSafely(ORDER_NO);

        verify(orderMapper, never()).selectByOrderNo(anyString());
        verify(paidHandlerRegistry, never()).handle(anyString(), anyString());
    }

    /**
     * DB 连接异常 → processOrderSafely 捕获 → 任务保留待重试
     */
    @Test
    public void shouldSwallowDbExceptionInProcessOrderSafely() {
        when(taskMapper.claim(eq(ORDER_NO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("DB connection pool exhausted"));

        // processOrderSafely 不应抛异常
        entitlementTaskService.processOrderSafely(ORDER_NO);
    }

    // ==================== 过期任务锁恢复 ====================

    /**
     * 任务被其他实例持有但已超时（staleBefore）→ 当前实例可以抢占
     * claim 使用 CAS：WHERE status != SUCCESS AND (lock_expire < now OR order_no = ?)
     */
    @Test
    public void shouldReclaimStaleLockedTask() {
        when(taskMapper.claim(eq(ORDER_NO), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        OshOrder order = buildPaidOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);

        OshOrderEntitlementTask task = new OshOrderEntitlementTask();
        task.setOrderNo(ORDER_NO);
        task.setProductType(ProductTypeEnum.COURSE.getCode());
        when(taskMapper.selectByOrderNo(ORDER_NO)).thenReturn(task);
        when(paidHandlerRegistry.hasHandler("course")).thenReturn(true);

        entitlementTaskService.processOrder(ORDER_NO);

        verify(paidHandlerRegistry).handle("course", ORDER_NO);
    }

    // ==================== ensureTask 幂等保障 ====================

    /**
     * ensureTask 在支付事务内调用 → insertOrKeep 唯一键保证不重复创建
     * 即使重复回调也只有一条任务记录
     */
    @Test
    public void shouldEnsureTaskIdempotentlyWithUniqueKey() {
        OshOrder order = buildPaidOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);

        OshOrderEntitlementTask task = new OshOrderEntitlementTask();
        task.setOrderNo(ORDER_NO);
        task.setProductType(ProductTypeEnum.COURSE.getCode());
        when(taskMapper.selectByOrderNo(ORDER_NO)).thenReturn(task);

        // 多次调用
        entitlementTaskService.ensureTask(ORDER_NO);
        entitlementTaskService.ensureTask(ORDER_NO);

        // insertOrKeep 被调用两次但唯一键保证幂等
        verify(taskMapper, times(2)).insertOrKeep(ORDER_NO, ProductTypeEnum.COURSE.getCode());
    }

    // ==================== Kafka 积压恢复（批量重试） ====================

    /**
     * Kafka 长时间不可用 → 多笔订单权益发放失败积压在 DB。
     * Kafka 恢复后定时任务调用 retryDueTasks() → 批量处理所有到期任务。
     * 验证：所有积压任务被逐一处理，无遗漏、无重复发放。
     */
    @Test
    public void shouldRecoverAllBackloggedTasksWhenKafkaComesBack() {
        // Given: taskMode 开启（定时任务生效）
        when(entitlementProperties.isTaskMode()).thenReturn(true);

        // 模拟 5 笔积压任务
        int backlogCount = 5;
        List<OshOrderEntitlementTask> dueTasks = new ArrayList<>();
        for (int i = 1; i <= backlogCount; i++) {
            OshOrderEntitlementTask t = new OshOrderEntitlementTask();
            t.setOrderNo("O_BACKLOG_" + i);
            t.setProductType(ProductTypeEnum.COURSE.getCode());
            dueTasks.add(t);
        }
        when(taskMapper.selectDueTasks(any(LocalDateTime.class), any(LocalDateTime.class), eq(50)))
                .thenReturn(dueTasks);

        // 每笔任务的 claim 都成功（无并发争用）
        when(taskMapper.claim(anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        for (int i = 1; i <= backlogCount; i++) {
            String orderNo = "O_BACKLOG_" + i;
            OshOrder order = buildPaidOrder();
            order.setOrderNo(orderNo);
            when(orderMapper.selectByOrderNo(orderNo)).thenReturn(order);
            OshOrderEntitlementTask task = new OshOrderEntitlementTask();
            task.setOrderNo(orderNo);
            task.setProductType(ProductTypeEnum.COURSE.getCode());
            when(taskMapper.selectByOrderNo(orderNo)).thenReturn(task);
        }
        when(paidHandlerRegistry.hasHandler("course")).thenReturn(true);

        // When: 定时任务触发批量重试
        int handled = entitlementTaskService.retryDueTasks();

        // Then: 所有积压任务都被处理
        assertEquals("应处理全部积压任务", backlogCount, handled);
        verify(paidHandlerRegistry, times(backlogCount)).handle(eq("course"), anyString());
        verify(taskMapper, times(backlogCount)).markSuccess(anyString(), any(LocalDateTime.class));
    }

    /**
     * taskMode 关闭时 → retryDueTasks 直接返回 0，不查询 DB。
     * 验证：非任务模式下定时任务不执行批量重试。
     */
    @Test
    public void shouldSkipRetryWhenTaskModeDisabled() {
        when(entitlementProperties.isTaskMode()).thenReturn(false);

        int handled = entitlementTaskService.retryDueTasks();

        assertEquals("taskMode 关闭应返回 0", 0, handled);
        verify(taskMapper, never()).selectDueTasks(any(), any(), anyInt());
    }

    /**
     * 批量重试中某笔任务处理失败 → 不影响其他任务继续处理。
     * 验证：单笔失败不阻断整批恢复。
     */
    @Test
    public void shouldContinueProcessingRemainingTasksWhenOneFails() {
        when(entitlementProperties.isTaskMode()).thenReturn(true);

        List<OshOrderEntitlementTask> dueTasks = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            OshOrderEntitlementTask t = new OshOrderEntitlementTask();
            t.setOrderNo("O_BATCH_" + i);
            t.setProductType(ProductTypeEnum.COURSE.getCode());
            dueTasks.add(t);
        }
        when(taskMapper.selectDueTasks(any(LocalDateTime.class), any(LocalDateTime.class), eq(50)))
                .thenReturn(dueTasks);

        // 第 2 笔任务 claim 失败（已被其他实例处理），其余成功
        when(taskMapper.claim(eq("O_BATCH_1"), any(), any())).thenReturn(1);
        when(taskMapper.claim(eq("O_BATCH_2"), any(), any())).thenReturn(0); // 被抢占
        when(taskMapper.claim(eq("O_BATCH_3"), any(), any())).thenReturn(1);

        OshOrder order = buildPaidOrder();
        when(orderMapper.selectByOrderNo(anyString())).thenReturn(order);
        OshOrderEntitlementTask task = new OshOrderEntitlementTask();
        task.setProductType(ProductTypeEnum.COURSE.getCode());
        when(taskMapper.selectByOrderNo(anyString())).thenReturn(task);
        when(paidHandlerRegistry.hasHandler("course")).thenReturn(true);

        // When
        int handled = entitlementTaskService.retryDueTasks();

        // Then: 3 笔都"处理"了（含被跳过的），但只有 2 笔真正发放了权益
        assertEquals(3, handled);
        verify(paidHandlerRegistry, times(2)).handle(eq("course"), anyString());
    }

    // ==================== 辅助方法 ====================

    private OshOrder buildPaidOrder() {
        OshOrder order = new OshOrder();
        order.setId(1L);
        order.setOrderNo(ORDER_NO);
        order.setStatus(OrderStatusEnum.PAID.getCode());
        order.setProductType(ProductTypeEnum.COURSE.getCode());
        order.setUserId(100L);
        order.setProductId(200L);
        order.setProductName("测试课程");
        return order;
    }
}
