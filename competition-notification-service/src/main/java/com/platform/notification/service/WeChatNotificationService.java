package com.platform.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WeChatNotificationService {

    private final RateLimiter rateLimiter = RateLimiter.create(50.0);

    /**
     * Mock 调用微信模板消息 API
     */
    public boolean sendAuditResultTemplateMsg(Long userId, Integer status, String reason) {
        // 1. 尝试获取令牌（最多等待 2 秒）
        boolean acquired = rateLimiter.tryAcquire(2, TimeUnit.SECONDS);
        if (!acquired) {
            log.warn("🚦 [微信API限流] 请求过于频繁，获取令牌超时，触发限流拒绝！");
            // 返回 false 交给外层抛出异常触发重试
            return false;
        }
        log.info("🚀 [Mock微信API] 开始调用微信接口发送消息...");
        try {
            // 模拟网络延迟
            Thread.sleep(500);

            String statusStr = (status == 2) ? "审核通过 ✅" : "审核驳回 ❌";
            log.info("✉️ [Mock微信API] 发送成功！接收人UserID: {}, 审核状态: {}, 原因: {}", userId, statusStr, reason);

            // 模拟极小概率的API调用失败
            if (Math.random() < 0.05) {
                log.warn("⚠️ [Mock微信API] 微信接口响应超时或报错！");
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}