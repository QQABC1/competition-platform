package com.platform.notification.listener;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.constant.RedisKeyConstants;
import com.platform.common.message.RegistrationAuditMessage;
import com.platform.notification.service.WeChatNotificationService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RegistrationAuditListener {

    @Autowired
    private WeChatNotificationService weChatNotificationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConstants.REGISTRATION_AUDIT_QUEUE)
    public void handleRegistrationAudit(RegistrationAuditMessage auditMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Long regId = auditMessage.getRegistrationId();
        Integer status = auditMessage.getStatus();
        Long auditTime = auditMessage.getAuditTime();

        // 示例: noti:audit:reg:1001:3:1698765432000
        String idempotentKey = String.format("%s%d:%d:%d",
                RedisKeyConstants.REDIS_IDEMPOTENT_PREFIX, regId, status, auditTime);

        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "PROCESSING", 1, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(isFirstTime)) {
            // 被防重机制拦截：说明这条通知已经发送过（或正在发送中），直接静默 ACK 丢弃
            log.info("重复的报名审核通知，已忽略。报名ID: {}, 状态: {}", regId, status);
            channel.basicAck(deliveryTag, false);
            return;
        }

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

            // 否则 Spring 的本地重试（或 MQ 的重新投递）会被上面的 setIfAbsent 再次拦截，导致永远无法重试成功。
            redisTemplate.delete(idempotentKey);
            log.info("已清理 Redis 占用标记，允许重试。Key: {}", idempotentKey);

            // 因此必须向外抛出。当达到 3 次上限后，Spring 会默认向 MQ 发送 Reject (requeue=false)。
            // 此时，因为我们配置了 x-dead-letter-exchange，消息会安静地躺进死信队列，供人工后续处理。
            throw e; // 直接抛出，将控制权交还给 Spring Retry 机制
        }
    }
}