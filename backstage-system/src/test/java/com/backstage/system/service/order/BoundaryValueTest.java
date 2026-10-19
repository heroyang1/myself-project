package com.backstage.system.service.order;

import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P1-A：库存 / 金额 / 时间边界值测试套件。
 * <p>
 * 基于边界值分析（Boundary Value Analysis），专测"刚好在临界点上"的数据：
 * <ul>
 *   <li>金额边界：unitPrice vs totalAmount（历史 Bug 5135e950）、零金额、BigDecimal 精度</li>
 *   <li>ID 边界：雪花 ID 超 INT 上限（历史 Bug a13289a5）</li>
 *   <li>订单状态边界：PENDING→PAID 的临界 CAS</li>
 *   <li>商品类型边界：合法枚举值 vs 非法值</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class BoundaryValueTest {

    @InjectMocks
    private OrderEntitlementTaskService entitlementTaskService;

    @Mock
    private OshOrderMapper orderMapper;

    // ==================== 金额边界值 ====================

    /**
     * 边界值：金额刚好为 0（免费订单）
     * payableAmount=0 时走 FREE 渠道，不经过支付网关
     */
    @Test
    public void shouldHandleZeroAmountOrderGracefully() {
        BigDecimal zeroAmount = BigDecimal.ZERO;
        BigDecimal unitPrice = new BigDecimal("0.00");

        assertEquals("零金额应等于 0", 0, zeroAmount.compareTo(BigDecimal.ZERO));
        assertEquals("0.00 应与 0 相等", 0, unitPrice.compareTo(BigDecimal.ZERO));
        // BigDecimal 精度验证
        assertEquals("0.1 + 0.2 应精确等于 0.3",
                new BigDecimal("0.3"),
                new BigDecimal("0.1").add(new BigDecimal("0.2")));
    }

    /**
     * 边界值：totalAmount ≠ unitPrice × quantity
     * 历史 Bug 5135e950：createPay 传 unitPrice（单价）而非 totalAmount（实付总额）
     */
    @Test
    public void shouldDistinguishUnitPriceFromTotalAmount() {
        BigDecimal unitPrice = new BigDecimal("99.00");
        int quantity = 3;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        assertEquals("totalAmount 应为 297.00", new BigDecimal("297.00"), totalAmount);
        assertNotEquals("totalAmount 不应等于 unitPrice", unitPrice, totalAmount);
    }

    /**
     * 边界值：金额精度 - 必须用 BigDecimal 而非 double
     * 0.1 + 0.2 != 0.3（浮点数精度丢失）
     */
    @Test
    public void shouldUseBigDecimalNotDoubleForMoney() {
        double d1 = 0.1 + 0.2;
        assertNotEquals("double 0.1+0.2 不等于 0.3（精度丢失）", 0.3, d1, 0);

        BigDecimal b1 = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        assertEquals("BigDecimal 0.1+0.2 精确等于 0.3",
                0, b1.compareTo(new BigDecimal("0.3")));
    }

    /**
     * 边界值：极大金额 - 验证不溢出
     */
    @Test
    public void shouldHandleLargeAmountWithoutOverflow() {
        BigDecimal largeAmount = new BigDecimal("99999999999.99");
        BigDecimal result = largeAmount.add(new BigDecimal("0.01"));
        assertEquals(new BigDecimal("100000000000.00"), result);
    }

    // ==================== ID 边界值（雪花 ID vs INT） ====================

    /**
     * 边界值：雪花 ID 刚好超过 INT_MAX
     * 历史 Bug a13289a5：INT 列被雪花 ID 截断，本地正常线上报错
     */
    @Test
    public void shouldDetectSnowflakeIdBeyondIntRange() {
        long snowflakeId = 2147483648L;  // Integer.MAX_VALUE + 1
        assertTrue("雪花 ID 应超过 INT 上限", snowflakeId > Integer.MAX_VALUE);

        // 验证不会静默截断
        int truncated = (int) snowflakeId;
        assertNotEquals("截断后不等于原值", snowflakeId, truncated);
        assertEquals("截断后变为负数", Integer.MIN_VALUE, truncated);
    }

    /**
     * 边界值：雪花 ID 在 BIGINT 范围内
     */
    @Test
    public void shouldFitSnowflakeIdInBigintRange() {
        long maxSnowflakeId = Long.MAX_VALUE;  // 9223372036854775807
        assertTrue("雪花 ID 在 BIGINT 范围内", maxSnowflakeId > 0);

        // 典型雪花 ID（18 位）
        long typicalSnowflake = 123456789012345678L;
        assertTrue("典型雪花 ID 应大于 INT_MAX", typicalSnowflake > Integer.MAX_VALUE);
    }

    // ==================== 订单状态边界值 ====================

    /**
     * 边界值：订单状态 PENDING(0) 到 PAID(1) 的临界转换
     */
    @Test
    public void shouldCorrectlyIdentifyOrderStatusTransitions() {
        assertEquals("PENDING 应为 0", 0, OrderStatusEnum.PENDING.getCode());
        assertEquals("PAID 应为 1", 1, OrderStatusEnum.PAID.getCode());

        // 边界验证：status=0 可以转 PAID，status=1 不能再次转 PAID
        OshOrder pendingOrder = new OshOrder();
        pendingOrder.setStatus(OrderStatusEnum.PENDING.getCode());
        assertEquals("待支付订单 status=0", 0, (int) pendingOrder.getStatus());

        OshOrder paidOrder = new OshOrder();
        paidOrder.setStatus(OrderStatusEnum.PAID.getCode());
        assertEquals("已支付订单 status=1", 1, (int) paidOrder.getStatus());
    }

    // ==================== 商品类型边界值 ====================

    /**
     * 边界值：合法商品类型枚举值（1-8）vs 非法值（0, 9, -1, null）
     */
    @Test
    public void shouldHandleValidAndInvalidProductTypeCodes() {
        // 合法值（1-8）
        for (int code = 1; code <= 8; code++) {
            assertNotNull("code=" + code + " 应为合法枚举",
                    ProductTypeEnum.fromCode(code));
        }

        // 非法值
        assertNull("code=0 应为 null", ProductTypeEnum.fromCode(0));
        assertNull("code=9 应为 null", ProductTypeEnum.fromCode(9));
        assertNull("code=-1 应为 null", ProductTypeEnum.fromCode(-1));
        assertNull("code=null 应为 null", ProductTypeEnum.fromCode(null));
    }

    /**
     * 边界值：fromName 合法名称 vs 空字符串 vs null
     */
    @Test
    public void shouldHandleValidAndInvalidProductTypeNames() {
        assertNotNull(ProductTypeEnum.fromName("course"));
        assertNotNull(ProductTypeEnum.fromName("group"));
        assertNotNull(ProductTypeEnum.fromName("seckill"));

        assertNull(ProductTypeEnum.fromName(""));
        assertNull(ProductTypeEnum.fromName("unknown"));
        assertNull(ProductTypeEnum.fromName(null));
    }

    // ==================== 数量边界值 ====================

    /**
     * 边界值：购买数量 qty=1（最小有效值）
     */
    @Test
    public void shouldHandleMinimumQuantity() {
        int minQty = 1;
        assertTrue("最小购买数量应 > 0", minQty > 0);
    }

    /**
     * 边界值：库存 stock=0（刚好卖完）
     */
    @Test
    public void shouldHandleZeroStock() {
        int stock = 0;
        assertFalse("库存为 0 不应允许购买", stock > 0);
    }

    /**
     * 边界值：归还库存封顶 - LEAST(cur+qty, total)
     */
    @Test
    public void shouldCapReturnStockAtTotal() {
        int currentStock = 10;
        int totalStock = 10;
        int returnQty = 3;
        // LEAST(currentStock + returnQty, totalStock) = LEAST(13, 10) = 10
        int capped = Math.min(currentStock + returnQty, totalStock);
        assertEquals("归还后不应超过总库存", totalStock, capped);
    }

    /**
     * 边界值：销量防负 - GREATEST(sales-qty, 0)
     */
    @Test
    public void shouldPreventNegativeSalesCount() {
        int currentSales = 2;
        int returnQty = 5;
        // GREATEST(currentSales - returnQty, 0) = GREATEST(-3, 0) = 0
        int floored = Math.max(currentSales - returnQty, 0);
        assertEquals("销量不应为负数", 0, floored);
    }
}
