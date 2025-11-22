package com.platform.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.user.dto.UserAuthDTO;
import com.platform.user.entity.User;

public interface UserService extends IService<User> {
    // 发送验证码
    void sendCode(String phone);

    // 登录或注册
    UserAuthDTO.LoginResponse login(UserAuthDTO.LoginRequest request);

    // 完善信息
    void bindUserInfo(Long userId, UserAuthDTO.BindRequest request);
}