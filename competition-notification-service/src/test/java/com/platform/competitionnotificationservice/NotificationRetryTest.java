package com.platform.competitionnotificationservice;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.message.RegistrationAuditMessage;
import com.platform.notification.NotificationApplication;
import com.platform.notification.service.WeChatNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = NotificationApplication.class)
public class NotificationRetryTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 使用 MockBean 替换 Spring 容器中真实的微信服务
    @MockBean
    private WeChatNotificationService weChatNotificationService;

    @Test
    public void testListenerRetry3Times() throws Exception {
        // ================= 1. 准备 Mock 行为 =================
        // 强行设置：只要调用 sendAuditResultTemplateMsg，就抛出 RuntimeException
        Mockito.when(weChatNotificationService.sendAuditResultTemplateMsg(
                Mockito.anyLong(), Mockito.anyInt(), Mockito.anyString()
        )).thenThrow(new RuntimeException("【测试模拟】微信服务器连接超时！"));

        // ================= 2. 构造并发送 MQ 消息 =================
        RegistrationAuditMessage mockMessage = new RegistrationAuditMessage()
                .setRegistrationId(3003L)
                .setUserId(111L)
                .setStatus(2)
                .setAuditReason("测试重试机制");

        System.out.println("🚀 [重试测试] 开始向 RabbitMQ 投递模拟消息...");
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.REGISTRATION_AUDIT_ROUTING_KEY,
                mockMessage
        );

        // ================= 3. 等待重试完成 =================
        // Spring Retry 默认的重试间隔是 1000ms（1秒）。
        // 正常执行 1 次 + 重试 2 次，大约需要等待 2~3 秒钟。
        // 我们让主线程睡眠 5 秒，确保异步重试逻辑全部执行完毕。
        System.out.println("⏳ [重试测试] 等待 5 秒钟，请观察控制台的异常打印次数...");
        Thread.sleep(5000);

        // ================= 4. 验证重试次数 =================
        // 核心断言：验证微信发送服务是否被精确调用了 3 次！
        Mockito.verify(weChatNotificationService, Mockito.times(3))
                .sendAuditResultTemplateMsg(
                        Mockito.eq(111L),
                        Mockito.eq(2),
                        Mockito.eq("测试重试机制")
                );

        System.out.println("✅ [重试测试] 验证通过！Service 确实被调用了 3 次，证明 max-attempts: 3 生效！");
    }
}