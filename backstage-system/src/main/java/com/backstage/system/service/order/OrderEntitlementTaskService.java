package com.backstage.system.service.order;

import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.OshOrderEntitlementTask;
import com.backstage.system.config.PaymentEntitlementProperties;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.order.OshOrderEntitlementTaskMapper;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.behavior.ContributionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 可靠权益发放任务：支付事务只负责写任务，处理失败会落库并由定时任务重试。
 */
@Service
public class OrderEntitlementTaskService {

    private static final Logger log = LoggerFactory.getLogger(OrderEntitlementTaskService.class);
    private static final int STATUS_SUCCESS = 2;
    private static final int BATCH_SIZE = 50;
    private static final long STALE_LOCK_MINUTES = 5L;

    @Resource
    private OshOrderEntitlementTaskMapper taskMapper;

    @Resource
    private OshOrderMapper orderMapper;

    @Lazy
    @Resource
    private OrderPaidHandlerRegistry paidHandlerRegistry;

    @Resource
    private PaymentEntitlementProperties entitlementProperties;

    @Resource
    private ContributionService contributionService;

    @Resource
    private PaymentSuccessEventPublisher paymentSuccessEventPublisher;

    /** 必须在支付成功事务中调用，唯一键保证重复回调不会重复建任务。 */
    public void ensureTask(String orderNo) {
        OshOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || order.getProductType() == null) {
            throw new IllegalStateException("支付成功但订单不存在或商品类型为空: " + orderNo);
        }
        taskMapper.insertOrKeep(orderNo, order.getProductType());
        OshOrderEntitlementTask task = taskMapper.selectByOrderNo(orderNo);
        if (task == null || !Objects.equals(task.getProductType(), order.getProductType())) {
            throw new IllegalStateException("权益任务未创建或商品类型不一致: " + orderNo);
        }
    }

    /** 事务提交后立即尝试一次；失败只记录任务，不影响支付平台 ACK。 */
    public void processOrderSafely(String orderNo) {
        try {
            processOrder(orderNo);
        } catch (Exception e) {
            log.error("权益任务执行异常, orderNo={}, error={}", orderNo, e.getMessage(), e);
        }
    }

    public void processOrder(String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusMinutes(STALE_LOCK_MINUTES);
        if (taskMapper.claim(orderNo, now, staleBefore) != 1) {
            return;
        }

        OshOrderEntitlementTask task = taskMapper.selectByOrderNo(orderNo);
        try {
            OshOrder order = orderMapper.selectByOrderNo(orderNo);
            if (order == null || !Objects.equals(order.getStatus(), OrderStatusEnum.PAID.getCode())) {
                throw new IllegalStateException("权益任务对应订单不是已支付状态");
            }
            ProductTypeEnum productType = ProductTypeEnum.fromCode(task == null ? null : task.getProductType());
            if (productType == null || !paidHandlerRegistry.hasHandler(productType.getName())) {
                throw new IllegalStateException("未找到商品权益处理器");
            }
            paidHandlerRegistry.handle(productType.getName(), orderNo);
            recordContributionRevenue(order, productType);
            sendPaidMessage(orderNo);
            if (taskMapper.markSuccess(orderNo, LocalDateTime.now()) != 1) {
                throw new IllegalStateException("权益任务成功状态更新失败");
            }
            log.info("权益任务执行成功, orderNo={}, productType={}", orderNo, productType.getName());
        } catch (Exception e) {
            int attempts = task == null || task.getAttempts() == null ? 1 : task.getAttempts();
            long retrySeconds = Math.min(3600L, 30L * (1L << Math.min(attempts - 1, 7)));
            taskMapper.markRetry(
                    orderNo,
                    LocalDateTime.now().plusSeconds(retrySeconds),
                    StringUtils.left(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), 500)
            );
            throw e;
        }
    }

    public int retryDueTasks() {
        if (!isTaskMode()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<OshOrderEntitlementTask> tasks = taskMapper.selectDueTasks(
                now, now.minusMinutes(STALE_LOCK_MINUTES), BATCH_SIZE);
        int handled = 0;
        for (OshOrderEntitlementTask task : tasks) {
            processOrderSafely(task.getOrderNo());
            handled++;
        }
        return handled;
    }

    public boolean isCompleted(String orderNo) {
        OshOrderEntitlementTask task = taskMapper.selectByOrderNo(orderNo);
        return task != null && Objects.equals(task.getStatus(), STATUS_SUCCESS);
    }

    public boolean exists(String orderNo) {
        return taskMapper.selectByOrderNo(orderNo) != null;
    }

    public boolean isTaskMode() {
        return entitlementProperties != null && entitlementProperties.isTaskMode();
    }

    private void recordContributionRevenue(OshOrder order, ProductTypeEnum productType) {
        if (!ContributionResourceType.contributionTracked(productType.getName())) {
            return;
        }
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
    }

    private void sendPaidMessage(String orderNo) {
        paymentSuccessEventPublisher.publish(orderNo);
    }
}
