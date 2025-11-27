package com.platform.competition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient // 开启服务注册发现
@EnableFeignClients    // 开启远程调用
// 确保扫描到 common 模块的全局异常处理和工具类
@ComponentScan({"com.platform.competition", "com.platform.common"})
public class CompetitionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompetitionApplication.class, args);
    }
}