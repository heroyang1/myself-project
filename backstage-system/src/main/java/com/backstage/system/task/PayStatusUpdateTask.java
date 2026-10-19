package com.backstage.system.task;

import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PayStatusUpdateTask {
    private static final Logger log = LoggerFactory.getLogger(PayStatusUpdateTask.class);

    private static final long ORDER_CLOSE_BUFFER_MINUTES = 1L;


    @Resource
    private OrderService orderService;

    @Resource
    private OshPaymentMapper paymentMapper;


    /**
     * 关闭超时未支付的支付流水和关联订单。
     *
     * 【已停用】本任务与 backstage-quartz 的 OrderCloseTask（@XxlJob "OrderCloseHandler"）
     * 逻辑完全相同（同为 selectExpiredPendingPayments + cancelPayment，同为 1 分钟缓冲），
     * 双调度器重复执行会浪费支付平台对账调用并产生误导性 warn 日志。
     * 职责已归口 OrderCloseTask（项目规范：定时任务统一归口 backstage-quartz + XXL-Job），禁止恢复此注解。
     */
    // @Scheduled(fixedDelayString = "${pay.status.reconcile-delay-ms:60000}",
    //         initialDelayString = "${pay.status.reconcile-initial-delay-ms:60000}")
    public void updateOrderStatus() {
        log.info("【支付-定时任务】开始关闭超时未支付订单");


        try{
            LocalDateTime expireDeadline = LocalDateTime.now().minusMinutes(ORDER_CLOSE_BUFFER_MINUTES);
            List<OshPayment> expiredPayments = paymentMapper.selectExpiredPendingPayments(expireDeadline);
            for (OshPayment payment : expiredPayments) {
                closeExpiredPayment(payment);
            }
        }catch (Exception e){
            log.error("【支付-定时任务】关闭超时未支付订单异常, 异常详情:{}",e.getMessage(),e);
        }

        log.info("【支付-定时任务】关闭超时未支付订单完成");
    }

    /**
     * 逐笔关闭过期支付流水，单笔异常不影响后续流水。
     *
     * @param payment 过期待支付流水
     */
    private void closeExpiredPayment(OshPayment payment) {
        try {
            orderService.cancelPayment(payment.getPaymentNo());
        } catch (Exception e) {
            log.warn("【支付-定时任务】关闭超时支付流水失败, paymentNo={}, orderNo={}, error={}",
                    payment.getPaymentNo(), payment.getOrderNo(), e.getMessage(), e);
        }
    }
}
