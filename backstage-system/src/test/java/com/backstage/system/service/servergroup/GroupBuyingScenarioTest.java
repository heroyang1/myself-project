package com.backstage.system.service.servergroup;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.component.CommentForbiddenWordFilter;
import com.backstage.system.domain.servergroup.OshGroupActivity;
import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.servergroup.OshGroupUserInitiated;
import com.backstage.system.domain.servergroup.OshGroupWork;
import com.backstage.system.domain.servergroup.enums.GroupConstants;
import com.backstage.system.domain.servergroup.enums.GroupOrderStatusEnum;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.mapper.servergroup.OshGroupUserInitiatedMapper;
import com.backstage.system.service.servergroup.event.GroupEventPublisher;
import com.backstage.system.service.servergroup.impl.OshGroupServerServiceImpl;
import com.backstage.system.utils.OssUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P0-B：拼团全链路端到端场景测试。
 * <p>
 * 基于场景法覆盖拼团模块核心业务流程：
 * <ul>
 *   <li>场景 A：支付成功 → 幂等检查 → 订单状态更新 → CAS 递增 → 成团判断</li>
 *   <li>场景 B：重复回调 → 幂等短路</li>
 *   <li>场景 C：异常订单处理（不存在/数据不完整）</li>
 *   <li>场景 D：数据源路由（系统模板 vs 用户发起）</li>
 *   <li>场景 E：超时取消入口已废弃 → 走安全对账任务</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("P0-B：拼团全链路场景测试")
class GroupBuyingScenarioTest {

    private static final String ORDER_NO = "GRP20260828001";

    @InjectMocks
    private OshGroupServerServiceImpl groupServerService;

    @Mock private OshGroupServerMapper groupServerMapper;
    @Mock private OshGroupUserInitiatedMapper userInitiatedMapper;
    @Mock private OshOrderMapper oshOrderMapper;
    @Mock private OshPaymentMapper oshPaymentMapper;
    @Mock private OssUtil ossUtil;
    @Mock private RedisTemplate<Object, Object> redisTemplate;
    @Mock private RedissonClient redissonClient;
    @Mock private ValueOperations<Object, Object> valueOperations;
    @Mock private CommentForbiddenWordFilter forbiddenWordFilter;
    @Mock private GroupEventPublisher groupEventPublisher;
    @Mock private RLock rLock;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    // ==================== 场景 A：支付成功全链路（系统活动） ====================

    @Nested
    @DisplayName("场景A：系统活动支付成功全链路")
    class SystemActivityPaymentSuccess {

        /**
         * Happy Path：待支付订单 → 支付回调 → 状态更新为已支付
         * → CAS 递增 → 未满员 → 返回 true
         */
        @Test
        @DisplayName("正常支付回调：待支付→已支付→CAS递增→未满员→返回true")
        void shouldCompletePaymentFlowWhenNotFull() throws Exception {
            // Given: 待支付的系统活动订单
            OshGroupOrder order = buildPendingOrder(GroupConstants.SOURCE_TYPE_SYSTEM);
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);
            when(groupServerMapper.updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(oshOrderMapper.updatePendingToPaid(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            // requireOrderGroupWork 需要的 mock
            OshGroupWork work = buildGroupWork(order);
            when(groupServerMapper.selectGroupWorkById(order.getGroupWorkId())).thenReturn(work);
            when(groupServerMapper.updatePendingGroupWorkToPaid(work.getId())).thenReturn(1);

            // 系统活动路径：atomicIncrementCurrentNum 成功
            when(groupServerMapper.atomicIncrementCurrentNum(order.getGroupActivityId())).thenReturn(1);

            // 查询活动信息（未满员：currentNum=2 < maxNum=10, minNum=2 → isFormed=true）
            OshGroupActivity activity = buildActivity(2, 2, 10);
            when(groupServerMapper.selectGroupActivityById(order.getGroupActivityId())).thenReturn(activity);
            when(groupServerMapper.batchUpdateWorkStatusByActivity(eq(order.getGroupActivityId()), eq(2), any(), any())).thenReturn(1);

            // When
            boolean result = groupServerService.handlePaymentSuccess(ORDER_NO);

            // Then
            assertTrue(result, "系统活动支付成功应返回 true");
            verify(groupServerMapper).updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class));
        }
    }

