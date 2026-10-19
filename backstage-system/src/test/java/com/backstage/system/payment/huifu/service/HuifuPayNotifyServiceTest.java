package com.backstage.system.payment.huifu.service;

import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.payment.VerifiedPaymentNotification;
import com.backstage.system.payment.huifu.client.HuifuPayClient;
import com.backstage.system.payment.huifu.dto.HuifuNotificationEnvelope;
import com.backstage.system.payment.huifu.dto.HuifuPaymentResult;
import com.backstage.system.payment.huifu.dto.HuifuTransactionStatus;
import com.backstage.system.service.order.OrderService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HuifuPayNotifyServiceTest {

    private HuifuPayClient client;
    private OrderService orderService;
    private HuifuPayNotifyService service;

    @Before
    public void setUp() {
        client = mock(HuifuPayClient.class);
        orderService = mock(OrderService.class);
        service = new HuifuPayNotifyService(client, orderService);
    }

    @Test
    public void shouldPromoteVerifiedAlipayNotificationAndReturnOfficialAck() {
        HuifuPaymentResult result = successResult("A_NATIVE");
        when(client.verifyNotification(any(HuifuNotificationEnvelope.class))).thenReturn(result);
        when(orderService.handleVerifiedPayNotify(any(VerifiedPaymentNotification.class))).thenReturn(true);

        String ack = service.handle("10000", "success", "signature", "raw-json");

        ArgumentCaptor<VerifiedPaymentNotification> captor =
                ArgumentCaptor.forClass(VerifiedPaymentNotification.class);
        verify(orderService).handleVerifiedPayNotify(captor.capture());
        VerifiedPaymentNotification notification = captor.getValue();
        assertEquals("RECV_ORD_ID_P202607100000001", ack);
        assertEquals("huifu", notification.getProvider());
        assertEquals("P202607100000001", notification.getPaymentNo());
        assertEquals("HF123", notification.getPlatformTradeNo());
        assertEquals(new BigDecimal("0.01"), notification.getAmount());
        assertEquals(Integer.valueOf(PayChannelEnum.ALIPAY.getCode()), notification.getChannel());
        assertTrue(notification.getSafePayload().containsKey("trans_stat"));
    }

    @Test
    public void shouldAckSignedFailedNotificationWithoutGrantingRights() {
        HuifuPaymentResult result = successResult("T_JSAPI");
        result.setTransactionStatus(HuifuTransactionStatus.FAILED);
        when(client.verifyNotification(any(HuifuNotificationEnvelope.class))).thenReturn(result);
        when(orderService.handleVerifiedPayFailure(any(VerifiedPaymentNotification.class))).thenReturn(true);

        String ack = service.handle("10000", "failed", "signature", "raw-json");

        assertEquals("RECV_ORD_ID_P202607100000001", ack);
        verify(orderService, never()).handleVerifiedPayNotify(any(VerifiedPaymentNotification.class));
        verify(orderService).handleVerifiedPayFailure(any(VerifiedPaymentNotification.class));
    }

    private HuifuPaymentResult successResult(String tradeType) {
        HuifuPaymentResult result = new HuifuPaymentResult();
        result.setRequestDate("20260710");
        result.setRequestSequenceId("P202607100000001");
        result.setHuifuSequenceId("HF123");
        result.setOrderStatus("1");
        result.setTradeType(tradeType);
        result.setAmount(new BigDecimal("0.01"));
        result.setTransactionStatus(HuifuTransactionStatus.SUCCESS);
        return result;
    }
}
