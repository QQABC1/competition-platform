package com.platform.notification.config;

import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.constant.RedisKeyConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 使用 JSON 序列化消息
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    //死信队列
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(RabbitMQConstants.DLX_EXCHANGE);
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(RabbitMQConstants.DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(RabbitMQConstants.DLX_ROUTING_KEY);
    }

    //正常业务队列
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue registrationAuditQueue() {
        // 🌟 核心：给主队列绑定死信属性
        return QueueBuilder.durable(RabbitMQConstants.REGISTRATION_AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindingRegistrationAudit(Queue registrationAuditQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(registrationAuditQueue)
                .to(notificationExchange)
                .with(RabbitMQConstants.REGISTRATION_AUDIT_ROUTING_KEY);
    }
}