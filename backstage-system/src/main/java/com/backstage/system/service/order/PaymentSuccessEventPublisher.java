package com.backstage.system.service.order;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.constant.KafkaConstants;
import com.backstage.common.utils.kafka.KafkaMessageUtil;
import com.backstage.system.domain.message.order.PaySuccessMessage;
import org.springframework.stereotype.Component;

/** 支付成功 Kafka 事件发布适配器，便于可靠任务重试与单元测试。 */
@Component
public class PaymentSuccessEventPublisher {

    public void publish(String orderNo) {
        PaySuccessMessage message = new PaySuccessMessage();
        message.setOrderNo(orderNo);
        // 权益任务只有在 broker 确认后才能标记成功；异步回调会在任务已成功后才暴露发送失败。
        KafkaMessageUtil.sendMessageSync(
                KafkaConstants.PAY_SUCCESS_TOPIC,
                orderNo,
                JSON.toJSONString(message)
        );
    }
}
