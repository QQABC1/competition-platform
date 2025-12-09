package com.platform.registration.controller;

import com.platform.common.api.R;
import com.platform.common.exception.BusinessException;
import com.platform.registration.dto.RegistrationDTO;
import com.platform.registration.service.RegistrationService;
import com.platform.registration.vo.RegistrationInitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/registration")
@Api(tags = "C端-报名管理")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    /**
     * 1. 进入报名页时调用
     */
    @GetMapping("/info/{competitionId}")
    @ApiOperation(value = "获取报名初始化信息")
    public R<RegistrationInitVO> getInfo(
            @Parameter(description = "竞赛ID", required = true)
            @PathVariable Long competitionId,
            HttpServletRequest request) {

        Long userId = getUserId(request);
        RegistrationInitVO vo = registrationService.getInitInfo(userId, competitionId);
        return R.ok(vo);
    }

    /**
     * 2. 点击提交报名时调用
     */
    @PostMapping("/apply")
    @Operation(summary = "提交报名申请", description = "需填写联系方式及附件等")
    public R<Void> apply(
            @RequestBody @Validated RegistrationDTO.Apply dto,
            HttpServletRequest request) {

        Long userId = getUserId(request);
        registrationService.apply(userId, dto);
        return R.ok();
    }

    /**
     * 辅助方法：从网关透传的 Header 中获取当前登录用户ID
     */
    private Long getUserId(HttpServletRequest request) {
        // 网关过滤器(AuthGlobalFilter)校验Token通过后，会放入 "Service-User-Id"
        String userIdStr = request.getHeader("Service-User-Id");

        // 如果是本地调试不走网关，可以手动在 Postman Header 加这个参数，或者在这里写死一个ID
        if (!StringUtils.hasText(userIdStr)) {
            //TODO 生产环境应该抛异常
//            throw new BusinessException(401, "未登录或Token失效");
            // TODO 开发环境为了方便调试，也可以临时返回一个测试ID
             return 1L;
        }

        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "用户ID格式错误");
        }
    }
}
