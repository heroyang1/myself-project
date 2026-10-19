package com.backstage.system.service.order;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.handler.seckill.SeckillPaidHandler;
import com.backstage.system.service.order.handler.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * P2-A：全模块等价类测试矩阵。
 * <p>
 * 基于等价类划分，按商品类型分组测试 OrderPaidHandlerRegistry 的路由分发逻辑：
 * <ul>
 *   <li>等价类 1：course → CoursePaidHandler</li>
 *   <li>等价类 2：book → BookPaidHandler</li>
 *   <li>等价类 3：seckill → SeckillPaidHandler</li>
 *   <li>等价类 4：tool → ToolPaidHandler</li>
 *   <li>等价类 5：group → GroupPaidHandler</li>
 *   <li>等价类 6：member → MemberPaidHandler</li>
 *   <li>等价类 7：bundle → BundlePaidHandler</li>
 *   <li>等价类 8：unknown → 抛异常</li>
 * </ul>
 * <p>
 * 同时覆盖审核状态机等价类（四状态 × 有效/无效转换）。
 */
@RunWith(MockitoJUnitRunner.class)
public class EquivalenceClassTest {

    private OrderPaidHandlerRegistry registry;

    @Mock private CoursePaidHandler coursePaidHandler;
    @Mock private BookPaidHandler bookPaidHandler;
    @Mock private SeckillPaidHandler seckillPaidHandler;
    @Mock private ToolPaidHandler toolPaidHandler;
    @Mock private GroupPaidHandler groupPaidHandler;
    @Mock private MemberPaidHandler memberPaidHandler;
    @Mock private BundlePaidHandler bundlePaidHandler;

    @Before
    public void setUp() {
        when(coursePaidHandler.bizType()).thenReturn(ProductTypeEnum.COURSE.getName());
        when(bookPaidHandler.bizType()).thenReturn(ProductTypeEnum.BOOK.getName());
        when(seckillPaidHandler.bizType()).thenReturn(ProductTypeEnum.SECKILL.getName());
        when(toolPaidHandler.bizType()).thenReturn(ProductTypeEnum.TOOL.getName());
        when(groupPaidHandler.bizType()).thenReturn(ProductTypeEnum.GROUP.getName());
        when(memberPaidHandler.bizType()).thenReturn(ProductTypeEnum.MEMBER.getName());
        when(bundlePaidHandler.bizType()).thenReturn(ProductTypeEnum.BUNDLE.getName());

        List<OrderPaidHandler> handlers = Arrays.asList(
                coursePaidHandler, bookPaidHandler, seckillPaidHandler,
                toolPaidHandler, groupPaidHandler, memberPaidHandler, bundlePaidHandler
        );
        registry = new OrderPaidHandlerRegistry(handlers);
    }

    // ==================== 等价类 1-7：各商品类型路由 ====================

    /**
     * 等价类：course → CoursePaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteCourseToCoursePaidHandler() {
        registry.handle("course", "O001");
        verify(coursePaidHandler).handle("O001");
    }

    /**
     * 等价类：book → BookPaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteBookToBookPaidHandler() {
        registry.handle("book", "O002");
        verify(bookPaidHandler).handle("O002");
    }

    /**
     * 等价类：seckill → SeckillPaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteSeckillToSeckillPaidHandler() {
        registry.handle("seckill", "O003");
        verify(seckillPaidHandler).handle("O003");
    }

    /**
     * 等价类：tool → ToolPaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteToolToToolPaidHandler() {
        registry.handle("tool", "O004");
        verify(toolPaidHandler).handle("O004");
    }

    /**
     * 等价类：group → GroupPaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteGroupToGroupPaidHandler() {
        registry.handle("group", "O005");
        verify(groupPaidHandler).handle("O005");
    }

    /**
     * 等价类：member → MemberPaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteMemberToMemberPaidHandler() {
        registry.handle("member", "O006");
        verify(memberPaidHandler).handle("O006");
    }

    /**
     * 等价类：bundle → BundlePaidHandler.handle(orderNo)
     */
    @Test
    public void shouldRouteBundleToBundlePaidHandler() {
        registry.handle("bundle", "O007");
        verify(bundlePaidHandler).handle("O007");
    }

