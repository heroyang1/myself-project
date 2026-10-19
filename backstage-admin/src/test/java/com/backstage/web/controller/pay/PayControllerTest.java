package com.backstage.web.controller.pay;

import com.backstage.system.controller.pay.PayController;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.service.order.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PayControllerTest {

    private MockMvc mockMvc;
    private OrderService orderService;

    @Before
    public void setUp() {
        PayController controller = new PayController();
        orderService = mock(OrderService.class);
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void shouldNotExposeGenericClientPricedCreateEndpoint() throws Exception {
        mockMvc.perform(post("/pc/pay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":9,\"productType\":5,\"productId\":1001,\"productName\":\"AI海报生成器\",\"originalAmount\":19.90,\"payableAmount\":19.90,\"channel\":\"alipay\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldWrapOrderStatusResponseInStandardResult() throws Exception {
        OrderStatusResult statusResult = new OrderStatusResult();
        statusResult.setOrderNo("O20260524002");
        statusResult.setPaymentNo("P20260524002");
        statusResult.setOrderStatus(0);
        statusResult.setPaymentStatus(0);
        statusResult.setPayStatus(false);

        when(orderService.getOrderStatusForUser("O20260524002", null)).thenReturn(statusResult);

        mockMvc.perform(get("/pc/pay/status").param("orderNo", "O20260524002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("O20260524002"))
                .andExpect(jsonPath("$.data.paymentNo").value("P20260524002"))
                .andExpect(jsonPath("$.data.orderStatus").value(0))
                .andExpect(jsonPath("$.data.paymentStatus").value(0))
                .andExpect(jsonPath("$.data.payStatus").value(false));
    }

    @Test
    public void shouldWrapCancelResponseInStandardResult() throws Exception {
        mockMvc.perform(post("/pc/pay/cancel").param("orderNo", "O20260524003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        verify(orderService).cancelPaymentByOrderNoForUser(eq("O20260524003"), eq(null));
    }
}
