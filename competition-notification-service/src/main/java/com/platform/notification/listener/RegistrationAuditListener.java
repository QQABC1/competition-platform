package com.platform.notification.listener;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.message.RegistrationAuditMessage;
import com.platform.notification.service.WeChatNotificationService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RegistrationAuditListener {

    @Autowired
    private WeChatNotificationService weChatNotificationService;

    @RabbitListener(queues = RabbitMQConstants.REGISTRATION_AUDIT_QUEUE)
    public void handleRegistrationAudit(RegistrationAuditMessage auditMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        log.info("📥 接收到报名审核状态变更消息: {}", auditMessage);

        try {
            // 1. 调用微信服务发送通知
            boolean isSent = weChatNotificationService.sendAuditResultTemplateMsg(
                    auditMessage.getUserId(),
                    auditMessage.getStatus(),
                    auditMessage.getAuditReason()
            );

            // 2. 根据发送结果处理 ACK
            if (isSent) {
                // 消费成功，手动 ACK (参数2: false代表只确认当前消息)
                channel.basicAck(deliveryTag, false);
                log.info("✅ 消息消费成功并ACK, 报名ID: {}", auditMessage.getRegistrationId());
            } else {
                //业务失败（如限流、网络波动），不手动 Nack！
                // 而是抛出异常，让 Spring 捕获并触发本地重试 (max-attempts: 3)
                throw new RuntimeException("微信推送返回失败，准备触发本地重试");
            }

        } catch (Exception e) {
            log.error("❌ 消息消费发生异常，报名ID: {}", auditMessage.getRegistrationId(), e);
            // 因此必须向外抛出。当达到 3 次上限后，Spring 会默认向 MQ 发送 Reject (requeue=false)。
            // 此时，因为我们配置了 x-dead-letter-exchange，消息会安静地躺进死信队列，供人工后续处理。
            throw e; // 直接抛出，将控制权交还给 Spring Retry 机制
        }
    }
}