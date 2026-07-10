package com.backstage.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 权益发放切换开关。legacy 兼容旧蓝绿 Kafka 消费者；task 启用数据库可靠任务。
 */
@Component
@ConfigurationProperties(prefix = "pay.entitlement")
public class PaymentEntitlementProperties {

    private String mode = "legacy";
    private long retryDelayMs = 30000L;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public boolean isTaskMode() {
        return "task".equalsIgnoreCase(mode);
    }
}
