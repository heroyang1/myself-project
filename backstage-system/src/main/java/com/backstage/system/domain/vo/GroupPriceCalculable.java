package com.backstage.system.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团价格计算公共接口
 * 
 * 用于统一 GroupActivityListVO 和 UserInitiatedActivityListVO 的价格计算逻辑，
 * 消除 OshGroupServerServiceImpl 中 calculatePriceAndTime 的重复重载。
 * 
 * @author system
 * @date 2026-07-11
 */
public interface GroupPriceCalculable {
    
    Boolean getIsSuccess();
    
    BigDecimal getBasePrice();
    
    LocalDateTime getServerEndTime();
    
    Integer getTotalDuration();
    
    void setRemainingMonths(BigDecimal remainingMonths);
    
    void setCurrentPrice(BigDecimal currentPrice);
}
