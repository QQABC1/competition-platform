package com.platform.user.controller;

import com.platform.common.api.R;
import com.platform.common.utils.JwtUtils;
import com.platform.user.dto.UserAuthDTO;
import com.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "处理C端用户登录/注册/绑定")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/send-code")
    @Operation(summary = "1. 发送验证码")
    public R<Void> sendCode(@RequestParam String phone) {
        userService.sendCode(phone);
        return R.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "2. 手机号验证码登录", description = "自动注册逻辑，返回Token和绑定状态")
    public R<UserAuthDTO.LoginResponse> login(@RequestBody UserAuthDTO.LoginRequest request) {
        UserAuthDTO.LoginResponse response = userService.login(request);
        return R.ok(response);
    }

    @PostMapping("/bind")
    @Operation(summary = "3. 完善个人信息", description = "需携带Token")
    public R<Void> bindInfo(@RequestHeader("Authorization") String token,
                            @RequestBody UserAuthDTO.BindRequest request) {
        // 从Header解析出用户ID
        Long userId = JwtUtils.getUserId(token);

        userService.bindUserInfo(userId, request);
        return R.ok();
    }
}