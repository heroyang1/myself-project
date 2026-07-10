package com.backstage.system.task;

import com.backstage.system.service.order.OrderEntitlementTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 支付权益补发扫描；数据库 CAS 允许蓝绿实例同时运行。 */
@Component
public class OrderEntitlementRetryTask {

    private static final Logger log = LoggerFactory.getLogger(OrderEntitlementRetryTask.class);
    private final OrderEntitlementTaskService taskService;

    public OrderEntitlementRetryTask(OrderEntitlementTaskService taskService) {
        this.taskService = taskService;
    }

    @Scheduled(fixedDelayString = "${pay.entitlement.retry-delay-ms:30000}")
    public void retry() {
        try {
            int handled = taskService.retryDueTasks();
            if (handled > 0) {
                log.info("权益补发扫描完成, handled={}", handled);
            }
        } catch (Exception e) {
            log.error("权益补发扫描异常", e);
        }
    }
}
