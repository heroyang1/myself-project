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
import com.backstage.system.payment.huifu.exception.HuifuApiException;
import com.backstage.system.payment.huifu.exception.HuifuConfigurationException;
import com.backstage.system.payment.huifu.exception.HuifuProtocolException;
import com.backstage.system.payment.huifu.exception.HuifuSignatureException;
import com.backstage.system.payment.huifu.exception.HuifuTransportException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇付聚合正扫轻量客户端。
 *
 * <p>不使用官方 SDK，避免 SDK debug 日志误输出敏感报文。该类自身也不记录密钥、签名和完整报文。</p>
 */
@Component
public class HuifuPayClient {

    static final String PREORDER_PATH = "/v2/trade/payment/jspay";
    static final String QUERY_PATH = "/v2/trade/payment/scanpay/query";
    static final String CLOSE_PATH = "/v2/trade/payment/scanpay/close";

    private static final String SUCCESS_CODE = "00000000";
    private static final String PROCESSING_CODE = "00000100";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<LinkedHashMap<String, Object>>() { };

    private final HuifuPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HuifuHttpTransport httpTransport;

    @Autowired
    public HuifuPayClient(HuifuPayProperties properties) {
        this(properties, new ObjectMapper(), new JdkHuifuHttpTransport());
    }

