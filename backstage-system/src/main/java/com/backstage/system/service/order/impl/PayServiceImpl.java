package com.backstage.system.service.order.impl;

import com.backstage.common.config.PayConfig;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.PaymentProviderEnum;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.payment.huifu.client.HuifuPayClient;
import com.backstage.system.payment.huifu.dto.HuifuClosePaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuCloseResult;
import com.backstage.system.payment.huifu.dto.HuifuCreatePaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuPaymentResult;
import com.backstage.system.payment.huifu.dto.HuifuQueryPaymentRequest;
import com.backstage.system.payment.huifu.dto.HuifuTransactionStatus;
import com.backstage.system.payment.huifu.exception.HuifuApiException;
import com.backstage.system.payment.huifu.exception.HuifuTransportException;
import com.backstage.system.payment.huifu.exception.HuifuProtocolException;
import com.backstage.system.payment.huifu.exception.HuifuSignatureException;
import com.backstage.system.service.order.PayService;
import com.backstage.system.utils.SignUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支付提供方路由。新订单按 pay.provider 选择平台；历史订单按 osh_payment.provider 回原平台查单。
 */
@Service
public class PayServiceImpl implements PayService {

    private static final Logger log = LoggerFactory.getLogger(PayServiceImpl.class);
    private static final String DEFAULT_NOTIFY_URL = "https://example.com/pay/notify-placeholder";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern DATED_PAYMENT_NO = Pattern.compile("^(?:P|GP|GRP)(\\d{8}).*$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private PayConfig payConfig;

    @Resource
    private HuifuPayClient huifuPayClient;

    @Override
    public String currentProvider() {
        PaymentProviderEnum provider = PaymentProviderEnum.fromValue(payConfig.PROVIDER);
        if (provider != PaymentProviderEnum.LEGACY && provider != PaymentProviderEnum.HUIFU) {
            throw new IllegalStateException("pay.provider 只允许配置为 legacy 或 huifu");
        }
        return provider.getValue();
    }

    @Override
    public PayResponse createPay(String outTradeNo,
                                 String name,
                                 String money,
                                 String clientIp,
                                 String channel) {
        return createPay(currentProvider(), outTradeNo, name, money, clientIp, channel);
    }

    @Override
    public PayResponse createPay(String provider,
                                 String outTradeNo,
                                 String name,
                                 String money,
                                 String clientIp,
                                 String channel) {
        PaymentProviderEnum providerEnum = PaymentProviderEnum.fromValue(provider);
        if (providerEnum != PaymentProviderEnum.LEGACY && providerEnum != PaymentProviderEnum.HUIFU) {
            throw new IllegalArgumentException("外部支付流水不支持 provider=" + provider);
        }
        String normalizedChannel = normalizePayChannel(channel);
        return providerEnum == PaymentProviderEnum.HUIFU
                ? createHuifuPay(outTradeNo, name, money, normalizedChannel)
                : createLegacyPay(outTradeNo, name, money, clientIp, normalizedChannel);
    }

    @Override
    public Map<String, String> queryPay(String outTradeNo, String provider, BigDecimal expectedAmount) {
        PaymentProviderEnum providerEnum = PaymentProviderEnum.fromValue(provider);
        if (providerEnum == PaymentProviderEnum.HUIFU) {
            return queryHuifuPay(outTradeNo, expectedAmount);
        }
        if (providerEnum == PaymentProviderEnum.LEGACY) {
            return queryLegacyPay(outTradeNo);
        }
        return null;
    }

    @Override
    public boolean closePay(String outTradeNo, String provider) {
        PaymentProviderEnum providerEnum = PaymentProviderEnum.fromValue(provider);
        if (providerEnum == PaymentProviderEnum.LEGACY) {
            // 旧平台没有可靠关单接口。绝不能假装成功后只关本地订单，否则旧二维码仍可付款，
            // 会形成“用户扣款但本地订单已关闭、无法发权益”。
            log.warn("旧支付平台不支持可靠关单，保持本地待支付, paymentNo={}", outTradeNo);
            return false;
        }
        if (providerEnum != PaymentProviderEnum.HUIFU) {
            return false;
        }
        try {
            HuifuClosePaymentRequest request = new HuifuClosePaymentRequest();
            request.setRequestDate(LocalDate.now().format(DATE));
            request.setRequestSequenceId(nextRequestSequence("C"));
            request.setOriginalRequestDate(resolveOriginalRequestDate(outTradeNo));
            request.setOriginalRequestSequenceId(outTradeNo);
            HuifuCloseResult result = huifuPayClient.closePayment(request);
            return result.getCloseStatus() == HuifuTransactionStatus.SUCCESS;
        } catch (Exception e) {
            log.warn("汇付关单失败, paymentNo={}, error={}", outTradeNo, resolveErrorMessage(e));
            return false;
        }
    }

    private PayResponse createHuifuPay(String outTradeNo, String name, String money, String channel) {
        try {
            LocalDateTime now = LocalDateTime.now();
            HuifuCreatePaymentRequest request = new HuifuCreatePaymentRequest();
            // req_date 是汇付幂等键的一部分，必须永远与 paymentNo 中固化的日期一致。
            // 跨日重试若改用当天日期，会生成第二笔上游订单且原查/关单无法覆盖。
            request.setRequestDate(resolveOriginalRequestDate(outTradeNo));
            request.setRequestSequenceId(outTradeNo);
            request.setTradeType(resolveHuifuTradeType(channel));
            request.setAmount(new BigDecimal(money).setScale(2, RoundingMode.UNNECESSARY));
            request.setGoodsDescription(truncate(StringUtils.defaultIfBlank(name, "OSH商品"), 40));
            request.setPrivateInfo(outTradeNo);
            request.setExpireTime(now.plusMinutes(resolveExpireMinutes()).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            HuifuPaymentResult result = huifuPayClient.createPayment(request);
            PayResponse response = new PayResponse();
            response.setCode(1);
            response.setMsg(StringUtils.defaultIfBlank(result.getResponseMessage(), "创建汇付正扫订单成功"));
            response.setOutTradeNo(outTradeNo);
            response.setTradeNo(StringUtils.defaultIfBlank(result.getHuifuSequenceId(), result.getPreOrderId()));
            response.setQrcode(result.getJumpUrl());
            response.setPayUrl(result.getJumpUrl());
            log.info("汇付正扫订单创建成功, paymentNo={}, tradeType={}, provider=huifu",
                    outTradeNo, result.getTradeType());
            return response;
        } catch (HuifuApiException e) {
            if ("00000100".equals(e.getResponseCode())) {
                log.warn("汇付正扫订单仍在处理, paymentNo={}", outTradeNo);
                PayResponse response = failedResponse(outTradeNo, "汇付响应处理中，请稍后查询原订单，请勿重复支付");
                response.setCode(-1);
                return response;
            }
            log.warn("汇付拒绝正扫下单, paymentNo={}, code={}", outTradeNo, e.getResponseCode());
            return failedResponse(outTradeNo, "汇付拒绝支付请求：" + resolveErrorMessage(e));
        } catch (HuifuTransportException | HuifuProtocolException | HuifuSignatureException e) {
            log.warn("汇付正扫订单响应不确定, paymentNo={}, error={}", outTradeNo, resolveErrorMessage(e));
            PayResponse response = failedResponse(outTradeNo, "汇付响应超时，订单状态确认中，请勿重复支付");
            response.setCode(-1);
            return response;
        } catch (Exception e) {
            log.warn("汇付正扫订单创建失败, paymentNo={}, error={}", outTradeNo, resolveErrorMessage(e));
            return failedResponse(outTradeNo, "汇付支付请求失败：" + resolveErrorMessage(e));
        }
    }

    private String resolveHuifuTradeType(String channel) {
        String normalizedChannel = normalizePayChannel(channel);
        PayChannelEnum channelEnum = PayChannelEnum.fromValue(normalizedChannel);
        if (channelEnum == PayChannelEnum.ALIPAY) {
            return "A_NATIVE";
        }
        throw new IllegalArgumentException("汇付暂不支持该支付方式：" + channel);
    }

    private String normalizePayChannel(String channel) {
        if (StringUtils.isBlank(channel)) {
            return PayChannelEnum.ALIPAY.getValue();
        }
        String normalized = channel.trim().toLowerCase();
        if (PayChannelEnum.ALIPAY.getValue().equals(normalized)
                || PayChannelEnum.WXPAY.getValue().equals(normalized)
                || "wechat".equals(normalized)
                || "union".equals(normalized)
                || "unionpay".equals(normalized)) {
            return PayChannelEnum.ALIPAY.getValue();
        }
        return normalized;
    }

    private Map<String, String> queryHuifuPay(String outTradeNo, BigDecimal expectedAmount) {
        try {
            HuifuQueryPaymentRequest request = new HuifuQueryPaymentRequest();
            request.setRequestDate(LocalDate.now().format(DATE));
            request.setRequestSequenceId(nextRequestSequence("Q"));
            request.setOriginalRequestDate(resolveOriginalRequestDate(outTradeNo));
            request.setOriginalRequestSequenceId(outTradeNo);
            request.setExpectedAmount(expectedAmount == null ? null : expectedAmount.setScale(2, RoundingMode.UNNECESSARY));

            HuifuPaymentResult result = huifuPayClient.queryPayment(request);
            if (result.isQueryPaidSuccessfully() && result.getAmount() == null) {
                throw new HuifuProtocolException("汇付查单成功但缺少交易金额");
            }
            Map<String, String> mapped = new HashMap<String, String>();
            mapped.put("status", mapHuifuQueryStatus(result));
            mapped.put("trade_no", StringUtils.defaultIfBlank(result.getHuifuSequenceId(), result.getPartyOrderId()));
            mapped.put("money", result.getAmount() == null ? null : result.getAmount().toPlainString());
            mapped.put("provider", PaymentProviderEnum.HUIFU.getValue());
            return mapped;
        } catch (Exception e) {
            log.warn("汇付查单失败, paymentNo={}, error={}", outTradeNo, resolveErrorMessage(e));
            return null;
        }
    }

    private String mapHuifuQueryStatus(HuifuPaymentResult result) {
        if (result.isQueryPaidSuccessfully()) {
            return "1";
        }
        if (HuifuTransactionStatus.SUCCESS == result.getCloseStatus()) {
            return "closed";
        }
        if (HuifuTransactionStatus.FAILED == result.getTransactionStatus()
                || "5".equals(result.getOrderStatus())) {
            return "failed";
        }
        if (HuifuTransactionStatus.PENDING == result.getCloseStatus()) {
            // 关单仍在处理中，不能当成普通未支付，更不能提前关闭本地订单。
            return "unknown";
        }
        if (HuifuTransactionStatus.PENDING == result.getTransactionStatus()
                && "2".equals(result.getOrderStatus())) {
            return "0";
        }
        // 支付中/处理中/初始均保持未知，避免超时任务误关正在入账的订单。
        return "unknown";
    }

    private PayResponse createLegacyPay(String outTradeNo,
                                        String name,
                                        String money,
                                        String clientIp,
                                        String channel) {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("pid", payConfig.PID);
            params.put("type", channel);
            params.put("out_trade_no", outTradeNo);
            params.put("notify_url", resolveNotifyUrl());
            params.put("return_url", PayConfig.RETURN_URL);
            params.put("name", name);
            params.put("money", money);
            params.put("clientip", clientIp);
            params.put("device", "pc");
            params.put("sign_type", "MD5");
            params.put("sign", SignUtil.createSign(params, payConfig.KEY));

            MultiValueMap<String, String> request = new LinkedMultiValueMap<String, String>();
            request.setAll(params);
            String raw = restTemplate().postForObject(payConfig.API_URL, request, String.class);
            PayResponse response = objectMapper.readValue(raw, PayResponse.class);
            log.info("旧支付平台下单完成, paymentNo={}, code={}", outTradeNo,
                    response == null ? null : response.getCode());
            return response;
        } catch (Exception e) {
            log.warn("旧支付平台下单失败, paymentNo={}, error={}", outTradeNo, resolveErrorMessage(e));
            return failedResponse(outTradeNo, "支付平台请求失败：" + resolveErrorMessage(e));
        }
    }

    private Map<String, String> queryLegacyPay(String outTradeNo) {
        try {
            String url = payConfig.STATUS_URL
                    + "?act=order&pid=" + payConfig.PID
                    + "&key=" + payConfig.KEY
                    + "&out_trade_no=" + outTradeNo;
            String result = restTemplate().getForObject(url, String.class);
            return objectMapper.readValue(result, new TypeReference<Map<String, String>>() { });
        } catch (Exception e) {
            log.warn("旧支付平台查单失败, paymentNo={}, error={}", outTradeNo, resolveErrorMessage(e));
            return null;
        }
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    private long resolveExpireMinutes() {
        return payConfig.EXPIRE_MINUTES == null || payConfig.EXPIRE_MINUTES <= 0
                ? 30L : payConfig.EXPIRE_MINUTES.longValue();
    }

    private String resolveOriginalRequestDate(String paymentNo) {
        Matcher matcher = paymentNo == null ? null : DATED_PAYMENT_NO.matcher(paymentNo);
        if (matcher != null && matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("汇付支付流水号缺少请求日期");
    }

    private String nextRequestSequence(String prefix) {
        return prefix + LocalDateTime.now().format(DATE_TIME)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private PayResponse failedResponse(String outTradeNo, String message) {
        PayResponse response = new PayResponse();
        response.setCode(0);
        response.setOutTradeNo(outTradeNo);
        response.setMsg(message);
        return response;
    }

    private String resolveNotifyUrl() {
        return StringUtils.defaultIfBlank(payConfig.NOTIFY_URL, DEFAULT_NOTIFY_URL);
    }

    private String resolveErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return StringUtils.defaultIfBlank(cause.getMessage(), exception.getClass().getSimpleName());
    }
}