    // ==================== 场景 B：重复回调幂等短路 ====================

    @Nested
    @DisplayName("场景B：重复回调幂等保护")
    class IdempotentProtection {

        /**
         * 已支付订单重复回调 → isPaidOrSuccess() == true → 直接返回 true
         */
        @Test
        @DisplayName("重复回调：订单已支付→幂等短路→返回true")
        void shouldSkipWhenOrderAlreadyPaid() {
            OshGroupOrder order = new OshGroupOrder();
            order.setId(1L);
            order.setOrderNo(ORDER_NO);
            order.setStatus(GroupOrderStatusEnum.PAID.getCode());
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);

            boolean result = groupServerService.handlePaymentSuccess(ORDER_NO);

            assertTrue(result, "已支付订单重复回调应返回 true");
            verify(groupServerMapper, never()).updatePendingOrderToPaid(anyLong(), any(LocalDateTime.class));
        }

        /**
         * 已成功的订单重复回调 → SUCCESS 也是终态 → 幂等短路
         */
        @Test
        @DisplayName("重复回调：订单已拼团成功→幂等短路→返回true")
        void shouldSkipWhenOrderAlreadySuccess() {
            OshGroupOrder order = new OshGroupOrder();
            order.setId(1L);
            order.setOrderNo(ORDER_NO);
            order.setStatus(GroupOrderStatusEnum.SUCCESS.getCode());
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);

            boolean result = groupServerService.handlePaymentSuccess(ORDER_NO);

