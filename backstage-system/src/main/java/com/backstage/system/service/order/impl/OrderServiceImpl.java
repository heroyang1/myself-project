package com.backstage.system.service.order.impl;

import com.backstage.common.config.PayConfig;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.ip.IpUtils;
import com.backstage.system.domain.payment.VerifiedPaymentNotification;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.domain.user.OshUserAssetRecord;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.order.OrderPaymentInfo;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.OshPaymentNotifyLogDO;
import com.backstage.system.domain.order.enums.NotifyProcessStatusEnum;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.order.enums.PaymentProviderEnum;
import com.backstage.system.domain.order.enums.PlatformPayQueryResult;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.order.OshPaymentNotifyLogMapper;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserAssetRecordMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.order.*;
import com.backstage.system.service.book.IBookService;
import com.backstage.system.service.order.OrderNoGenerator;
import com.backstage.system.service.order.PayService;
import com.backstage.system.service.order.OrderCancelHandlerRegistry;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.utils.SignUtil;
import com.backstage.system.utils.UserContextUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 统一订单服务实现，负责订单创建、支付查询和支付回调处理。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final int PAY_CREATE_SUCCESS_CODE = 1;
    private static final int PAY_CREATE_UNKNOWN_CODE = -1;
    private static final int SIGN_INVALID = 0;
    private static final int SIGN_VALID = 1;

    private static final int ORDER_PAID = OrderStatusEnum.PAID.getCode();
    private static final int PAYMENT_PENDING = PaymentStatusEnum.PENDING.getCode();
    private static final int PAYMENT_SUCCESS = PaymentStatusEnum.SUCCESS.getCode();
    private static final int PAYMENT_FAILED = PaymentStatusEnum.FAILED.getCode();
    private static final int PAYMENT_CLOSED = PaymentStatusEnum.CLOSED.getCode();
    private static final int CHANNEL_FREE = PayChannelEnum.FREE.getCode();
    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String PAY_CREATE_FAILED_MESSAGE = "发起支付失败";
    private static final int DEFAULT_PAY_EXPIRE_MINUTES = 30;
    private static final int ASSET_CHANGE_TYPE_INCOME = 0;
    private static final int ASSET_CHANGE_TYPE_EXPENSE = 1;
    private static final int ASSET_CHANGE_SOURCE_BUY_PRODUCT = 3;
    private static final BigDecimal POINTS_PER_YUAN = BigDecimal.TEN;
    private static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private PayConfig payConfig;

    @Resource
    private OshOrderMapper orderMapper;

    @Resource
    private OshPaymentMapper paymentMapper;

    @Resource
    private OshPaymentNotifyLogMapper notifyLogMapper;

    @Resource
    private PayService payService;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Resource
    private Validator validator;

    @Resource
    private OrderEntitlementTaskService entitlementTaskService;

    @Resource
    private PaymentSuccessEventPublisher paymentSuccessEventPublisher;

    @Resource
    private OrderCancelHandlerRegistry cancelHandlerRegistry;

    @Resource
    private OshUserAssetMapper oshUserAssetMapper;

    @Resource
    private OshUserAssetRecordMapper oshUserAssetRecordMapper;

    @Resource
    private RedisCache redisCache;

    @Resource
    private ContributionService contributionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建统一订单，并按金额返回免费完成结果或支付渠道参数。
     *
     * @param reqVO 结算参数
     * @return 订单结算结果
     */
    @Override
    public OrderCheckoutRespVO checkout(OrderCheckoutReqVO reqVO, Boolean enablePointDeduction) {

        String clientIp = reqVO.getClientIp();
        if (clientIp == null || clientIp.isEmpty()) {
            try {
                clientIp = IpUtils.getIpAddr();
            } catch (Exception e) {
                clientIp = "unknown";
            }
        }
        // 校验结算参数
        validateCheckoutParam(reqVO);

        // 准备订单号、金额和支付渠道
        String orderNo = orderNoGenerator.nextOrderNo(ProductTypeEnum.fromCode(reqVO.getProductType()).getName());
        String paymentNo = orderNoGenerator.nextPaymentNo();
        BigDecimal originalPayableAmount = money(reqVO.getPayableAmount());
        PayChannelEnum channelEnum = resolvePaymentChannel(reqVO.getChannel());
        int channelCode = channelEnum.getCode();

        // 创建本地待支付订单和支付流水，必要时在同一事务内扣减积分
        CheckoutCreateContext checkoutContext = createPendingOrderAndPayment(
                reqVO, enablePointDeduction, orderNo, paymentNo, originalPayableAmount, clientIp, channelCode);
        BigDecimal amount = checkoutContext.getCashPayAmount();
        OshPayment payment = checkoutContext.getPayment();
        PointDeduction pointDeduction = checkoutContext.getPointDeduction();

        // 免费订单直接完成支付状态
        if (isFreeAmount(amount)) {
            LocalDateTime now = LocalDateTime.now();
            completeFreePayment(orderNo, paymentNo, now);
            return freeCheckoutResult(orderNo, paymentNo, amount, pointDeduction);
        }

        // 请求支付渠道创建付款信息
        PayResponse payResponse = requestPayment(orderNo, paymentNo, reqVO.getProductName(), amount, clientIp, channelEnum.getValue());
        log.info("支付平台下单完成, provider={}, paymentNo={}, code={}",
                payment.getProvider(), paymentNo, payResponse == null ? null : payResponse.getCode());
        // 保存支付渠道响应
        savePayResponse(paymentNo, payResponse);

        // 支付渠道创建失败时关闭本地订单
        if (!isPayCreateSuccess(payResponse)) {
            if (payResponse.getCode() != PAY_CREATE_UNKNOWN_CODE) {
                failPaymentAndCloseOrder(paymentNo, orderNo, payResponse);
            }

            throw new ServiceException(StringUtils.defaultIfBlank(payResponse.getMsg(), PAY_CREATE_FAILED_MESSAGE));
        }

        // 组装支付参数返回前端
        return packageOrderCheckoutResult(orderNo, paymentNo, amount, channelEnum.getValue(), payResponse, payment, pointDeduction);
    }

    @Override
    public PayResponse createPaymentForExistingOrder(String orderNo,
                                                     String paymentNo,
                                                     String clientIp,
                                                     String channel) {
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        OshPayment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (order == null || payment == null || !Objects.equals(payment.getOrderNo(), orderNo)) {
            throw new ServiceException("统一订单或支付流水不存在");
        }
        if (!Objects.equals(order.getStatus(), OrderStatusEnum.PENDING.getCode())
                || !Objects.equals(payment.getStatus(), PAYMENT_PENDING)) {
            throw new ServiceException("当前订单状态不允许发起支付");
        }
        if (payment.getExpireTime() != null && !payment.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException("支付订单已超时，请重新发起");
        }
        if (StringUtils.isNotBlank(payment.getQrcode()) || StringUtils.isNotBlank(payment.getPayUrl())) {
            PayResponse cached = new PayResponse();
            cached.setCode(PAY_CREATE_SUCCESS_CODE);
            cached.setMsg("复用原支付二维码");
            cached.setOutTradeNo(paymentNo);
            cached.setTradeNo(payment.getPlatformTradeNo());
            cached.setQrcode(payment.getQrcode());
            cached.setPayUrl(payment.getPayUrl());
            return cached;
        }

        PayResponse response;
        try {
            response = payService.createPay(
                    payment.getProvider(),
                    paymentNo,
                    StringUtils.defaultIfBlank(order.getProductName(), "OSH商品"),
                    money(payment.getAmount()).toPlainString(),
                    clientIp,
                    resolvePaymentChannel(channel).getValue()
            );
        } catch (RuntimeException e) {
            log.warn("已有订单发起支付异常, orderNo={}, paymentNo={}", orderNo, paymentNo, e);
            response = failedPayResponse();
        }
        if (response == null) {
            response = failedPayResponse();
        }
        savePayResponse(paymentNo, response);
        if (!isPayCreateSuccess(response) && response.getCode() != PAY_CREATE_UNKNOWN_CODE) {
            failPaymentAndCloseOrder(paymentNo, orderNo, response);
        }
        return response;
    }

    /**
     * 根据订单号查询订单和支付状态。
     *
     * @param orderNo 订单号
     * @return 订单状态结果
     */
    @Override
    public OrderStatusResult getOrderStatus(String orderNo) {
        // 查询订单主信息
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (Objects.isNull(order)) {
            throw new ServiceException("订单不存在");
        }

        // 查询订单关联的支付流水
        OshPayment payment = paymentMapper.selectByOrderNo(orderNo);
        if (Objects.nonNull(payment) && Objects.nonNull(payment.getStatus()) && payment.getStatus() == PAYMENT_PENDING) {
            tryRefreshFromPlatform(payment);
            order = orderMapper.selectByOrderNo(orderNo);
            payment = paymentMapper.selectByOrderNo(orderNo);
        }

        // 组装统一订单状态
        return toStatusResult(order, payment);
    }

    @Override
    public OrderStatusResult getOrderStatusForUser(String orderNo, Long userId) {
        OshOrder order = requireUserOrder(orderNo, userId);
        OshPayment payment = paymentMapper.selectByOrderNo(orderNo);
        if (Objects.nonNull(payment) && Objects.nonNull(payment.getStatus()) && payment.getStatus() == PAYMENT_PENDING) {
            tryRefreshFromPlatform(payment);
            order = orderMapper.selectByOrderNo(orderNo);
            payment = paymentMapper.selectByOrderNo(orderNo);
        }
        return toStatusResult(order, payment);
    }

    /**
     * 根据支付流水号查询订单和支付状态。
     *
     * @param paymentNo 支付流水号
     * @return 订单状态结果
     */
    @Override
    public OrderStatusResult getPaymentStatus(String paymentNo) {
        // 查询支付流水
        OshPayment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (Objects.isNull(payment)) {
            throw new ServiceException("支付流水不存在");
        }

        // 本地还在待支付状态时，主动查询支付平台
        if (Objects.nonNull(payment.getStatus()) && payment.getStatus() == PAYMENT_PENDING) {
            tryRefreshFromPlatform(payment);
            payment = paymentMapper.selectByPaymentNo(paymentNo);
        }

        // 查询支付流水关联的订单
        OshOrder order = orderMapper.selectByOrderNo(payment.getOrderNo());

        // 组装统一订单状态
        return toStatusResult(order, payment);
    }

    /**
     * 取消支付，关闭支付流水和订单。
     *
     * @param paymentNo 支付流水号
     */
    @Override
    public void cancelPayment(String paymentNo) {
        OshPayment payment = paymentMapper.selectByPaymentNo(paymentNo);
        cancelPendingPayment(payment);
    }

    /**
     * 按订单号取消支付，前端业务侧只感知订单号。
     *
     * @param orderNo 订单号
     */
    @Override
    public void cancelPaymentByOrderNo(String orderNo) {
        OshPayment payment = paymentMapper.selectByOrderNo(orderNo);
        cancelPendingPayment(payment);
    }

    @Override
    public void cancelPaymentByOrderNoForUser(String orderNo, Long userId) {
        requireUserOrder(orderNo, userId);
        cancelPaymentByOrderNo(orderNo);
    }

    /**
     * 处理支付平台回调，并推进本地支付流水和订单状态。
     *
     * @param params 支付平台回调参数
     * @return 回调是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePayNotify(Map<String, String> params) {
        String paymentNo = params.get("out_trade_no");
        String platformTradeNo = params.get("trade_no");
        boolean signValid = isSignValid(params);

        // 记录原始支付回调
        OshPaymentNotifyLogDO notifyLog = buildNotifyLog(paymentNo, platformTradeNo, params, signValid);
        notifyLogMapper.insertOshPaymentNotifyLog(notifyLog);

        // 校验回调签名
        if (!signValid) {
            return markNotifyFailed(notifyLog, SIGN_INVALID, "签名校验失败");
        }
        if (!Objects.equals(payConfig.PID, params.get("pid"))) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "旧支付商户号不一致");
        }

        // 过滤非支付成功状态
        if (!TRADE_SUCCESS.equals(params.get("trade_status"))) {
            return markNotifyIgnored(notifyLog, "非支付成功状态", false);
        }

        // 查询支付流水
        OshPayment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (Objects.isNull(payment)) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付流水不存在");
        }
        notifyLog.setOrderNo(payment.getOrderNo());
        if (!PaymentProviderEnum.LEGACY.getValue().equals(payment.getProvider())) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付提供方不一致");
        }

        // 校验支付金额
        if (!sameMoney(params.get("money"), payment.getAmount())) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付金额不一致");
        }

        // 处理重复回调
        if (Objects.nonNull(payment.getStatus()) && payment.getStatus() == PAYMENT_SUCCESS) {
            handleOrderProductPaid(payment.getOrderNo());
            return markNotifyIgnored(notifyLog, "重复回调", true);
        }

        // 推进支付流水和订单状态
        return completePaidNotify(paymentNo, platformTradeNo, payment, notifyLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleVerifiedPayNotify(VerifiedPaymentNotification notification) {
        if (notification == null || StringUtils.isBlank(notification.getPaymentNo())) {
            return false;
        }

        OshPaymentNotifyLogDO notifyLog = buildVerifiedNotifyLog(notification);
        notifyLogMapper.insertOshPaymentNotifyLog(notifyLog);

        OshPayment payment = paymentMapper.selectByPaymentNo(notification.getPaymentNo());
        if (Objects.isNull(payment)) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付流水不存在");
        }
        notifyLog.setOrderNo(payment.getOrderNo());

        if (!Objects.equals(payment.getProvider(), notification.getProvider())) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付提供方不一致");
        }
        if (notification.getAmount() == null
                || notification.getAmount().compareTo(money(payment.getAmount())) != 0) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付金额不一致");
        }
        if (Objects.equals(payment.getStatus(), PAYMENT_SUCCESS)) {
            handleOrderProductPaid(payment.getOrderNo());
            return markNotifyIgnored(notifyLog, "重复回调", true);
        }

        return completePaidNotify(
                payment.getPaymentNo(),
                notification.getPlatformTradeNo(),
                payment,
                notifyLog,
                notification.getChannel()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleVerifiedPayFailure(VerifiedPaymentNotification notification) {
        if (notification == null || StringUtils.isBlank(notification.getPaymentNo())) {
            return false;
        }

        OshPaymentNotifyLogDO notifyLog = buildVerifiedNotifyLog(notification);
        notifyLogMapper.insertOshPaymentNotifyLog(notifyLog);

        OshPayment payment = paymentMapper.selectByPaymentNo(notification.getPaymentNo());
        if (payment == null) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付流水不存在");
        }
        notifyLog.setOrderNo(payment.getOrderNo());
        if (!Objects.equals(payment.getProvider(), notification.getProvider())) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付提供方不一致");
        }
        if (notification.getAmount() != null
                && notification.getAmount().compareTo(money(payment.getAmount())) != 0) {
            return markNotifyFailed(notifyLog, SIGN_VALID, "支付金额不一致");
        }
        if (Objects.equals(payment.getStatus(), PAYMENT_SUCCESS)) {
            handleOrderProductPaid(payment.getOrderNo());
            return markNotifyIgnored(notifyLog, "支付已成功，忽略迟到的失败通知", true);
        }
        if (!Objects.equals(payment.getStatus(), PAYMENT_PENDING)) {
            return markNotifyIgnored(notifyLog, "重复失败通知", true);
        }

        OshOrder order = orderMapper.selectByOrderNo(payment.getOrderNo());
        int paymentUpdated = paymentMapper.updatePendingToFailed(
                payment.getPaymentNo(), toJson(notification.getSafePayload()));
        if (paymentUpdated != 1) {
            return handlePaymentUpdateConflict(payment.getPaymentNo(), notifyLog);
        }
        int orderUpdated = orderMapper.updatePendingToClosed(payment.getOrderNo(), LocalDateTime.now());
        if (orderUpdated != 1) {
            throw new ServiceException("支付失败时订单状态并发冲突");
        }
        refundOrderPoints(payment.getOrderNo());
        markNotifyLog(notifyLog, SIGN_VALID, NotifyProcessStatusEnum.SUCCESS.getCode(), null);
        registerAfterCommit(() -> handleOrderProductCanceled(order, payment.getPaymentNo()));
        return true;
    }

    /**
     * 构建待支付订单实体。
     *
     * @param param 结算参数
     * @param orderNo 订单号
     * @param amount 订单金额
     * @return 待支付订单
     */
    private OshOrder buildPendingOrder(OrderCheckoutReqVO param,
                                       String orderNo,
                                       BigDecimal amount,
                                       PointDeduction pointDeduction) {
        OshOrder order = new OshOrder();
        order.setUserId(param.getUserId());
        order.setOrderNo(orderNo);
        order.setStatus(OrderStatusEnum.PENDING.getCode());
        order.setProductType(param.getProductType());
        order.setProductId(param.getProductId());
        order.setProductName(param.getProductName());
        order.setPurchaseMode(param.getPurchaseMode());
        order.setActivityId(param.getActivityId());
        order.setOriginalAmount(money(param.getOriginalAmount()));
        order.setDiscountAmount(param.getDiscountAmount());
        order.setPayableAmount(amount);
        order.setPointsAmount(pointDeduction.getPointsUsed());
        order.setPointsDeductAmount(pointDeduction.getDeductAmount());
        order.setCouponId(param.getCouponId());
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        order.setDeleteFlag(0);
        return order;
    }

    /**
     * 构建待支付支付流水实体。
     *
     * @param orderNo 订单号
     * @param paymentNo 支付流水号
     * @param amount 支付金额
     * @param param 结算参数
     * @param clientIp 客户端 IP
     * @param channelCode 支付渠道
     * @return 待支付支付流水
     */
    private OshPayment buildPendingPayment(String orderNo,
                                           String paymentNo,
                                           BigDecimal amount,
                                           OrderCheckoutReqVO param,
                                           String clientIp,
                                           int channelCode) {
        OshPayment payment = new OshPayment();
        payment.setOrderNo(orderNo);
        payment.setPaymentNo(paymentNo);
        payment.setChannel(isFreeAmount(amount) ? CHANNEL_FREE : channelCode);
        payment.setProvider(isFreeAmount(amount)
                ? PaymentProviderEnum.INTERNAL.getValue()
                : payService.currentProvider());
        payment.setAmount(amount);
        payment.setStatus(PAYMENT_PENDING);
        payment.setRequestPayload(toJson(payRequestSnapshot(paymentNo, param, clientIp, channelCode, amount)));
        payment.setExpireTime(LocalDateTime.now().plusMinutes(resolvePayExpireMinutes()));
        payment.setCreatedTime(LocalDateTime.now());
        payment.setUpdatedTime(LocalDateTime.now());
        payment.setDeleteFlag(0);
        return payment;
    }

    /**
     * 在本地事务中写入订单和支付流水。
     *
     * @param order 待写入订单
     * @param payment 待写入支付流水
     */
    private CheckoutCreateContext createPendingOrderAndPayment(OrderCheckoutReqVO reqVO,
                                                               Boolean enablePointDeduction,
                                                               String orderNo,
                                                               String paymentNo,
                                                               BigDecimal originalPayableAmount,
                                                               String clientIp,
                                                               int channelCode) {
        CheckoutCreateContext context = new CheckoutCreateContext();
        executeInTransaction(() -> {
            PointDeduction pointDeduction = deductOrderPointsIfNeeded(reqVO, enablePointDeduction, originalPayableAmount, orderNo);
            BigDecimal cashPayAmount = originalPayableAmount.subtract(pointDeduction.getDeductAmount());
            cashPayAmount = money(cashPayAmount);

            OshOrder order = buildPendingOrder(reqVO, orderNo, cashPayAmount, pointDeduction);
            OshPayment payment = buildPendingPayment(orderNo, paymentNo, cashPayAmount, reqVO, clientIp, channelCode);
            orderMapper.insertOshOrder(order);
            payment.setOrderId(order.getId());
            paymentMapper.insertOshPayment(payment);
            context.setOrder(order);
            context.setPayment(payment);
            context.setCashPayAmount(cashPayAmount);
            context.setPointDeduction(pointDeduction);
        });
        return context;
    }

    /**
     * 按订单金额扣减积分，返回本次抵扣快照。
     */
    private PointDeduction deductOrderPointsIfNeeded(OrderCheckoutReqVO reqVO,
                                                     Boolean enablePointDeduction,
                                                     BigDecimal originalPayableAmount,
                                                     String orderNo) {
        if (!Boolean.TRUE.equals(enablePointDeduction) || isFreeAmount(originalPayableAmount)) {
            return PointDeduction.none(originalPayableAmount);
        }

        OshUserAsset userAsset = oshUserAssetMapper.selectByUserIdForUpdate(reqVO.getUserId());
        if (Objects.isNull(userAsset) || Objects.isNull(userAsset.getPoints()) || userAsset.getPoints() <= 0) {
            return PointDeduction.none(originalPayableAmount);
        }

        long beforeBalance = userAsset.getPoints();
        long maxDeductiblePoints = originalPayableAmount.multiply(POINTS_PER_YUAN)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long pointsUsed = Math.min(beforeBalance, maxDeductiblePoints);
        if (pointsUsed <= 0) {
            return PointDeduction.none(originalPayableAmount);
        }

        BigDecimal deductAmount = BigDecimal.valueOf(pointsUsed)
                .divide(POINTS_PER_YUAN, 2, RoundingMode.DOWN);
        long afterBalance = beforeBalance - pointsUsed;
        userAsset.setPoints(afterBalance);
        oshUserAssetMapper.updateById(userAsset);
        insertAssetRecord(reqVO.getUserId(), ASSET_CHANGE_TYPE_EXPENSE, pointsUsed, beforeBalance, afterBalance,
                "订单【" + orderNo + "】积分抵扣");
        registerAssetCacheRefreshAfterCommit(reqVO.getUserId(), afterBalance);
        return new PointDeduction(pointsUsed, deductAmount, afterBalance);
    }

    /**
     * 本地轮询时主动查询支付平台，若已支付则推进本地状态。
     */
    private void tryRefreshFromPlatform(OshPayment payment) {
        reconcilePaymentFromPlatform(payment);
    }

    @Override
    public PlatformPayQueryResult reconcilePaymentFromPlatform(String paymentNo) {
        OshPayment payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (Objects.isNull(payment)) {
            log.warn("对账跳过，支付流水不存在, paymentNo={}", paymentNo);
            return PlatformPayQueryResult.UNKNOWN;
        }
        return reconcilePaymentFromPlatform(payment);
    }

    @Override
    public PlatformPayQueryResult reconcilePaymentFromPlatform(OshPayment payment) {
        if (Objects.isNull(payment)) {
            return PlatformPayQueryResult.UNKNOWN;
        }
        if (Objects.equals(payment.getStatus(), PAYMENT_SUCCESS)) {
            handleOrderProductPaid(payment.getOrderNo());
            return PlatformPayQueryResult.PAID;
        }
        if (Objects.equals(payment.getStatus(), PAYMENT_CLOSED)) {
            return PlatformPayQueryResult.CLOSED;
        }
        if (Objects.equals(payment.getStatus(), PAYMENT_FAILED)) {
            return PlatformPayQueryResult.FAILED;
        }
        if (!Objects.equals(payment.getStatus(), PAYMENT_PENDING)) {
            return PlatformPayQueryResult.UNKNOWN;
        }

        Map<String, String> platformResult;
        try {
            platformResult = payService.queryPay(
                    payment.getPaymentNo(),
                    payment.getProvider(),
                    payment.getAmount()
            );
        } catch (Exception e) {
            log.warn("主动查询支付平台异常, paymentNo={}", payment.getPaymentNo(), e);
            return PlatformPayQueryResult.UNKNOWN;
        }
        if (Objects.isNull(platformResult)) {
            return PlatformPayQueryResult.UNKNOWN;
        }

        String tradeStatus = platformResult.get("status");
        if ("closed".equals(tradeStatus)) {
            return closeTerminalPaymentFromPlatform(payment);
        }
        if ("failed".equals(tradeStatus)) {
            return failTerminalPaymentFromPlatform(payment, platformResult);
        }
        if (!"1".equals(tradeStatus)) {
            return "0".equals(tradeStatus) ? PlatformPayQueryResult.UNPAID : PlatformPayQueryResult.UNKNOWN;
        }
        if (StringUtils.isNotBlank(platformResult.get("money"))
                && !sameMoney(platformResult.get("money"), payment.getAmount())) {
            log.error("支付平台查单金额不一致，拒绝补单, paymentNo={}, orderNo={}",
                    payment.getPaymentNo(), payment.getOrderNo());
            return PlatformPayQueryResult.UNKNOWN;
        }

        LocalDateTime paidTime = LocalDateTime.now();
        String platformTradeNo = platformResult.get("trade_no");
        boolean paymentUpdated = Boolean.TRUE.equals(executeInTransaction(() -> {
            int updated = paymentMapper.updatePendingToSuccess(payment.getPaymentNo(), platformTradeNo, paidTime, null);
            if (updated == 0) {
                return false;
            }
            int orderUpdated = orderMapper.updatePendingToPaid(payment.getOrderNo(), paidTime);
            if (orderUpdated != 1) {
                throw new ServiceException("补单时订单状态并发冲突");
            }
            handleOrderProductPaid(payment.getOrderNo());
            return true;
        }));
        if (!paymentUpdated) {
            log.info("主动查单发现订单已被处理，跳过重复权益发放, paymentNo={}, orderNo={}",
                    payment.getPaymentNo(), payment.getOrderNo());
        }
        return PlatformPayQueryResult.PAID;
    }

    private PlatformPayQueryResult closeTerminalPaymentFromPlatform(OshPayment payment) {
        OshOrder order = orderMapper.selectByOrderNo(payment.getOrderNo());
        return executeInTransaction(() -> {
            int paymentUpdated = paymentMapper.updatePendingToClosed(payment.getPaymentNo());
            if (paymentUpdated != 1) {
                OshPayment latest = paymentMapper.selectByPaymentNo(payment.getPaymentNo());
                if (latest != null && Objects.equals(latest.getStatus(), PAYMENT_SUCCESS)) {
                    return PlatformPayQueryResult.PAID;
                }
                if (latest != null && Objects.equals(latest.getStatus(), PAYMENT_CLOSED)) {
                    return PlatformPayQueryResult.CLOSED;
                }
                if (latest != null && Objects.equals(latest.getStatus(), PAYMENT_FAILED)) {
                    return PlatformPayQueryResult.FAILED;
                }
                return PlatformPayQueryResult.UNKNOWN;
            }
            if (orderMapper.updatePendingToClosed(payment.getOrderNo(), LocalDateTime.now()) != 1) {
                throw new ServiceException("同步平台关单状态时发生并发冲突");
            }
            refundOrderPoints(payment.getOrderNo());
            registerAfterCommit(() -> handleOrderProductCanceled(order, payment.getPaymentNo()));
            return PlatformPayQueryResult.CLOSED;
        });
    }

    private PlatformPayQueryResult failTerminalPaymentFromPlatform(OshPayment payment,
                                                                    Map<String, String> platformResult) {
        OshOrder order = orderMapper.selectByOrderNo(payment.getOrderNo());
        return executeInTransaction(() -> {
            int paymentUpdated = paymentMapper.updatePendingToFailed(
                    payment.getPaymentNo(), toJson(platformResult));
            if (paymentUpdated != 1) {
                OshPayment latest = paymentMapper.selectByPaymentNo(payment.getPaymentNo());
                if (latest != null && Objects.equals(latest.getStatus(), PAYMENT_SUCCESS)) {
                    return PlatformPayQueryResult.PAID;
                }
                if (latest != null && (Objects.equals(latest.getStatus(), PAYMENT_FAILED)
                        || Objects.equals(latest.getStatus(), PAYMENT_CLOSED))) {
                    return PlatformPayQueryResult.FAILED;
                }
                return PlatformPayQueryResult.UNKNOWN;
            }
            if (orderMapper.updatePendingToClosed(payment.getOrderNo(), LocalDateTime.now()) != 1) {
                throw new ServiceException("平台失败终态关闭订单时发生并发冲突");
            }
            refundOrderPoints(payment.getOrderNo());
            registerAfterCommit(() -> handleOrderProductCanceled(order, payment.getPaymentNo()));
            return PlatformPayQueryResult.FAILED;
        });
    }

    /**
     * 免费订单直接完成支付流水和订单状态。
     *
     * @param orderNo 订单号
     * @param paymentNo 支付流水号
     * @param paidTime 支付完成时间
     */
    private void completeFreePayment(String orderNo, String paymentNo, LocalDateTime paidTime) {
        executeInTransaction(() -> {
            if (paymentMapper.updatePendingToSuccess(paymentNo, null, paidTime, null) != 1) {
                throw new ServiceException("免费订单支付流水状态更新失败");
            }
            if (orderMapper.updatePendingToPaid(orderNo, paidTime) != 1) {
                throw new ServiceException("免费订单状态更新失败");
            }
            handleOrderProductPaid(orderNo);
        });
    }

    /**
     * 请求支付渠道创建支付参数。
     *
     * @param orderNo 订单号
     * @param paymentNo 支付流水号
     * @param productName 商品名称
     * @param amount 支付金额
     * @param clientIp 客户端 IP
     * @return 支付渠道响应，异常或空响应时返回失败响应
     */
    private PayResponse requestPayment(String orderNo,
                                       String paymentNo,
                                       String productName,
                                       BigDecimal amount,
                                       String clientIp,
                                       String channelValue) {
        try {
            PayResponse response = payService.createPay(paymentNo, productName, amount.toPlainString(), clientIp, channelValue);
            return Objects.isNull(response) ? failedPayResponse() : response;
        } catch (RuntimeException e) {
            log.warn("发起支付异常, orderNo={}, paymentNo={}", orderNo, paymentNo, e);
            return failedPayResponse();
        }
    }

    /**
     * 保存支付渠道返回的交易号、链接和二维码。
     *
     * @param paymentNo 支付流水号
     * @param payResponse 支付渠道响应
     */
    private void savePayResponse(String paymentNo, PayResponse payResponse) {
        executeInTransaction(() -> {
            paymentMapper.updatePayResponse(
                    paymentNo,
                    payResponse.getTradeNo(),
                    payResponse.getPayUrl(),
                    payResponse.getQrcode(),
                    toJson(payResponse)
            );
        });
    }

    /**
     * 支付创建失败时关闭订单并标记支付流水失败。
     *
     * @param paymentNo 支付流水号
     * @param orderNo 订单号
     * @param payResponse 支付渠道响应
     */
    private void failPaymentAndCloseOrder(String paymentNo, String orderNo, PayResponse payResponse) {
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        executeInTransaction(() -> {
            int paymentUpdated = paymentMapper.updatePendingToFailed(paymentNo, toJson(payResponse));
            if (paymentUpdated != 1) {
                throw new ServiceException("支付创建失败时支付流水状态已变化");
            }
            int orderUpdated = orderMapper.updatePendingToClosed(orderNo, LocalDateTime.now());
            if (orderUpdated != 1) {
                throw new ServiceException("支付创建失败时订单状态已变化");
            }
            refundOrderPoints(orderNo);
        });
        handleOrderProductCanceled(order, paymentNo);
    }

    /**
     * 执行本地事务操作。
     *
     * @param operation 需要纳入事务的操作
     */
    private void executeInTransaction(Runnable operation) {
        new TransactionTemplate(transactionManager).execute(status -> {
            operation.run();
            return null;
        });
    }

    /**
     * 执行本地事务操作，并返回事务内计算结果。
     *
     * @param operation 需要纳入事务的操作
     * @param <T> 返回结果类型
     * @return 事务内操作返回值
     */
    private <T> T executeInTransaction(Supplier<T> operation) {
        return new TransactionTemplate(transactionManager).execute(status -> operation.get());
    }

    /**
     * 关闭待支付流水和关联订单。
     *
     * @param payment 待关闭支付流水
     */
    private void cancelPendingPayment(OshPayment payment) {
        if (Objects.isNull(payment)) {
            throw new ServiceException("支付流水不存在");
        }
        if (payment.getStatus() != PAYMENT_PENDING) {
            throw new ServiceException("当前支付状态不可取消");
        }
        if (!PaymentProviderEnum.INTERNAL.getValue().equals(payment.getProvider())) {
            PlatformPayQueryResult queryResult = reconcilePaymentFromPlatform(payment);
            if (queryResult == PlatformPayQueryResult.PAID) {
                throw new ServiceException("订单已支付，不能取消");
            }
            if (queryResult == PlatformPayQueryResult.FAILED) {
                return;
            }
            if (queryResult == PlatformPayQueryResult.CLOSED) {
                return;
            }
            if (queryResult == PlatformPayQueryResult.UNKNOWN) {
                throw new ServiceException("支付平台状态暂时未知，稍后重试关闭");
            }
            if (!payService.closePay(payment.getPaymentNo(), payment.getProvider())) {
                throw new ServiceException("支付平台关单失败，未关闭本地订单");
            }
            PlatformPayQueryResult afterClose = reconcilePaymentFromPlatform(
                    paymentMapper.selectByPaymentNo(payment.getPaymentNo()));
            if (afterClose == PlatformPayQueryResult.PAID) {
                throw new ServiceException("关单后确认订单已支付，不能关闭本地订单");
            }
            if (afterClose == PlatformPayQueryResult.FAILED) {
                return;
            }
            if (afterClose == PlatformPayQueryResult.CLOSED) {
                return;
            }
            if (afterClose == PlatformPayQueryResult.UNKNOWN) {
                throw new ServiceException("关单后平台状态暂时未知，保持待支付并稍后重试");
            }
        }
        String paymentNo = payment.getPaymentNo();
        OshOrder order = orderMapper.selectByOrderNo(payment.getOrderNo());
        boolean closed = Boolean.TRUE.equals(executeInTransaction(() -> {
            int paymentUpdated = paymentMapper.updatePendingToClosed(paymentNo);
            if (paymentUpdated != 1) {
                return false;
            }
            int orderUpdated = orderMapper.updatePendingToClosed(payment.getOrderNo(), LocalDateTime.now());
            if (orderUpdated != 1) {
                throw new ServiceException("取消支付时订单状态并发冲突");
            }
            refundOrderPoints(payment.getOrderNo());
            return true;
        }));
        if (closed) {
            handleOrderProductCanceled(order, paymentNo);
        }
    }

    /**
     * 执行订单取消后的业务释放处理。
     *
     * @param order 统一订单
     * @param paymentNo 支付流水号
     */
    private void handleOrderProductCanceled(OshOrder order, String paymentNo) {
        if (Objects.isNull(order)) {
            log.warn("订单取消后置处理跳过，统一订单不存在, paymentNo={}", paymentNo);
            return;
        }
        ProductTypeEnum productType = ProductTypeEnum.fromCode(order.getProductType());
        if (Objects.isNull(productType)) {
            log.warn("订单取消后置处理跳过，商品类型非法, orderNo={}, productType={}",
                    order.getOrderNo(), order.getProductType());
            return;
        }
        cancelHandlerRegistry.handleIfPresent(productType.getName(), order.getOrderNo(), paymentNo);
    }

    /**
     * 退回待支付订单已扣减积分。
     */
    private void refundOrderPoints(String orderNo) {
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (Objects.isNull(order) || Objects.isNull(order.getPointsAmount()) || order.getPointsAmount() <= 0) {
            return;
        }
        OshUserAsset userAsset = oshUserAssetMapper.selectByUserIdForUpdate(order.getUserId());
        if (Objects.isNull(userAsset)) {
            throw new ServiceException("用户资产不存在，无法退回订单积分，orderNo=" + orderNo + ", userId=" + order.getUserId());
        }
        long beforeBalance = Objects.isNull(userAsset.getPoints()) ? 0L : userAsset.getPoints();
        long refundPoints = order.getPointsAmount();
        long afterBalance = beforeBalance + refundPoints;
        userAsset.setPoints(afterBalance);
        oshUserAssetMapper.updateById(userAsset);
        insertAssetRecord(order.getUserId(), ASSET_CHANGE_TYPE_INCOME, refundPoints, beforeBalance, afterBalance,
                "取消订单【" + orderNo + "】退回积分");
        registerAssetCacheRefreshAfterCommit(order.getUserId(), afterBalance);
    }

    /**
     * 记录用户资产变动流水。
     */
    private void insertAssetRecord(Long userId,
                                   int changeType,
                                   long changeAmount,
                                   long beforeBalance,
                                   long afterBalance,
                                   String remark) {
        OshUserAssetRecord assetRecord = new OshUserAssetRecord();
        assetRecord.setUserId(userId);
        assetRecord.setChangeType(changeType);
        assetRecord.setChangeSource(ASSET_CHANGE_SOURCE_BUY_PRODUCT);
        assetRecord.setChangeAmount(changeAmount);
        assetRecord.setBeforeBalance(beforeBalance);
        assetRecord.setAfterBalance(afterBalance);
        assetRecord.setRemark(remark);
        oshUserAssetRecordMapper.insert(assetRecord);
    }

    /**
     * 构建支付回调日志。
     *
     * @param paymentNo 支付流水号
     * @param platformTradeNo 平台交易号
     * @param params 回调原始参数
     * @param signValid 签名是否有效
     * @return 支付回调日志
     */
    private OshPaymentNotifyLogDO buildNotifyLog(String paymentNo,
                                                 String platformTradeNo,
                                                 Map<String, String> params,
                                                 boolean signValid) {
        OshPaymentNotifyLogDO notifyLog = new OshPaymentNotifyLogDO();
        notifyLog.setPaymentNo(paymentNo);
        notifyLog.setPlatformTradeNo(platformTradeNo);
        notifyLog.setNotifyPayload(toJson(sanitizeLegacyNotify(params)));
        notifyLog.setSignValid(signValid ? SIGN_VALID : SIGN_INVALID);
        notifyLog.setProcessStatus(NotifyProcessStatusEnum.RECEIVED.getCode());
        notifyLog.setCreatedTime(new Date());
        return notifyLog;
    }

    private Map<String, String> sanitizeLegacyNotify(Map<String, String> params) {
        Map<String, String> safe = new LinkedHashMap<>();
        if (params == null) {
            return safe;
        }
        copyIfPresent(params, safe, "out_trade_no");
        copyIfPresent(params, safe, "trade_no");
        copyIfPresent(params, safe, "trade_status");
        copyIfPresent(params, safe, "type");
        copyIfPresent(params, safe, "money");
        copyIfPresent(params, safe, "pid");
        return safe;
    }

    private void copyIfPresent(Map<String, String> source, Map<String, String> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private OshPaymentNotifyLogDO buildVerifiedNotifyLog(VerifiedPaymentNotification notification) {
        OshPaymentNotifyLogDO notifyLog = new OshPaymentNotifyLogDO();
        notifyLog.setPaymentNo(notification.getPaymentNo());
        notifyLog.setPlatformTradeNo(notification.getPlatformTradeNo());
        notifyLog.setNotifyPayload(toJson(notification.getSafePayload()));
        notifyLog.setSignValid(SIGN_VALID);
        notifyLog.setProcessStatus(NotifyProcessStatusEnum.RECEIVED.getCode());
        notifyLog.setCreatedTime(new Date());
        return notifyLog;
    }

    /**
     * 完成支付成功回调的本地状态推进。
     *
     * @param paymentNo 支付流水号
     * @param platformTradeNo 平台交易号
     * @param payment 当前支付流水
     * @param notifyLog 回调日志
     * @return 状态推进是否成功
     */
    private boolean completePaidNotify(String paymentNo,
                                       String platformTradeNo,
                                       OshPayment payment,
                                       OshPaymentNotifyLogDO notifyLog) {
        return completePaidNotify(paymentNo, platformTradeNo, payment, notifyLog, null);
    }

    private boolean completePaidNotify(String paymentNo,
                                       String platformTradeNo,
                                       OshPayment payment,
                                       OshPaymentNotifyLogDO notifyLog,
                                       Integer channel) {
        LocalDateTime paidTime = LocalDateTime.now();

        // 更新支付流水，避免重复回调重复推进
        int paymentUpdated = paymentMapper.updatePendingToSuccess(paymentNo, platformTradeNo, paidTime, channel);
        if (paymentUpdated == 0) {
            return handlePaymentUpdateConflict(paymentNo, notifyLog);
        }

        // 同步推进订单状态和回调处理结果
        int orderUpdated = orderMapper.updatePendingToPaid(payment.getOrderNo(), paidTime);
        if (orderUpdated != 1) {
            throw new ServiceException("支付回调更新订单时发生并发冲突");
        }
        markNotifyLog(notifyLog, SIGN_VALID, NotifyProcessStatusEnum.SUCCESS.getCode(), null);

        // 根据商品类型发放权益
        handleOrderProductPaid(payment.getOrderNo());
        return true;
    }

    /**
     * 支付成功后的统一后置处理。
     *
     * @param orderNo 订单号
     */
    private void handleOrderProductPaid(String orderNo) {
        boolean newTask = !entitlementTaskService.exists(orderNo);
        entitlementTaskService.ensureTask(orderNo);

        Runnable paidTask = () -> {
            if (entitlementTaskService.isTaskMode()) {
                entitlementTaskService.processOrderSafely(orderNo);
                return;
            }
            if (!newTask) {
                return;
            }
            recordContributionRevenue(orderNo);

            try {
                paymentSuccessEventPublisher.publish(orderNo);
                log.info("【支付】发送支付成功消息成功，orderNo={}, userId={}", orderNo, UserContextUtil.getCurrentUserId());
            } catch (Exception e) {
                log.info("【支付】发送支付成功消息失败，orderNo={}, userId={}", orderNo, UserContextUtil.getCurrentUserId());
            }
        };

        registerAfterCommit(paidTask);
    }

    private void registerAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void recordContributionRevenue(String orderNo) {
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (Objects.isNull(order)) {
            return;
        }
        ProductTypeEnum productType = ProductTypeEnum.fromCode(order.getProductType());
        if (Objects.isNull(productType) || !isContributionResource(productType)) {
            return;
        }
        try {
            contributionService.recordRevenue(
                    productType.getName(),
                    order.getProductId(),
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getPayableAmount(),
                    order.getPointsAmount(),
                    productType.getName()
            );
        } catch (Exception e) {
            log.warn("record contribution revenue failed, orderNo={}, productType={}", orderNo, productType.getName(), e);
        }
    }

    private boolean isContributionResource(ProductTypeEnum productType) {
        return productType != null && ContributionResourceType.contributionTracked(productType.getName());
    }

    /**
     * 处理支付流水并发更新冲突。
     *
     * @param paymentNo 支付流水号
     * @param notifyLog 回调日志
     * @return 冲突处理结果
     */
    private boolean handlePaymentUpdateConflict(String paymentNo, OshPaymentNotifyLogDO notifyLog) {
        OshPayment latest = paymentMapper.selectByPaymentNo(paymentNo);
        if (Objects.nonNull(latest) && Objects.nonNull(latest.getStatus()) && latest.getStatus() == PAYMENT_SUCCESS) {
            return markNotifyIgnored(notifyLog, "重复回调", true);
        }
        return markNotifyFailed(notifyLog, SIGN_VALID, "支付流水状态不可更新");
    }

    /**
     * 校验结算参数。
     *
     * @param param 结算参数
     */
    private void validateCheckoutParam(OrderCheckoutReqVO param) {
        if (Objects.isNull(param)) {
            throw new ServiceException("结算参数不能为空");
        }
        Set<ConstraintViolation<OrderCheckoutReqVO>> violations = validator.validate(param);
        if (!violations.isEmpty()) {
            throw new ServiceException(violations.iterator().next().getMessage());
        }
    }

    /**
     * 组装需要支付的结算结果。
     *
     * @param orderNo 订单号
     * @param paymentNo 支付流水号
     * @param amount 支付金额
     * @param channel 支付渠道
     * @param payResponse 支付渠道响应
     * @return 需要支付的结算结果
     */
    private OrderCheckoutRespVO packageOrderCheckoutResult(String orderNo,
                                                           String paymentNo,
                                                           BigDecimal amount,
                                                           String channel,
                                                           PayResponse payResponse,
                                                           OshPayment payment,
                                                           PointDeduction pointDeduction) {
        OrderPaymentInfo paymentInfo = new OrderPaymentInfo();
        paymentInfo.setChannel(channel);
        paymentInfo.setQrcode(payResponse.getQrcode());
        paymentInfo.setPayUrl(payResponse.getPayUrl());

        OrderCheckoutRespVO result = new OrderCheckoutRespVO();
        result.setNeedPay(true);
        result.setOrderNo(orderNo);
        result.setPaymentNo(paymentNo);
        result.setPayStatus(String.valueOf(OrderStatusEnum.PENDING.getCode()));
        result.setPrice(amount);
        fillPointDeductionResult(result, pointDeduction);
        result.setExpireTime(formatDateTime(payment == null ? null : payment.getExpireTime()));
        result.setCloseExpireMinutes((int) resolvePayExpireMinutes());
        result.setPayment(paymentInfo);
        return result;
    }

    /**
     * 组装免费订单结算结果。
     *
     * @param orderNo 订单号
     * @param paymentNo 支付流水号
     * @param amount 支付金额
     * @return 免费订单结算结果
     */
    private OrderCheckoutRespVO freeCheckoutResult(String orderNo,
                                                   String paymentNo,
                                                   BigDecimal amount,
                                                   PointDeduction pointDeduction) {
        OrderCheckoutRespVO result = new OrderCheckoutRespVO();
        result.setNeedPay(false);
        result.setOrderNo(orderNo);
        result.setPaymentNo(paymentNo);
        result.setPayStatus(String.valueOf(ORDER_PAID));
        result.setPrice(amount);
        fillPointDeductionResult(result, pointDeduction);
        result.setCloseExpireMinutes((int) resolvePayExpireMinutes());
        return result;
    }

    /**
     * 填充积分抵扣展示字段。
     */
    private void fillPointDeductionResult(OrderCheckoutRespVO result, PointDeduction pointDeduction) {
        PointDeduction deduction = pointDeduction == null ? PointDeduction.none(BigDecimal.ZERO) : pointDeduction;
        result.setPointsUsed(deduction.getPointsUsed());
        result.setPointsDeductAmount(deduction.getDeductAmount());
        result.setRemainingPoints(deduction.getRemainingPoints());
    }

    /**
     * 将订单和支付流水转换为统一状态结果。
     *
     * @param order 订单信息
     * @param payment 支付流水信息
     * @return 统一状态结果
     */
    private OrderStatusResult toStatusResult(OshOrder order, OshPayment payment) {
        OrderStatusResult result = new OrderStatusResult();
        if (Objects.nonNull(order)) {
            result.setOrderNo(order.getOrderNo());
            result.setOrderStatus(order.getStatus());
        }
        if (Objects.nonNull(payment)) {
            result.setPaymentNo(payment.getPaymentNo());
            result.setPaymentStatus(payment.getStatus());
            result.setPayStatus(Objects.nonNull(payment.getStatus()) && payment.getStatus() == PAYMENT_SUCCESS);
        } else {
            result.setPayStatus(Objects.nonNull(order) && Objects.nonNull(order.getStatus()) && order.getStatus() == ORDER_PAID);
        }
        return result;
    }

    private OshOrder requireUserOrder(String orderNo, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (Objects.isNull(order)) {
            throw new ServiceException("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new ServiceException("无权操作该订单");
        }
        return order;
    }

    /**
     * 生成支付请求快照，便于后续追踪支付发起上下文。
     *
     * @param paymentNo 支付流水号
     * @param param 结算参数
     * @param clientIp 客户端 IP
     * @param channelCode 支付渠道
     * @return 支付请求快照
     */
    private Map<String, Object> payRequestSnapshot(String paymentNo,
                                                   OrderCheckoutReqVO param,
                                                   String clientIp,
                                                   int channelCode,
                                                   BigDecimal amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("out_trade_no", paymentNo);
        payload.put("name", param.getProductName());
        payload.put("money", money(amount).toPlainString());
        payload.put("clientip", clientIp);
        payload.put("channel", channelCode);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private void refreshUserAssetCache(Long userId, Long points) {
        if (redisCache == null) {
            return;
        }
        Map<String, Object> userMap = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        if (userMap == null) {
            return;
        }
        Object assetObject = userMap.get(OshUserConstants.ASSET);
        if (!(assetObject instanceof Map)) {
            return;
        }
        Map<String, String> asset = (Map<String, String>) assetObject;
        asset.put(OshUserConstants.POINTS, String.valueOf(points));
        userMap.put(OshUserConstants.ASSET, asset);
        redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);
    }

    private void registerAssetCacheRefreshAfterCommit(Long userId, Long points) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshUserAssetCache(userId, points);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshUserAssetCache(userId, points);
            }
        });
    }

    /**
     * 标记回调处理失败。
     *
     * @param notifyLog 回调日志
     * @param signValid 签名状态
     * @param errorMsg 失败原因
     * @return 固定返回 false
     */
    private boolean markNotifyFailed(OshPaymentNotifyLogDO notifyLog, Integer signValid, String errorMsg) {
        markNotifyLog(notifyLog, signValid, NotifyProcessStatusEnum.FAILED.getCode(), errorMsg);
        return false;
    }

    /**
     * 标记回调被忽略。
     *
     * @param notifyLog 回调日志
     * @param errorMsg 忽略原因
     * @param result 需要返回给调用方的处理结果
     * @return 指定的处理结果
     */
    private boolean markNotifyIgnored(OshPaymentNotifyLogDO notifyLog, String errorMsg, boolean result) {
        markNotifyLog(notifyLog, SIGN_VALID, NotifyProcessStatusEnum.IGNORED.getCode(), errorMsg);
        return result;
    }

    /**
     * 更新回调日志处理结果。
     *
     * @param notifyLog 回调日志
     * @param signValid 签名状态
     * @param processStatus 处理状态
     * @param errorMsg 错误信息
     */
    private void markNotifyLog(OshPaymentNotifyLogDO notifyLog,
                               Integer signValid,
                               Integer processStatus,
                               String errorMsg) {
        notifyLogMapper.updateProcessResult(
                notifyLog.getId(),
                notifyLog.getOrderNo(),
                signValid,
                processStatus,
                errorMsg
        );
    }

    /**
     * 校验回调签名。
     *
     * @param params 回调参数
     * @return 签名是否有效
     */
    private boolean isSignValid(Map<String, String> params) {
        String sign = params.get("sign");
        return Objects.nonNull(sign) && Objects.equals(sign, SignUtil.createSign(params,payConfig.KEY));
    }

    /**
     * 比较支付平台金额和本地金额是否一致。
     *
     * @param actual 支付平台回传金额
     * @param expected 本地支付金额
     * @return 金额是否一致
     */
    private boolean sameMoney(String actual, BigDecimal expected) {
        if (Objects.isNull(actual) || Objects.isNull(expected)) {
            return false;
        }
        try {
            return new BigDecimal(actual).compareTo(money(expected)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 统一金额精度。
     *
     * @param amount 原始金额
     * @return 保留两位小数的金额
     */
    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判断金额是否为免费订单金额。
     *
     * @param amount 支付金额
     * @return 是否免费
     */
    private boolean isFreeAmount(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 判断支付渠道创建支付是否成功。
     *
     * @param payResponse 支付渠道响应
     * @return 是否创建成功
     */
    private boolean isPayCreateSuccess(PayResponse payResponse) {
        return payResponse.getCode() == PAY_CREATE_SUCCESS_CODE;
    }

    private long resolvePayExpireMinutes() {
        if (payConfig.EXPIRE_MINUTES == null || payConfig.EXPIRE_MINUTES <= 0) {
            return DEFAULT_PAY_EXPIRE_MINUTES;
        }
        return payConfig.EXPIRE_MINUTES;
    }

    /**
     * 构建支付创建失败响应。
     *
     * @return 支付创建失败响应
     */
    private PayResponse failedPayResponse() {
        PayResponse response = new PayResponse();
        response.setCode(0);
        response.setMsg(PAY_CREATE_FAILED_MESSAGE);
        return response;
    }

    /**
     * 解析支付渠道枚举。新建订单统一走支付宝；旧前端传 wxpay/wechat/union 时也兜底为支付宝。
     *
     * @param channelValue 前端传入的渠道标识
     * @return 支付渠道枚举
     */
    private PayChannelEnum resolvePaymentChannel(String channelValue) {
        if (StringUtils.isBlank(channelValue)) {
            return PayChannelEnum.ALIPAY;
        }
        String normalized = channelValue.trim().toLowerCase();
        if (PayChannelEnum.WXPAY.getValue().equals(normalized)
                || "wechat".equals(normalized)
                || "union".equals(normalized)
                || "unionpay".equals(normalized)) {
            return PayChannelEnum.ALIPAY;
        }
        PayChannelEnum channel = PayChannelEnum.fromValue(normalized);
        return Objects.nonNull(channel) ? channel : PayChannelEnum.ALIPAY;
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串，序列化失败时返回对象字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化支付上下文失败, valueType={}", Objects.isNull(value) ? "null" : value.getClass().getName(), e);
            return String.valueOf(value);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_TIME_FORMATTER);
    }

    private static class CheckoutCreateContext {

        private OshOrder order;

        private OshPayment payment;

        private BigDecimal cashPayAmount;

        private PointDeduction pointDeduction;

        public OshOrder getOrder() {
            return order;
        }

        public void setOrder(OshOrder order) {
            this.order = order;
        }

        public OshPayment getPayment() {
            return payment;
        }

        public void setPayment(OshPayment payment) {
            this.payment = payment;
        }

        public BigDecimal getCashPayAmount() {
            return cashPayAmount;
        }

        public void setCashPayAmount(BigDecimal cashPayAmount) {
            this.cashPayAmount = cashPayAmount;
        }

        public PointDeduction getPointDeduction() {
            return pointDeduction;
        }

        public void setPointDeduction(PointDeduction pointDeduction) {
            this.pointDeduction = pointDeduction;
        }
    }

    private static class PointDeduction {

        private final long pointsUsed;

        private final BigDecimal deductAmount;

        private final Long remainingPoints;

        private PointDeduction(long pointsUsed, BigDecimal deductAmount, Long remainingPoints) {
            this.pointsUsed = pointsUsed;
            this.deductAmount = deductAmount;
            this.remainingPoints = remainingPoints;
        }

        private static PointDeduction none(BigDecimal ignoredAmount) {
            return new PointDeduction(0L, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), null);
        }

        public long getPointsUsed() {
            return pointsUsed;
        }

        public BigDecimal getDeductAmount() {
            return deductAmount;
        }

        public Long getRemainingPoints() {
            return remainingPoints;
        }
    }
}
