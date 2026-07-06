package com.backstage.system.service.impl.course;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.backstage.common.config.PayConfig;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.order.OrderPaymentInfo;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.order.enums.PurchaseModeEnum;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.order.OrderNoGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Course/package scoped checkout path for the RSA payment platform.
 */
@Service
public class CourseBundlePayCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CourseBundlePayCheckoutService.class);

    private static final String DEFAULT_PAY_CREATE_URL = "http://43.242.200.25:1717/api/pay/create";
    private static final String DEFAULT_PRIVATE_KEY_FILE = "/tmp/codex_pay_rsa.json";
    private static final String DEFAULT_RETURN_URL = "http://127.0.0.1:5173/return";
    private static final String PAY_METHOD_JUMP = "jump";
    private static final String SIGN_TYPE_RSA = "RSA";
    private static final int PAY_CREATE_SUCCESS_CODE = 1;
    private static final int DEFAULT_PAY_EXPIRE_MINUTES = 30;

    @Resource
    private PayConfig payConfig;

    @Resource
    private OshOrderMapper orderMapper;

    @Resource
    private OshPaymentMapper paymentMapper;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Value("${pay.course-api-url:}")
    private String configuredPayCreateUrl;

    @Value("${pay.private-key:}")
    private String configuredPrivateKey;

    @Value("${pay.private-key-file:" + DEFAULT_PRIVATE_KEY_FILE + "}")
    private String privateKeyFile;

    public OrderCheckoutRespVO checkout(Long userId,
                                        ProductTypeEnum productType,
                                        Long productId,
                                        String productName,
                                        BigDecimal originalAmount,
                                        BigDecimal payableAmount,
                                        String channel,
                                        String clientIp) {
        PayChannelEnum channelEnum = resolvePaymentChannel(channel);
        String orderNo = orderNoGenerator.nextOrderNo(productType.getName());
        String paymentNo = orderNoGenerator.nextPaymentNo();
        BigDecimal original = money(originalAmount);
        BigDecimal amount = money(payableAmount);

        OshOrder order = buildPendingOrder(userId, productType, productId, productName, orderNo, original, amount);
        OshPayment payment = buildPendingPayment(orderNo, paymentNo, amount, channelEnum, clientIp, productName);
        new TransactionTemplate(transactionManager).execute(status -> {
            orderMapper.insertOshOrder(order);
            payment.setOrderId(order.getId());
            paymentMapper.insertOshPayment(payment);
            return null;
        });

        PayResponse payResponse = requestPayment(paymentNo, productName, amount, clientIp, channelEnum.getValue());
        paymentMapper.updatePayResponse(
                paymentNo,
                payResponse.getTradeNo(),
                payResponse.getPayUrl(),
                payResponse.getQrcode(),
                JSON.toJSONString(payResponse)
        );

        if (payResponse.getCode() != PAY_CREATE_SUCCESS_CODE) {
            paymentMapper.updatePendingToFailed(paymentNo, JSON.toJSONString(payResponse));
            orderMapper.updatePendingToClosed(orderNo, LocalDateTime.now());
            throw new ServiceException(StringUtils.defaultIfBlank(payResponse.getMsg(), "发起支付失败"));
        }

        return toCheckoutResp(orderNo, paymentNo, amount, channelEnum.getValue(), payResponse);
    }

    private OshOrder buildPendingOrder(Long userId,
                                       ProductTypeEnum productType,
                                       Long productId,
                                       String productName,
                                       String orderNo,
                                       BigDecimal originalAmount,
                                       BigDecimal payableAmount) {
        LocalDateTime now = LocalDateTime.now();
        OshOrder order = new OshOrder();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setStatus(OrderStatusEnum.PENDING.getCode());
        order.setProductType(productType.getCode());
        order.setProductId(productId);
        order.setProductName(productName);
        order.setPurchaseMode(PurchaseModeEnum.NORMAL.getCode());
        order.setOriginalAmount(originalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayableAmount(payableAmount);
        order.setPointsAmount(0L);
        order.setPointsDeductAmount(BigDecimal.ZERO);
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        order.setDeleteFlag(0);
        return order;
    }

    private OshPayment buildPendingPayment(String orderNo,
                                           String paymentNo,
                                           BigDecimal amount,
                                           PayChannelEnum channel,
                                           String clientIp,
                                           String productName) {
        LocalDateTime now = LocalDateTime.now();
        OshPayment payment = new OshPayment();
        payment.setOrderNo(orderNo);
        payment.setPaymentNo(paymentNo);
        payment.setChannel(channel.getCode());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatusEnum.PENDING.getCode());
        payment.setRequestPayload(JSON.toJSONString(payRequestSnapshot(paymentNo, productName, amount, clientIp, channel.getValue())));
        payment.setExpireTime(now.plusMinutes(resolvePayExpireMinutes()));
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setDeleteFlag(0);
        return payment;
    }

    private PayResponse requestPayment(String paymentNo, String productName, BigDecimal amount, String clientIp, String channel) {
        try {
            Map<String, String> params = buildPayParams(paymentNo, productName, amount, clientIp, channel);
            params.put("sign", createSign(params));
            String raw = postForm(resolvePayCreateUrl(), params);
            JSONObject json = JSON.parseObject(raw);
            return mapPayResponse(json, paymentNo);
        } catch (Exception e) {
            log.warn("课程/套餐发起支付失败, paymentNo={}", paymentNo, e);
            PayResponse resp = new PayResponse();
            resp.setCode(0);
            resp.setOutTradeNo(paymentNo);
            resp.setMsg("支付平台请求失败：" + resolveErrorMessage(e));
            return resp;
        }
    }

    private Map<String, String> buildPayParams(String paymentNo,
                                               String productName,
                                               BigDecimal amount,
                                               String clientIp,
                                               String channel) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pid", payConfig.PID);
        params.put("method", PAY_METHOD_JUMP);
        params.put("type", channel);
        params.put("out_trade_no", paymentNo);
        params.put("notify_url", StringUtils.defaultIfBlank(payConfig.NOTIFY_URL, "https://example.com/pay/notify-placeholder"));
        params.put("return_url", DEFAULT_RETURN_URL);
        params.put("name", productName);
        params.put("money", money(amount).toPlainString());
        params.put("clientip", normalizeClientIp(clientIp));
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("sign_type", SIGN_TYPE_RSA);
        return params;
    }

    private String createSign(Map<String, String> params) throws Exception {
        String content = signContent(params);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(loadPrivateKey());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String signContent(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder builder = new StringBuilder();
        sorted.forEach((key, value) -> {
            if ("sign".equals(key) || "sign_type".equals(key) || StringUtils.isBlank(value)) {
                return;
            }
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(key).append('=').append(value);
        });
        return builder.toString();
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String rawKey = StringUtils.defaultIfBlank(configuredPrivateKey, readPrivateKeyFromFile());
        if (StringUtils.isBlank(rawKey)) {
            throw new ServiceException("课程/套餐支付缺少 RSA 私钥配置");
        }
        String normalized = rawKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private String readPrivateKeyFromFile() {
        try {
            if (StringUtils.isBlank(privateKeyFile) || !Files.exists(Paths.get(privateKeyFile))) {
                return "";
            }
            JSONObject json = JSON.parseObject(new String(Files.readAllBytes(Paths.get(privateKeyFile)), StandardCharsets.UTF_8));
            return json.getString("private_key");
        } catch (Exception e) {
            log.warn("读取课程/套餐支付 RSA 私钥文件失败, file={}", privateKeyFile, e);
            return "";
        }
    }

    private String postForm(String url, Map<String, String> params) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(8000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.setAll(params);
        return restTemplate.postForObject(url, body, String.class);
    }

    private PayResponse mapPayResponse(JSONObject raw, String paymentNo) {
        PayResponse resp = new PayResponse();
        resp.setOutTradeNo(paymentNo);
        if (raw == null) {
            resp.setCode(0);
            resp.setMsg("支付平台无响应");
            return resp;
        }
        boolean success = raw.getIntValue("code") == 0;
        resp.setCode(success ? PAY_CREATE_SUCCESS_CODE : 0);
        resp.setMsg(raw.getString("msg"));
        if (success) {
            String payInfo = raw.getString("pay_info");
            resp.setTradeNo(raw.getString("trade_no"));
            resp.setPayUrl(payInfo);
            resp.setQrcode(payInfo);
        }
        return resp;
    }

    private OrderCheckoutRespVO toCheckoutResp(String orderNo,
                                               String paymentNo,
                                               BigDecimal amount,
                                               String channel,
                                               PayResponse payResponse) {
        OrderPaymentInfo paymentInfo = new OrderPaymentInfo();
        paymentInfo.setChannel(channel);
        paymentInfo.setPayUrl(payResponse.getPayUrl());
        paymentInfo.setQrcode(StringUtils.defaultIfBlank(payResponse.getQrcode(), payResponse.getPayUrl()));

        OrderCheckoutRespVO resp = new OrderCheckoutRespVO();
        resp.setNeedPay(true);
        resp.setOrderNo(orderNo);
        resp.setPaymentNo(paymentNo);
        resp.setPayStatus("pending");
        resp.setPrice(amount);
        resp.setPointsUsed(0L);
        resp.setPointsDeductAmount(BigDecimal.ZERO);
        resp.setExpireTime(LocalDateTime.now().plusMinutes(resolvePayExpireMinutes()).toString());
        resp.setCloseExpireMinutes(resolvePayExpireMinutes());
        resp.setPayment(paymentInfo);
        return resp;
    }

    private PayChannelEnum resolvePaymentChannel(String channel) {
        PayChannelEnum channelEnum = PayChannelEnum.fromValue(channel);
        if (channelEnum == PayChannelEnum.WXPAY || channelEnum == PayChannelEnum.ALIPAY) {
            return channelEnum;
        }
        throw new ServiceException("不支持的支付方式: " + channel);
    }

    private String resolvePayCreateUrl() {
        if (StringUtils.isNotBlank(configuredPayCreateUrl)) {
            return configuredPayCreateUrl;
        }
        return DEFAULT_PAY_CREATE_URL;
    }

    private int resolvePayExpireMinutes() {
        return payConfig.EXPIRE_MINUTES == null || payConfig.EXPIRE_MINUTES <= 0
                ? DEFAULT_PAY_EXPIRE_MINUTES
                : payConfig.EXPIRE_MINUTES;
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeClientIp(String clientIp) {
        if (StringUtils.isBlank(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp.trim())) {
            return "127.0.0.1";
        }
        String ip = clientIp.trim();
        int commaIndex = ip.indexOf(',');
        if (commaIndex >= 0) {
            ip = ip.substring(0, commaIndex).trim();
        }
        return StringUtils.defaultIfBlank(ip, "127.0.0.1");
    }

    private Map<String, Object> payRequestSnapshot(String paymentNo,
                                                   String productName,
                                                   BigDecimal amount,
                                                   String clientIp,
                                                   String channel) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("paymentNo", paymentNo);
        snapshot.put("productName", productName);
        snapshot.put("amount", amount);
        snapshot.put("clientIp", normalizeClientIp(clientIp));
        snapshot.put("channel", channel);
        snapshot.put("method", PAY_METHOD_JUMP);
        return snapshot;
    }

    private String resolveErrorMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return StringUtils.defaultIfBlank(cause.getMessage(), e.getClass().getSimpleName());
    }
}