    HuifuPayClient(HuifuPayProperties properties,
                   ObjectMapper objectMapper,
                   HuifuHttpTransport httpTransport) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpTransport = httpTransport;
    }

    /** 聚合正扫下单，返回 qr_code；前端将该 URL 生成二维码。 */
    public HuifuPaymentResult createPayment(HuifuCreatePaymentRequest request) {
        validateConfiguration();
        validateCreateRequest(request);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("req_date", request.getRequestDate());
        data.put("req_seq_id", request.getRequestSequenceId());
        data.put("huifu_id", properties.getHuifuId());
        data.put("trade_type", request.getTradeType());
        data.put("goods_desc", request.getGoodsDescription());
        data.put("trans_amt", formatAmount(request.getAmount()));
        data.put("delay_acct_flag", "N");
        data.put("notify_url", chooseNotifyUrl(request.getNotifyUrl()));
        if (!isBlank(request.getPrivateInfo())) {
            data.put("mer_priv", request.getPrivateInfo());
        }
        if (!isBlank(request.getExpireTime())) {
            data.put("time_expire", request.getExpireTime());
        }

        Map<String, Object> response = postAndVerify(PREORDER_PATH, data);
        validateBusinessCode(response, true);
        validateMerchant(response);
        validateEcho(response, "req_date", request.getRequestDate(), true);
        validateEcho(response, "req_seq_id", request.getRequestSequenceId(), true);

        HuifuPaymentResult result = mapPaymentResult(response, false);
        if (isBlank(result.getJumpUrl())) {
            throw new HuifuProtocolException("汇付正扫下单成功但未返回 qr_code");
        }
        return result;
    }

    /** 主动查单；只有 result.isQueryPaidSuccessfully() 为 true 才能发放权益。 */
    public HuifuPaymentResult queryPayment(HuifuQueryPaymentRequest request) {
        validateConfiguration();
        validateQueryRequest(request);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("req_date", request.getRequestDate());
        data.put("req_seq_id", request.getRequestSequenceId());
        data.put("huifu_id", properties.getHuifuId());
        if (!isBlank(request.getOriginalRequestDate())) {
            data.put("org_req_date", request.getOriginalRequestDate());
        }
        if (!isBlank(request.getOriginalRequestSequenceId())) {
            data.put("org_req_seq_id", request.getOriginalRequestSequenceId());
        }
        if (!isBlank(request.getPartyOrderId())) {
            data.put("party_order_id", request.getPartyOrderId());
        }

        Map<String, Object> response = postAndVerify(QUERY_PATH, data);
        validateBusinessCode(response, false);
        validateMerchant(response);
        if (!isBlank(request.getOriginalRequestDate())) {
            validateEcho(response, "org_req_date", request.getOriginalRequestDate(), true);
            validateEcho(response, "org_req_seq_id", request.getOriginalRequestSequenceId(), true);
        }

        HuifuPaymentResult result = mapPaymentResult(response, true);
        if (request.getExpectedAmount() != null) {
            if (result.getAmount() == null) {
                throw new HuifuProtocolException("汇付查单缺少交易金额");
            }
            if (request.getExpectedAmount().compareTo(result.getAmount()) != 0) {
                throw new HuifuProtocolException("汇付查单金额与本地订单不一致");
            }
        }
        return result;
    }

    /** 关闭统一收银台未完成订单。 */
    public HuifuCloseResult closePayment(HuifuClosePaymentRequest request) {
        validateConfiguration();
        validateCloseRequest(request);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("req_date", request.getRequestDate());
        data.put("req_seq_id", request.getRequestSequenceId());
        data.put("huifu_id", properties.getHuifuId());
        data.put("org_req_date", request.getOriginalRequestDate());
        data.put("org_req_seq_id", request.getOriginalRequestSequenceId());

        Map<String, Object> response = postAndVerify(CLOSE_PATH, data);
        validateBusinessCode(response, true);
        validateMerchant(response);
        validateEcho(response, "req_date", request.getRequestDate(), true);
        validateEcho(response, "req_seq_id", request.getRequestSequenceId(), true);

        HuifuCloseResult result = new HuifuCloseResult();
        result.setResponseCode(asString(response.get("resp_code")));
        result.setResponseMessage(asString(response.get("resp_desc")));
        result.setRequestDate(asString(response.get("req_date")));
        result.setRequestSequenceId(asString(response.get("req_seq_id")));
        result.setOriginalRequestDate(asString(response.get("org_req_date")));
        result.setOriginalRequestSequenceId(asString(response.get("org_req_seq_id")));
        result.setOriginalTransactionStatus(parseOptionalStatus(response.get("org_trans_stat")));
        result.setCloseStatus(parseCloseStatus(firstNonBlank(response, "close_stat", "trans_stat"), "close_stat"));
        return result;
    }

    /**
     * 验签并解析托管支付异步通知。验签发生在 JSON 解析之前，确保 resp_data 原文未被重排。
     */
    public HuifuPaymentResult verifyNotification(HuifuNotificationEnvelope envelope) {
        validateConfiguration();
        if (envelope == null || isBlank(envelope.getRawResponseData()) || isBlank(envelope.getSignature())) {
            throw new HuifuProtocolException("汇付异步通知字段不完整");
        }
        // 异步外层 resp_code 是消息投递码，官方示例为 10000；真实交易结果只认
        // 已签名 resp_data 内的 resp_code/trans_stat，不能拿同步接口的 00000000 规则套这里。
        if (!HuifuRsa2.verifyAsyncResponseData(envelope.getRawResponseData(),
                envelope.getSignature(), publicKey())) {
            throw new HuifuSignatureException("汇付异步通知验签失败");
        }

        Map<String, Object> response = parseRawData(envelope.getRawResponseData());
        validateBusinessCode(response, false);
        validateMerchant(response);
        HuifuPaymentResult result = mapPaymentResult(response, true);
        if (result.getTransactionStatus() != HuifuTransactionStatus.SUCCESS
                && result.getTransactionStatus() != HuifuTransactionStatus.FAILED) {
            throw new HuifuProtocolException("汇付异步通知只能是成功或失败终态");
        }
        if (isBlank(result.getRequestDate()) || isBlank(result.getRequestSequenceId())) {
            throw new HuifuProtocolException("汇付异步通知缺少原支付流水");
        }
        return result;
    }

    public boolean verifyNotificationSignature(String rawResponseData, String signature) {
        validateConfiguration();
        return HuifuRsa2.verifyAsyncResponseData(rawResponseData, signature, publicKey());
    }

    private Map<String, Object> postAndVerify(String path, Map<String, Object> data) {
        String signature = HuifuRsa2.signData(data, privateKey());
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("sys_id", properties.getSysId());
        body.put("product_id", properties.getProductId());
        body.put("data", data);
        body.put("sign", signature);

        String requestJson = toCompactJson(body, "请求报文");
        String responseBody = httpTransport.postJson(buildUrl(path), requestJson,
                properties.getConnectTimeoutMs(), properties.getReadTimeoutMs(), properties.getProxyToken());
        if (isBlank(responseBody)) {
            throw new HuifuTransportException("汇付支付网关返回空响应", null);
        }
        return parseAndVerifySyncResponse(responseBody);
    }

    private Map<String, Object> parseAndVerifySyncResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root == null || !root.isObject() || !root.hasNonNull("sign")
                    || !root.has("data") || !root.get("data").isObject()) {
                throw new HuifuProtocolException("汇付同步返回报文结构错误");
            }
            Map<String, Object> data = objectMapper.convertValue(root.get("data"), MAP_TYPE);
            String signature = root.get("sign").asText();
            if (!HuifuRsa2.verifySyncData(data, signature, publicKey())) {
                throw new HuifuSignatureException("汇付同步返回验签失败");
            }
            return data;
        } catch (HuifuProtocolException e) {
            throw e;
        } catch (HuifuSignatureException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new HuifuProtocolException("汇付同步返回不是合法 JSON", e);
        } catch (IllegalArgumentException e) {
            throw new HuifuProtocolException("汇付同步返回 data 解析失败", e);
        }
    }

    private HuifuPaymentResult mapPaymentResult(Map<String, Object> response, boolean parseTransactionState) {
        HuifuPaymentResult result = new HuifuPaymentResult();
        result.setResponseCode(asString(response.get("resp_code")));
        result.setResponseMessage(asString(response.get("resp_desc")));
        result.setRequestDate(firstNonBlank(response, "org_req_date", "req_date"));
        result.setRequestSequenceId(firstNonBlank(response, "org_req_seq_id", "req_seq_id"));
        result.setHuifuSequenceId(firstNonBlank(response, "org_hf_seq_id", "hf_seq_id"));
        result.setPreOrderId(asString(response.get("pre_order_id")));
        result.setOrderStatus(asString(response.get("order_stat")));
        result.setTradeType(firstNonBlank(response, "pay_type", "trans_type", "trade_type"));
        result.setAmount(parseOptionalAmount(response.get("trans_amt"), "trans_amt"));
        result.setPaidAmount(parseOptionalAmount(response.get("pay_amt"), "pay_amt"));
        if (parseTransactionState) {
            result.setTransactionStatus(parseRequiredStatus(response.get("trans_stat"), "trans_stat"));
            result.setCloseStatus(parseOptionalCloseStatus(response.get("close_stat"), "close_stat"));
        }
        result.setJumpUrl(firstNonBlank(response, "qr_code", "jump_url", "pay_info"));
        result.setHuifuId(asString(response.get("huifu_id")));
        result.setPartyOrderId(asString(response.get("party_order_id")));
        result.setOutsideTransactionId(asString(response.get("out_trans_id")));
        result.setBankMessage(firstNonBlank(response, "bank_message", "bank_desc"));
        result.setRemark(asString(response.get("remark")));
        result.setEndTime(firstNonBlank(response, "end_time", "trans_finish_time", "trans_time"));
        validateOrderStatus(result.getOrderStatus());
        return result;
    }

    private Map<String, Object> parseRawData(String rawData) {
        try {
            return objectMapper.readValue(rawData, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new HuifuProtocolException("汇付异步 resp_data 不是合法 JSON", e);
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new HuifuConfigurationException("汇付支付未启用");
        }
        requireText(properties.getBaseUrl(), "pay.huifu.base-url");
        validateHttpsUrl(properties.getBaseUrl(), "pay.huifu.base-url");
        requireText(properties.getSysId(), "pay.huifu.sys-id");
        requireText(properties.getProductId(), "pay.huifu.product-id");
        requireText(properties.getHuifuId(), "pay.huifu.huifu-id");
        requireText(properties.getPrivateKeyFile(), "pay.huifu.private-key-file");
        requireText(properties.getPublicKeyFile(), "pay.huifu.public-key-file");
        if (properties.getConnectTimeoutMs() <= 0 || properties.getReadTimeoutMs() <= 0) {
            throw new HuifuConfigurationException("汇付 HTTP 超时时间必须大于 0");
        }
    }

    private void validateCreateRequest(HuifuCreatePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("汇付预下单请求不能为空");
        }
        validateDate(request.getRequestDate(), "请求日期");
        requireText(request.getRequestSequenceId(), "请求流水号");
        validateLength(request.getRequestSequenceId(), 64, "请求流水号");
        requireText(request.getTradeType(), "交易类型");
        validateLength(request.getTradeType(), 16, "交易类型");
        requireText(request.getGoodsDescription(), "商品描述");
        validateLength(request.getGoodsDescription(), 40, "商品描述");
        requireText(request.getPrivateInfo(), "商户私有信息");
        validateLength(request.getPrivateInfo(), 255, "商户私有信息");
        formatAmount(request.getAmount());
        if (!isBlank(request.getExpireTime()) && !request.getExpireTime().matches("\\d{14}")) {
            throw new IllegalArgumentException("交易失效时间必须是 yyyyMMddHHmmss");
        }
        if (!isBlank(request.getNotifyUrl())) {
            validateCallbackUrl(request.getNotifyUrl(), "异步通知地址");
        }
    }

    private void validateQueryRequest(HuifuQueryPaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("汇付查单请求不能为空");
        }
        validateDate(request.getRequestDate(), "查单请求日期");
        requireText(request.getRequestSequenceId(), "查单请求流水号");
        validateLength(request.getRequestSequenceId(), 64, "查单请求流水号");
        boolean byOriginalRequest = !isBlank(request.getOriginalRequestDate())
                && !isBlank(request.getOriginalRequestSequenceId());
        boolean byPartyOrder = !isBlank(request.getPartyOrderId());
        if (!byOriginalRequest && !byPartyOrder) {
            throw new IllegalArgumentException("查单必须提供原请求日期和流水号，或汇付商户订单号");
        }
        if (byOriginalRequest) {
            validateDate(request.getOriginalRequestDate(), "原交易请求日期");
            validateLength(request.getOriginalRequestSequenceId(), 64, "原交易请求流水号");
        }
        if (byPartyOrder) {
            validateLength(request.getPartyOrderId(), 64, "汇付商户订单号");
        }
        if (request.getExpectedAmount() != null) {
            formatAmount(request.getExpectedAmount());
        }
    }

    private void validateCloseRequest(HuifuClosePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("汇付关单请求不能为空");
        }
        validateDate(request.getRequestDate(), "关单请求日期");
        requireText(request.getRequestSequenceId(), "关单请求流水号");
        validateLength(request.getRequestSequenceId(), 64, "关单请求流水号");
        validateDate(request.getOriginalRequestDate(), "原交易请求日期");
        requireText(request.getOriginalRequestSequenceId(), "原交易请求流水号");
        validateLength(request.getOriginalRequestSequenceId(), 64, "原交易请求流水号");
    }

    private void validateBusinessCode(Map<String, Object> response, boolean allowProcessing) {
        String responseCode = asString(response.get("resp_code"));
        if (SUCCESS_CODE.equals(responseCode)) {
            return;
        }
        if (allowProcessing && PROCESSING_CODE.equals(responseCode)) {
            return;
        }
        throw new HuifuApiException(responseCode, asString(response.get("resp_desc")));
    }

    private void validateMerchant(Map<String, Object> response) {
        String huifuId = asString(response.get("huifu_id"));
        if (isBlank(huifuId) || !properties.getHuifuId().equals(huifuId)) {
            throw new HuifuProtocolException("汇付返回商户号与当前配置不一致");
        }
    }

    private void validateEcho(Map<String, Object> response,
                              String field,
                              String expected,
                              boolean required) {
        String actual = asString(response.get(field));
        if (required && isBlank(actual)) {
            throw new HuifuProtocolException("汇付返回缺少字段: " + field);
        }
        if (!isBlank(actual) && !actual.equals(expected)) {
            throw new HuifuProtocolException("汇付返回字段与请求不一致: " + field);
        }
    }

    private void validateOrderStatus(String orderStatus) {
        if (!isBlank(orderStatus) && !Arrays.asList("1", "2", "3", "4", "5", "6").contains(orderStatus)) {
            throw new HuifuProtocolException("非法的汇付预下单状态");
        }
    }

    private HuifuTransactionStatus parseRequiredStatus(Object value, String field) {
        String status = asString(value);
        if (isBlank(status)) {
            throw new HuifuProtocolException("汇付返回缺少字段: " + field);
        }
        try {
            return HuifuTransactionStatus.fromCode(status);
        } catch (IllegalArgumentException e) {
            throw new HuifuProtocolException("汇付返回非法状态字段: " + field, e);
        }
    }

    private HuifuTransactionStatus parseOptionalStatus(Object value) {
        return isBlank(asString(value)) ? null : parseRequiredStatus(value, "交易状态");
    }

    private HuifuTransactionStatus parseCloseStatus(Object value, String field) {
        HuifuTransactionStatus status = parseRequiredStatus(value, field);
        if (status == HuifuTransactionStatus.INITIAL) {
            throw new HuifuProtocolException("汇付关单返回非法状态: " + field);
        }
        return status;
    }

    private HuifuTransactionStatus parseOptionalCloseStatus(Object value, String field) {
        return isBlank(asString(value)) ? null : parseCloseStatus(value, field);
    }

    private BigDecimal parseOptionalAmount(Object value, String field) {
        String amount = asString(value);
        if (isBlank(amount)) {
            return null;
        }
        try {
            return new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new HuifuProtocolException("汇付返回金额精度错误: " + field, e);
        } catch (NumberFormatException e) {
            throw new HuifuProtocolException("汇付返回金额格式错误: " + field, e);
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("支付金额不能小于 0.01 元");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("支付金额最多保留两位小数", e);
        }
    }

    private String chooseNotifyUrl(String requestNotifyUrl) {
        return isBlank(requestNotifyUrl) ? properties.getNotifyUrl() : requestNotifyUrl;
    }

    private String toCompactJson(Object value, String description) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new HuifuProtocolException("汇付" + description + "序列化失败", e);
        }
    }

    private PrivateKey privateKey() {
        try {
            return HuifuRsa2.loadPrivateKey(Paths.get(properties.getPrivateKeyFile()));
        } catch (InvalidPathException e) {
            throw new HuifuConfigurationException("汇付私钥文件路径无效", e);
        }
    }

    private PublicKey publicKey() {
        try {
            return HuifuRsa2.loadPublicKey(Paths.get(properties.getPublicKeyFile()));
        } catch (InvalidPathException e) {
            throw new HuifuConfigurationException("汇付公钥文件路径无效", e);
        }
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private void validateDate(String value, String field) {
        if (isBlank(value) || !value.matches("\\d{8}")) {
            throw new IllegalArgumentException(field + "必须是 yyyyMMdd");
        }
    }

    private void validateCallbackUrl(String value, String field) {
        validateLength(value, 512, field);
        try {
            URI uri = new URI(value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || isBlank(uri.getHost())) {
                throw new HuifuConfigurationException(field + "必须是有效的 HTTP(S) 地址");
            }
        } catch (URISyntaxException e) {
            throw new HuifuConfigurationException(field + "格式错误", e);
        }
    }

    private void validateHttpsUrl(String value, String field) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || isBlank(uri.getHost())) {
                throw new HuifuConfigurationException(field + "必须是有效的 HTTPS 地址");
            }
        } catch (URISyntaxException e) {
            throw new HuifuConfigurationException(field + "格式错误", e);
        }
    }

    private void validateLength(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(field + "长度不能超过 " + maxLength);
        }
    }

    private void requireText(String value, String field) {
        if (isBlank(value)) {
            throw new HuifuConfigurationException("缺少配置或参数: " + field);
        }
    }

    private String firstNonBlank(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            String value = asString(data.get(key));
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
