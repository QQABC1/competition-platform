package com.platform.registration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.platform.registration.mapper")
// 扫描本服务包 + 公共API包
@ComponentScan({"com.platform.registration", "com.platform.common"})
// 核心：显式扫描 API 模块中的 FeignClient
@EnableFeignClients(basePackages = {"com.platform.api.client"})
public class RegistrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegistrationApplication.class, args);
    }
}