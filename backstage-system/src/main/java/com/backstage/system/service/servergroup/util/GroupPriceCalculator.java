package com.backstage.system.service.servergroup.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 拼团价格计算工具类
 * 
 * 统一封装拼团模块中的价格计算、剩余时间计算逻辑，消除重复代码。
 * 
 * @author system
 * @date 2026-06-24
 */
public class GroupPriceCalculator {

    /** 会员折扣比例（八折） */
    public static final BigDecimal MEMBER_DISCOUNT = new BigDecimal("0.8");

    /** 支付超时时间（分钟） */
    public static final int PAY_TIMEOUT_MINUTES = 30;

    private GroupPriceCalculator() {
        // 工具类不允许实例化
    }

    /**
     * 计算剩余月数（向上取整：不足一个月按一个月计算）
     *
     * @param expireTime 到期时间
     * @return 剩余月数（>=0）
     */
    public static BigDecimal calculateRemainingMonths(LocalDateTime expireTime) {
        if (expireTime == null) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), expireTime);
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        long remainingMonths = (days + 29) / 30; // 向上取整
        return new BigDecimal(remainingMonths);
    }

    /**
     * 根据剩余月数和总时长计算当前价格
     * 公式：basePrice * remainingMonths / totalDuration
     *
     * @param basePrice       基础价格
     * @param remainingMonths 剩余月数
     * @param totalDuration   总时长（月）
     * @return 当前价格（保留2位小数，四舍五入）
     */
    public static BigDecimal calculateCurrentPrice(BigDecimal basePrice, BigDecimal remainingMonths, int totalDuration) {
        if (basePrice == null || remainingMonths == null || totalDuration <= 0) {
            return basePrice != null ? basePrice : BigDecimal.ZERO;
        }
        if (remainingMonths.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return basePrice
                .multiply(remainingMonths)
                .divide(new BigDecimal(totalDuration), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算会员价格（当前价格 * 折扣比例）
     *
     * @param currentPrice 当前价格
     * @return 会员价格
     */
    public static BigDecimal calculateMemberPrice(BigDecimal currentPrice) {
        if (currentPrice == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.multiply(MEMBER_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算活动的完整价格信息（已成团场景）
     *
     * @param basePrice    基础价格
     * @param expireTime   服务器到期时间
     * @param totalDuration 总时长（月）
     * @return 价格结果
     */
    public static PriceResult calculateFormedGroupPrice(BigDecimal basePrice, LocalDateTime expireTime, int totalDuration) {
        BigDecimal remainingMonths = calculateRemainingMonths(expireTime);
        BigDecimal currentPrice = calculateCurrentPrice(basePrice, remainingMonths, totalDuration);
        BigDecimal memberPrice = calculateMemberPrice(currentPrice);
        return new PriceResult(remainingMonths, currentPrice, memberPrice);
    }

    /**
     * 计算活动的完整价格信息（未成团场景，显示基础价格）
     *
     * @param basePrice 基础价格
     * @return 价格结果
     */
    public static PriceResult calculateUnformedGroupPrice(BigDecimal basePrice) {
        BigDecimal price = basePrice != null ? basePrice : BigDecimal.ZERO;
        BigDecimal memberPrice = calculateMemberPrice(price);
        return new PriceResult(null, price, memberPrice);
    }

    /**
     * 价格计算结果
     */
    public static class PriceResult {
        private final BigDecimal remainingMonths;
        private final BigDecimal currentPrice;
        private final BigDecimal memberPrice;

        public PriceResult(BigDecimal remainingMonths, BigDecimal currentPrice, BigDecimal memberPrice) {
            this.remainingMonths = remainingMonths;
            this.currentPrice = currentPrice;
            this.memberPrice = memberPrice;
        }

        public BigDecimal getRemainingMonths() {
            return remainingMonths;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public BigDecimal getMemberPrice() {
            return memberPrice;
        }
    }
}
