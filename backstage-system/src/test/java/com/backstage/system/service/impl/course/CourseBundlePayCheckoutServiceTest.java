package com.backstage.system.service.impl.course;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.service.order.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CourseBundlePayCheckoutServiceTest {

    private CourseBundlePayCheckoutService service;
    private OrderService orderService;

    @Before
    public void setUp() {
        service = new CourseBundlePayCheckoutService();
        orderService = mock(OrderService.class);
        ReflectionTestUtils.setField(service, "orderService", orderService);
    }

    @Test
    public void shouldDelegateCourseAndBundleToUnifiedOrderFlowWithoutPoints() {
        OrderCheckoutRespVO expected = new OrderCheckoutRespVO();
        when(orderService.checkout(org.mockito.ArgumentMatchers.any(OrderCheckoutReqVO.class), eq(Boolean.FALSE)))
                .thenReturn(expected);

        OrderCheckoutRespVO actual = service.checkout(
                7L,
                ProductTypeEnum.BUNDLE,
                8L,
                "测试套餐",
                new BigDecimal("8.80"),
                new BigDecimal("5.80"),
                "alipay",
                "127.0.0.1"
        );

        ArgumentCaptor<OrderCheckoutReqVO> requestCaptor = ArgumentCaptor.forClass(OrderCheckoutReqVO.class);
        verify(orderService).checkout(requestCaptor.capture(), eq(Boolean.FALSE));
        OrderCheckoutReqVO request = requestCaptor.getValue();

        assertSame(expected, actual);
        assertEquals(Long.valueOf(7L), request.getUserId());
        assertEquals(Integer.valueOf(ProductTypeEnum.BUNDLE.getCode()), request.getProductType());
        assertEquals(Long.valueOf(8L), request.getProductId());
        assertEquals(new BigDecimal("3.00"), request.getDiscountAmount());
        assertEquals("alipay", request.getChannel());
        assertFalse(request.getUsePoints());
    }
}
