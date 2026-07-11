package com.backstage.system.task;

import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.servergroup.enums.GroupOrderStatusEnum;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团订单超时取消定时任务
 * 
 * 功能：
 * - 每5分钟执行一次
 * - 查询超时未支付的订单（默认15分钟）
 * - 取消订单并释放参团名额
 * 
 * 使用方式：
 * 1. 添加 @Scheduled(cron = "0 0/5 * * * ?") 注解
 * 2. 或集成到 Quartz 定时任务管理系统
 *
 * @author system
 * @date 2026-05-14
 */
@Component
public class GroupOrderTimeoutCancelTask {

    private static final Logger log = LoggerFactory.getLogger(GroupOrderTimeoutCancelTask.class);

    /** 订单超时时间（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private OshGroupServerMapper groupServerMapper;

    @Autowired
    private OshPaymentMapper paymentMapper;

    @Autowired
    private OrderService orderService;

    /**
     * 取消超时未支付的拼团订单
     * 
     * TODO: 启用定时任务
     * 建议执行时间：每5分钟
     * Cron 表达式：0 0/5 * * * ?
     */
    @Scheduled(fixedDelayString = "${pay.group.reconcile-delay-ms:60000}",
            initialDelayString = "${pay.group.reconcile-initial-delay-ms:60000}")
    public void cancelTimeoutOrders() {
        log.info("========== 开始取消超时拼团订单 ==========");
        
        long startTime = System.currentTimeMillis();
        int totalCount = 0;
        int cancelledCount = 0;
        int failedCount = 0;
        
        try {
            // 计算超时时间点
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
            
            // 查询超时未支付的订单
            List<OshGroupOrder> timeoutOrders = groupServerMapper.selectTimeoutOrders(timeoutThreshold);
            
            if (timeoutOrders == null || timeoutOrders.isEmpty()) {
                log.info("没有超时订单需要处理");
                return;
            }
            
            totalCount = timeoutOrders.size();
            log.info("发现 {} 个超时订单", totalCount);
            
            for (OshGroupOrder order : timeoutOrders) {
                try {
                    if (cancelOrderSafely(order)) {
                        cancelledCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                    log.error("取消订单失败，订单号: {}", order.getOrderNo(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("取消超时订单任务执行失败", e);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("========== 取消超时订单完成 ==========");
        log.info("总计检查: {} 个订单", totalCount);
        log.info("成功取消: {} 个", cancelledCount);
        log.info("失败数量: {} 个", failedCount);
        log.info("耗时: {} ms", endTime - startTime);
    }

    /**
     * 按订单号取消待支付拼团订单，并复用原有名额释放逻辑。
     *
     * @param orderNo 拼团订单号
     */
    public void cancelPendingOrderByOrderNo(String orderNo) {
        OshGroupOrder order = groupServerMapper.selectGroupOrderByOrderNo(orderNo);
        if (order == null) {
            log.warn("拼团订单不存在，跳过取消，订单号: {}", orderNo);
            return;
        }
        if (!GroupOrderStatusEnum.PENDING.getCode().equals(order.getStatus())) {
            log.info("拼团订单不是待支付状态，跳过取消，订单号: {}, status={}", orderNo, order.getStatus());
            return;
        }
        cancelOrder(order);
    }

    /**
     * 取消单个订单并释放名额
     */
    private void cancelOrder(OshGroupOrder order) {
        log.info("开始取消订单: {}, 用户ID: {}", order.getOrderNo(), order.getUserId());
        
        // 1. 更新订单状态为已取消
        int updateOrderResult = groupServerMapper.updatePendingGroupOrderToCancelled(
                order.getId(), LocalDateTime.now());
        
        if (updateOrderResult <= 0) {
            throw new RuntimeException("更新订单状态失败");
        }
        
        // 2. 更新参团记录状态为已取消（3=已取消）
        if (order.getGroupWorkId() != null) {
            groupServerMapper.updatePendingGroupWorkToCancelled(order.getGroupWorkId());
        }
        
        // 注意：不再释放拼团活动人数（currentNum），因为 join 时未递增 currentNum，
        // currentNum 仅在支付成功回调中递增，取消未支付订单无需回退。
        
        log.info("订单取消成功: {}", order.getOrderNo());
    }

    /**
     * 有统一支付流水时，必须先查单、关单并再次查单；只有从未创建支付流水的预占单
     * 才能直接释放，避免二维码仍可付款但拼团已经被本地取消。
     */
    private boolean cancelOrderSafely(OshGroupOrder order) {
        OshPayment payment = paymentMapper.selectByPaymentNo(order.getOrderNo());
        if (payment == null) {
            cancelOrder(order);
            return true;
        }
        if (PaymentStatusEnum.PENDING.getCode() == payment.getStatus()) {
            orderService.cancelPayment(payment.getPaymentNo());
            return true;
        }
        if (PaymentStatusEnum.SUCCESS.getCode() == payment.getStatus()) {
            log.warn("拼团统一订单已支付，禁止超时取消, orderNo={}", order.getOrderNo());
            return false;
        }
        // 统一支付已明确失败/关闭，但拼团后置状态尚未同步，安全释放预占。
        cancelOrder(order);
        return true;
    }

}
