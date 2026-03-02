package com.platform.common.constant;

public class RabbitMQConstants {
    // 交换机名称
    public static final String NOTIFICATION_EXCHANGE = "competition.notification.topic";
    // 报名审核队列名称
    public static final String REGISTRATION_AUDIT_QUEUE = "notification.registration.audit.queue";
    // 路由键
    public static final String REGISTRATION_AUDIT_ROUTING_KEY = "notification.registration.audit";

    // ================= 1. 死信队列配置 (DLX) =================
    public static final String DLX_EXCHANGE = "notification.dlx.exchange";
    public static final String DLX_QUEUE = "notification.dlx.queue";
    public static final String DLX_ROUTING_KEY = "notification.dlx.routing";
}