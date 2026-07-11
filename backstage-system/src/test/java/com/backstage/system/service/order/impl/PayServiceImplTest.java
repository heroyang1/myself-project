package com.backstage.system.service.order.impl;

import com.backstage.common.config.PayConfig;
import com.backstage.system.payment.huifu.client.HuifuPayClient;
import com.backstage.system.payment.huifu.dto.HuifuPaymentResult;
import com.backstage.system.payment.huifu.dto.HuifuQueryPaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuTransactionStatus;
import com.backstage.system.payment.huifu.exception.HuifuApiException;
import com.backstage.system.domain.vo.order.PayResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class PayServiceImplTest {

    private HuifuPayClient huifuPayClient;
    private PayServiceImpl payService;

    @Before
    public void setUp() {
        PayConfig payConfig = new PayConfig();
        payConfig.PROVIDER = "huifu";
        huifuPayClient = mock(HuifuPayClient.class);
        payService = new PayServiceImpl();
        ReflectionTestUtils.setField(payService, "payConfig", payConfig);
        ReflectionTestUtils.setField(payService, "huifuPayClient", huifuPayClient);
    }

    @Test
    public void shouldQueryGroupPaymentUsingDateEmbeddedInGpOrderNo() {
        HuifuPaymentResult result = new HuifuPaymentResult();
        result.setOrderStatus("1");
        result.setTransactionStatus(HuifuTransactionStatus.SUCCESS);
        result.setAmount(new BigDecimal("0.01"));
        result.setHuifuSequenceId("HF123");
        when(huifuPayClient.queryPayment(any(HuifuQueryPaymentRequest.class))).thenReturn(result);

        Map<String, String> response = payService.queryPay(
                "GP20260710121212ABCDEF", "huifu", new BigDecimal("0.01"));

        assertEquals("1", response.get("status"));
        ArgumentCaptor<HuifuQueryPaymentRequest> captor =
                ArgumentCaptor.forClass(HuifuQueryPaymentRequest.class);
        org.mockito.Mockito.verify(huifuPayClient).queryPayment(captor.capture());
        assertEquals("20260710", captor.getValue().getOriginalRequestDate());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectInternalAsGlobalExternalProvider() {
        PayConfig payConfig = new PayConfig();
        payConfig.PROVIDER = "internal";
        ReflectionTestUtils.setField(payService, "payConfig", payConfig);
        payService.currentProvider();
    }

    @Test
    public void shouldKeepLocalPaymentPendingWhenHuifuIsProcessing() {
        doThrow(new HuifuApiException("00000100", "处理中"))
                .when(huifuPayClient).createPayment(any());

        PayResponse response = payService.createPay(
                "huifu", "P202607100001", "测试商品", "0.01", "127.0.0.1", "alipay");

        assertTrue(response.getCode() == -1);
    }

    @Test
    public void shouldFailFastWhenHuifuRejectsPreorder() {
        doThrow(new HuifuApiException("E1001", "参数错误"))
                .when(huifuPayClient).createPayment(any());

        PayResponse response = payService.createPay(
                "huifu", "P202607100003", "测试商品", "0.01", "127.0.0.1", "alipay");

        assertEquals(0, response.getCode());
    }

    @Test
    public void shouldMapSuccessfulCloseFromHuifuQuery() {
        HuifuPaymentResult result = new HuifuPaymentResult();
        result.setOrderStatus("2");
        result.setTransactionStatus(HuifuTransactionStatus.PENDING);
        result.setCloseStatus(HuifuTransactionStatus.SUCCESS);
        result.setAmount(new BigDecimal("0.01"));
        when(huifuPayClient.queryPayment(any(HuifuQueryPaymentRequest.class))).thenReturn(result);

        Map<String, String> response = payService.queryPay(
                "P202607100001", "huifu", new BigDecimal("0.01"));

        assertEquals("closed", response.get("status"));
    }

    @Test
    public void shouldKeepClosingHuifuPaymentUnknown() {
        HuifuPaymentResult result = new HuifuPaymentResult();
        result.setOrderStatus("2");
        result.setTransactionStatus(HuifuTransactionStatus.PENDING);
        result.setCloseStatus(HuifuTransactionStatus.PENDING);
        result.setAmount(new BigDecimal("0.01"));
        when(huifuPayClient.queryPayment(any(HuifuQueryPaymentRequest.class))).thenReturn(result);

        Map<String, String> response = payService.queryPay(
                "P202607100002", "huifu", new BigDecimal("0.01"));

        assertEquals("unknown", response.get("status"));
    }
}