            assertTrue(result);
            verify(groupServerMapper, never()).updatePendingOrderToPaid(anyLong(), any(LocalDateTime.class));
        }

        /**
         * CAS 更新返回 0 → 二次查询 → 已被并发线程处理为已支付 → 返回 true
         */
        @Test
        @DisplayName("并发回调：CAS更新失败→二次查询→已被其他线程处理→返回true")
        void shouldIdempotentOnCasConflict() {
            OshGroupOrder pendingOrder = buildPendingOrder(GroupConstants.SOURCE_TYPE_SYSTEM);
            // 第一次查：待支付
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO))
                    .thenReturn(pendingOrder);
            // CAS 更新失败（被并发线程抢先）
            when(groupServerMapper.updatePendingOrderToPaid(eq(pendingOrder.getId()), any(LocalDateTime.class)))
                    .thenReturn(0);

            // 二次查：已支付
            OshGroupOrder paidOrder = new OshGroupOrder();
            paidOrder.setStatus(GroupOrderStatusEnum.PAID.getCode());
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO))
                    .thenReturn(pendingOrder)  // 第一次调用
                    .thenReturn(paidOrder);   // 第二次调用（CAS 失败后）

            boolean result = groupServerService.handlePaymentSuccess(ORDER_NO);

            assertTrue(result, "并发回调已被处理时应返回 true");
        }
    }

    // ==================== 场景 C：异常订单处理 ====================

    @Nested
    @DisplayName("场景C：异常订单处理")
    class AbnormalOrderHandling {

        /**
         * 订单不存在 → 返回 false（不抛异常，避免支付平台重试风暴）
         */
        @Test
        @DisplayName("订单不存在→返回false")
        void shouldReturnFalseWhenOrderNotFound() {
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(null);

            boolean result = groupServerService.handlePaymentSuccess(ORDER_NO);

            assertFalse(result, "订单不存在应返回 false");
            verify(groupServerMapper, never()).updatePendingOrderToPaid(anyLong(), any(LocalDateTime.class));
        }

        /**
         * 订单缺少 groupWorkId → 抛异常（数据不完整）
         */
        @Test
        @DisplayName("订单缺少groupWorkId→抛ServiceException")
        void shouldThrowWhenGroupWorkIdMissing() {
            OshGroupOrder order = buildPendingOrder(GroupConstants.SOURCE_TYPE_SYSTEM);
            order.setGroupWorkId(null);
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);
            when(groupServerMapper.updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(oshOrderMapper.updatePendingToPaid(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);

            assertThrows(ServiceException.class,
                    () -> groupServerService.handlePaymentSuccess(ORDER_NO));
        }

        /**
         * 用户发起拼团但缺少 initiatedId → 抛异常
         */
        @Test
        @DisplayName("用户发起拼团缺少initiatedId→抛ServiceException")
        void shouldThrowWhenInitiatedIdMissing() {
            OshGroupOrder order = buildPendingOrder(GroupConstants.SOURCE_TYPE_INITIATED);
            order.setGroupWorkId(20L);
            order.setInitiatedId(null);
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);
            when(groupServerMapper.updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(oshOrderMapper.updatePendingToPaid(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);

            assertThrows(ServiceException.class,
                    () -> groupServerService.handlePaymentSuccess(ORDER_NO));
        }

        /**
         * 未知来源类型 → 抛异常
         */
        @Test
        @DisplayName("未知来源类型→抛ServiceException")
        void shouldThrowWhenSourceTypeUnknown() {
            OshGroupOrder order = buildPendingOrder("unknown");
            order.setGroupWorkId(20L);
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);
            when(groupServerMapper.updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(oshOrderMapper.updatePendingToPaid(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);

            assertThrows(ServiceException.class,
                    () -> groupServerService.handlePaymentSuccess(ORDER_NO));
        }
    }

    // ==================== 场景 D：用户发起拼团路径 ====================

    @Nested
    @DisplayName("场景D：用户发起拼团路径")
    class UserInitiatedPaymentPath {

        /**
         * 用户发起拼团 → CAS 递增 → 未满员 → 返回 true
         */
        @Test
        @DisplayName("用户发起拼团：正常支付→incrementCurrentNumAtomic→未满员→返回true")
        void shouldRouteToInitiatedPathAndIncrement() throws Exception {
            OshGroupOrder order = buildPendingOrder(GroupConstants.SOURCE_TYPE_INITIATED);
            order.setGroupWorkId(20L);
            order.setInitiatedId(200L);
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);
            when(groupServerMapper.updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(oshOrderMapper.updatePendingToPaid(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            // requireOrderGroupWork 需要的 mock
            OshGroupWork work = new OshGroupWork();
            work.setId(20L);
            work.setOrderId(order.getId());
            work.setUserId(order.getUserId());
            work.setGroupStatus(0);
            when(groupServerMapper.selectGroupWorkById(20L)).thenReturn(work);
            when(groupServerMapper.updatePendingGroupWorkToPaid(20L)).thenReturn(1);

            OshGroupUserInitiated initiated = new OshGroupUserInitiated();
            initiated.setId(200L);
            initiated.setMinNum(2);
            initiated.setMaxNum(5);
            initiated.setDuration(3);
            initiated.setCurrentNum(1);
            when(userInitiatedMapper.selectById(200L)).thenReturn(initiated);
            when(userInitiatedMapper.incrementCurrentNumAtomic(200L)).thenReturn(1);

            boolean result = groupServerService.handlePaymentSuccess(ORDER_NO);

            assertTrue(result);
            // selectById 被调用两次：handlePaymentSuccess 中查 initiated + handleUserInitiatedPayment 中递增后重读
            verify(userInitiatedMapper, times(2)).selectById(200L);
        }

        /**
         * 满员降级：用户发起拼团时活动已满员 → CAS 递增返回 0 → 抛异常触发事务回滚。
         * 验证不抛 NPE、不发放权益、不发布成团/满员事件，订单状态更新随事务回滚。
         */
        @Test
        @DisplayName("用户发起拼团：已满员→CAS递增返回0→抛ServiceException触发回滚")
        void shouldThrowAndRollbackWhenGroupAlreadyFull() throws Exception {
            OshGroupOrder order = buildPendingOrder(GroupConstants.SOURCE_TYPE_INITIATED);
            order.setGroupWorkId(20L);
            order.setInitiatedId(200L);
            when(groupServerMapper.selectGroupOrderByOrderNo(ORDER_NO)).thenReturn(order);
            when(groupServerMapper.updatePendingOrderToPaid(eq(order.getId()), any(LocalDateTime.class))).thenReturn(1);
            when(oshOrderMapper.updatePendingToPaid(eq(ORDER_NO), any(LocalDateTime.class))).thenReturn(1);
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            // requireOrderGroupWork 需要的 mock（work 状态正常）
            OshGroupWork work = new OshGroupWork();
            work.setId(20L);
            work.setOrderId(order.getId());
            work.setUserId(order.getUserId());
            work.setGroupStatus(0);
            when(groupServerMapper.selectGroupWorkById(20L)).thenReturn(work);

            OshGroupUserInitiated initiated = new OshGroupUserInitiated();
            initiated.setId(200L);
            initiated.setMinNum(2);
            initiated.setMaxNum(5);
            initiated.setDuration(3);
            initiated.setCurrentNum(5); // 已满员
            when(userInitiatedMapper.selectById(200L)).thenReturn(initiated);
            // CAS 递增失败（已满员或已删除）→ 返回 0
            when(userInitiatedMapper.incrementCurrentNumAtomic(200L)).thenReturn(0);

            // When/Then: 应抛 ServiceException 触发事务回滚（订单状态回退为 PENDING）
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> groupServerService.handlePaymentSuccess(ORDER_NO));
            assertTrue(ex.getMessage().contains("拼团权益处理失败"), "异常信息应提示权益处理失败");

            // Then: 不更新 work 状态、不发放权益、不发布任何成团/满员事件
            verify(groupServerMapper, never()).updatePendingGroupWorkToPaid(anyLong());
            verify(groupEventPublisher, never()).publishGroupFull(anyLong(), any(), anyLong(), anyString());
            verify(groupEventPublisher, never()).publishGroupFormed(anyLong(), any(), anyLong(), anyString());
            verify(groupEventPublisher, never()).publishOrderPaid(anyString(), any(), anyLong(), any(), anyString());
        }
    }

    // ==================== 场景 E：超时取消入口已废弃 ====================

    @Nested
    @DisplayName("场景E：旧批量取消入口已废弃")
    class DeprecatedCancelEntry {

        /**
         * cancelExpiredOrders 已废弃，直接返回 0
         * 实际取消走 GroupOrderTimeoutCancelTask → OrderService.cancelPayment 安全对账
         */
        @Test
        @DisplayName("旧批量取消入口→返回0（已废弃）")
        void shouldReturnZeroFromDeprecatedCancelMethod() {
            int result = groupServerService.cancelExpiredOrders();
            assertEquals(0, result, "旧入口已废弃，应返回 0");
        }
    }

    // ==================== 辅助方法 ====================

    private OshGroupOrder buildPendingOrder(String sourceType) {
        OshGroupOrder order = new OshGroupOrder();
        order.setId(1L);
        order.setOrderNo(ORDER_NO);
        order.setStatus(GroupOrderStatusEnum.PENDING.getCode());
        order.setUserId(100L);
        order.setGroupWorkId(10L);
        order.setGroupActivityId(1L);
        order.setSourceType(sourceType);
        return order;
    }

    private OshGroupWork buildGroupWork(OshGroupOrder order) {
        OshGroupWork work = new OshGroupWork();
        work.setId(10L);
        work.setOrderId(order.getId());
        work.setUserId(order.getUserId());
        work.setGroupStatus(0);
        return work;
    }

    private OshGroupActivity buildActivity(int currentNum, int minNum, int maxNum) {
        OshGroupActivity activity = new OshGroupActivity();
        activity.setId(1L);
        activity.setCurrentNum(currentNum);
        activity.setGroupMinNum(minNum);
        activity.setGroupMaxNum(maxNum);
        activity.setTotalDuration(1);
        activity.setStatus(1);
        activity.setServerStartTime(LocalDateTime.now());
        activity.setServerEndTime(LocalDateTime.now().plusMonths(1));
        return activity;
    }
}
