package com.platform.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("registrationThreadPool")
    public Executor registrationThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：IO密集型通常设置为 2N+1 (N为CPU核数)
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2 + 1);
        // 最大线程数
        executor.setMaxPoolSize(50);
        // 阻塞队列容量：最大允许5000个人排队落库，防止内存溢出
        executor.setQueueCapacity(5000);
        // 线程名称前缀，方便排查日志
        executor.setThreadNamePrefix("Reg-Async-");
        // 拒绝策略：队列满了之后，由调用者(Tomcat主线程)自己去执行，保证任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}