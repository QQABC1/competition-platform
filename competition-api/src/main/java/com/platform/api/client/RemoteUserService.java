package com.platform.api.client;

import com.platform.api.domain.UserInternalVO;
import com.platform.common.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 远程调用用户服务接口
 * name: 必须与 user-service 的 spring.application.name 一致
 * contextId: 必填，当一个服务有多个 FeignClient 类时，用于区分 Bean 名称
 */
@FeignClient(name = "competition-user-service", contextId = "remoteUserService")
public interface RemoteUserService {

    /**
     * 根据ID获取用户信息 (内部调用)
     */
    @GetMapping("/user/internal/info/{id}")
    R<UserInternalVO> getUserInfo(@PathVariable("id") Long id);
}