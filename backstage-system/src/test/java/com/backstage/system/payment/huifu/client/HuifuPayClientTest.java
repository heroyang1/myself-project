package com.backstage.system.payment.huifu.client;

import com.backstage.system.payment.huifu.config.HuifuPayProperties;
import com.backstage.system.payment.huifu.crypto.HuifuRsa2;
import com.backstage.system.payment.huifu.dto.HuifuClosePaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuCloseResult;
import com.backstage.system.payment.huifu.dto.HuifuCreatePaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuNotificationEnvelope;
import com.backstage.system.payment.huifu.dto.HuifuPaymentResult;
import com.backstage.system.payment.huifu.dto.HuifuQueryPaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuTransactionStatus;
import com.backstage.system.payment.huifu.exception.HuifuSignatureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HuifuPayClientTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HuifuPayProperties properties;
    private KeyPair huifuKeyPair;
    private RecordingTransport transport;
    private HuifuPayClient client;

    @Before
    public void setUp() throws Exception {
        huifuKeyPair = generateKeyPair();
        java.io.File privateKeyFile = temporaryFolder.newFile("app-private.key");
        java.io.File publicKeyFile = temporaryFolder.newFile("huifu-public.key");
        Files.write(privateKeyFile.toPath(), Base64.getEncoder()
                .encodeToString(huifuKeyPair.getPrivate().getEncoded()).getBytes(StandardCharsets.UTF_8));
        Files.write(publicKeyFile.toPath(), Base64.getEncoder()
                .encodeToString(huifuKeyPair.getPublic().getEncoded()).getBytes(StandardCharsets.UTF_8));

        properties = new HuifuPayProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://api.huifu.test");
        properties.setSysId("6666000000000001");
        properties.setProductId("YYZY");
        properties.setHuifuId("6666000000000002");
        properties.setProjectId("OSH_PC_CASHIER");
        properties.setProjectTitle("OSH 支付收银台");
        properties.setPrivateKeyFile(privateKeyFile.getAbsolutePath());
        properties.setPublicKeyFile(publicKeyFile.getAbsolutePath());
        properties.setNotifyUrl("https://api.example.com/notify/huifu");
        properties.setCallbackUrl("https://www.example.com/pay/result");

        transport = new RecordingTransport();
        client = new HuifuPayClient(properties, objectMapper, transport);
    }

    @Test
    public void createsAggregateScanPaymentWithRequestedTradeType() throws Exception {
        Map<String, Object> responseData = baseResponse("20260710", "OSH202607100001");
        responseData.put("resp_code", "00000100");
        responseData.put("resp_desc", "下单成功");
        responseData.put("hf_seq_id", "HF202607100001");
        responseData.put("trade_type", "A_NATIVE");
        responseData.put("trans_stat", "P");
        responseData.put("qr_code", "https://qr.alipay.com/bax000000000000000");
        String response = signedResponse(responseData, huifuKeyPair);

        transport.response = response;

        HuifuPaymentResult result = client.createPayment(createRequest());

        assertEquals("HF202607100001", result.getHuifuSequenceId());
        assertEquals("https://qr.alipay.com/bax000000000000000", result.getJumpUrl());
        assertEquals("https://api.huifu.test" + HuifuPayClient.PREORDER_PATH, transport.url);
        com.fasterxml.jackson.databind.JsonNode data = objectMapper.readTree(transport.requestJson).get("data");
        assertFalse(data.has("project_id"));
        assertEquals("A_NATIVE", data.get("trade_type").asText());
        assertEquals("PAY202607100001", data.get("mer_priv").asText());
    }

    @Test
    public void queryIsSuccessfulOnlyWhenOrderAndTransactionStatusesBothSucceed() throws Exception {
        Map<String, Object> responseData = baseResponse("20260710", "QUERY202607100001");
        responseData.put("org_req_date", "20260710");
        responseData.put("org_req_seq_id", "OSH202607100001");
        responseData.put("org_hf_seq_id", "HF202607100001");
        responseData.put("pre_order_id", "H202607100001");
        responseData.put("order_stat", "1");
        responseData.put("trans_stat", "S");
        responseData.put("trans_amt", "0.01");
        String response = signedResponse(responseData, huifuKeyPair);

        transport.response = response;

        HuifuQueryPaymentRequest request = new HuifuQueryPaymentRequest();
        request.setRequestDate("20260710");
        request.setRequestSequenceId("QUERY202607100001");
        request.setOriginalRequestDate("20260710");
        request.setOriginalRequestSequenceId("OSH202607100001");
        request.setExpectedAmount(new BigDecimal("0.01"));
        HuifuPaymentResult result = client.queryPayment(request);

        assertTrue(result.isQueryPaidSuccessfully());
        assertEquals("OSH202607100001", result.getRequestSequenceId());
        assertEquals("https://api.huifu.test" + HuifuPayClient.QUERY_PATH, transport.url);
        result.setOrderStatus("2");
        assertFalse(result.isQueryPaidSuccessfully());
        result.setOrderStatus("1");
        result.setTransactionStatus(HuifuTransactionStatus.fromCode("I"));
        assertEquals(HuifuTransactionStatus.INITIAL, result.getTransactionStatus());
        assertFalse(result.isQueryPaidSuccessfully());
    }

    @Test
    public void closesScanPaymentWithOfficialV2Endpoint() throws Exception {
        Map<String, Object> responseData = baseResponse("20260710", "CLOSE202607100001");
        responseData.put("org_req_date", "20260710");
        responseData.put("org_req_seq_id", "OSH202607100001");
        responseData.put("org_trans_stat", "P");
        responseData.put("trans_stat", "S");
        transport.response = signedResponse(responseData, huifuKeyPair);

        HuifuClosePaymentRequest request = new HuifuClosePaymentRequest();
        request.setRequestDate("20260710");
        request.setRequestSequenceId("CLOSE202607100001");
        request.setOriginalRequestDate("20260710");
        request.setOriginalRequestSequenceId("OSH202607100001");
        HuifuCloseResult result = client.closePayment(request);

        assertEquals("S", result.getCloseStatus().getCode());
        assertEquals("https://api.huifu.test" + HuifuPayClient.CLOSE_PATH, transport.url);
    }

    @Test
    public void queryReturnsOfficialCloseStatus() throws Exception {
        Map<String, Object> responseData = baseResponse("20260710", "QUERY202607100002");
        responseData.put("org_req_date", "20260710");
        responseData.put("org_req_seq_id", "OSH202607100002");
        responseData.put("order_stat", "2");
        responseData.put("trans_stat", "P");
        responseData.put("close_stat", "S");
        responseData.put("trans_amt", "0.01");
        transport.response = signedResponse(responseData, huifuKeyPair);

        HuifuQueryPaymentRequest request = new HuifuQueryPaymentRequest();
        request.setRequestDate("20260710");
        request.setRequestSequenceId("QUERY202607100002");
        request.setOriginalRequestDate("20260710");
        request.setOriginalRequestSequenceId("OSH202607100002");
        request.setExpectedAmount(new BigDecimal("0.01"));

        HuifuPaymentResult result = client.queryPayment(request);

        assertEquals(HuifuTransactionStatus.SUCCESS, result.getCloseStatus());
        assertFalse(result.isQueryPaidSuccessfully());
    }

    @Test
    public void verifiesNotificationRawTextAndBuildsOfficialAcknowledgement() throws Exception {
        Map<String, Object> notificationData = new LinkedHashMap<String, Object>();
        notificationData.put("req_seq_id", "OSH202607100001");
        notificationData.put("resp_desc", "交易成功");
        notificationData.put("huifu_id", properties.getHuifuId());
        notificationData.put("trans_stat", "S");
        notificationData.put("req_date", "20260710");
        notificationData.put("resp_code", "00000000");
        notificationData.put("trans_amt", "0.01");
        notificationData.put("hf_seq_id", "HF202607100001");
        notificationData.put("remark", "PAY202607100001");
        String rawResponseData = objectMapper.writeValueAsString(notificationData);

        HuifuNotificationEnvelope envelope = new HuifuNotificationEnvelope();
        envelope.setResponseCode("10000");
        envelope.setResponseMessage("成功");
        envelope.setRawResponseData(rawResponseData);
        envelope.setSignature(HuifuRsa2.signRaw(rawResponseData, huifuKeyPair.getPrivate()));

        HuifuPaymentResult result = client.verifyNotification(envelope);

        assertEquals("HF202607100001", result.getHuifuSequenceId());
        assertEquals("PAY202607100001", result.getRemark());
        assertEquals("RECV_ORD_ID_OSH202607100001", result.buildAcknowledgement());
    }

    @Test(expected = HuifuSignatureException.class)
    public void rejectsSynchronousResponseWithInvalidSignature() throws Exception {
        Map<String, Object> responseData = baseResponse("20260710", "OSH202607100001");
        responseData.put("pre_order_id", "H202607100001");
        responseData.put("qr_code", "https://qr.alipay.com/bax000000000000000");
        String response = signedResponse(responseData, generateKeyPair());

        transport.response = response;

        client.createPayment(createRequest());
    }

    private HuifuCreatePaymentRequest createRequest() {
        HuifuCreatePaymentRequest request = new HuifuCreatePaymentRequest();
        request.setRequestDate("20260710");
        request.setRequestSequenceId("OSH202607100001");
        request.setTradeType("A_NATIVE");
        request.setAmount(new BigDecimal("0.01"));
        request.setGoodsDescription("OSH 课程");
        request.setPrivateInfo("PAY202607100001");
        request.setExpireTime("20260710235959");
        return request;
    }

    private Map<String, Object> baseResponse(String requestDate, String requestSequenceId) {
        Map<String, Object> responseData = new LinkedHashMap<String, Object>();
        responseData.put("resp_code", "00000000");
        responseData.put("resp_desc", "成功");
        responseData.put("req_date", requestDate);
        responseData.put("req_seq_id", requestSequenceId);
        responseData.put("huifu_id", properties.getHuifuId());
        return responseData;
    }

    private String signedResponse(Map<String, Object> data, KeyPair signer) throws Exception {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("data", data);
        response.put("sign", HuifuRsa2.signData(data, signer.getPrivate()));
        return objectMapper.writeValueAsString(response);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static class RecordingTransport implements HuifuHttpTransport {
        private String response;
        private String url;
        private String requestJson;

        @Override
        public String postJson(String url,
                               String requestJson,
                               int connectTimeoutMs,
                               int readTimeoutMs,
                               String proxyToken) {
            this.url = url;
            this.requestJson = requestJson;
            return response;
        }
    }
}