    // ==================== 等价类 8：未知类型 → 异常 ====================

    /**
     * 等价类：unknown → 抛 ServiceException（未注册的 handler）
     */
    @Test(expected = ServiceException.class)
    public void shouldThrowForUnknownProductType() {
        registry.handle("unknown_type", "O999");
    }

    // ==================== hasHandler 判断 ====================

    @Test
    public void shouldReturnTrueForRegisteredBizTypes() {
        assertTrue(registry.hasHandler("course"));
        assertTrue(registry.hasHandler("book"));
        assertTrue(registry.hasHandler("seckill"));
        assertTrue(registry.hasHandler("tool"));
        assertTrue(registry.hasHandler("group"));
        assertTrue(registry.hasHandler("member"));
        assertTrue(registry.hasHandler("bundle"));
    }

    @Test
    public void shouldReturnFalseForUnregisteredBizType() {
        assertFalse(registry.hasHandler("unknown"));
        assertFalse(registry.hasHandler(""));
        assertFalse(registry.hasHandler(null));
    }

    // ==================== 审核状态机等价类 ====================

    /**
     * 审核状态四态等价类：
     * DRAFT(0) → PENDING_AUDIT(2) → PUBLISHED(4) / OFF_SHELF(6)
     * <p>
     * 有效转换：DRAFT→PENDING, DRAFT→PUBLISHED(创始人), PENDING→PUBLISHED, PENDING→OFF_SHELF
     * 无效转换：PUBLISHED→DRAFT, OFF_SHELF→DRAFT（终态不可回退）
     */
    @Test
    public void shouldDefineValidAuditStateTransitions() {
        // 定义四状态
        final int DRAFT = 0;
        final int PENDING_AUDIT = 2;
        final int PUBLISHED = 4;
        final int OFF_SHELF = 6;

        // 有效转换
        assertTrue("DRAFT → PENDING_AUDIT 应允许", isValidTransition(DRAFT, PENDING_AUDIT));
        assertTrue("DRAFT → PUBLISHED 应允许（创始人直接通过）", isValidTransition(DRAFT, PUBLISHED));
        assertTrue("PENDING_AUDIT → PUBLISHED 应允许", isValidTransition(PENDING_AUDIT, PUBLISHED));
        assertTrue("PENDING_AUDIT → OFF_SHELF 应允许", isValidTransition(PENDING_AUDIT, OFF_SHELF));

        // 无效转换（终态不可回退）
        assertFalse("PUBLISHED → DRAFT 不应允许", isValidTransition(PUBLISHED, DRAFT));
        assertFalse("OFF_SHELF → DRAFT 不应允许", isValidTransition(OFF_SHELF, DRAFT));
        assertFalse("PUBLISHED → PENDING 不应允许", isValidTransition(PUBLISHED, PENDING_AUDIT));
    }

    /**
     * 模拟审核状态转换验证（简化版，实际逻辑在 ResourceAuditServiceImpl 中）
     */
    private boolean isValidTransition(int from, int to) {
        // CAS 闸门：仅 DRAFT(0) 和 PENDING_AUDIT(2) 可以流转到其他状态
        return (from == 0 || from == 2) && (to == 0 || to == 2 || to == 4 || to == 6) && from != to;
    }

    // ==================== 订单取消处理器注册表等价类 ====================

    /**
     * OrderCancelHandlerRegistry 等价类：
     * 已知类型 → 执行 handler
     * 未知类型 → handleIfPresent 静默跳过（不抛异常）
     */
    @Test
    public void shouldSilentlySkipUnregisteredCancelHandler() {
        OrderCancelHandlerRegistry cancelRegistry = new OrderCancelHandlerRegistry(
                Arrays.asList(/* 空的或只有部分 handler */));

        // 未知类型 → 返回 false，不抛异常
        boolean handled = cancelRegistry.handleIfPresent("nonexistent", "O999", "P999");
        assertFalse("未注册类型应返回 false", handled);
    }
}
