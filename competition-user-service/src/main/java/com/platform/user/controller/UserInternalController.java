package com.platform.user.controller;

import com.platform.api.domain.UserInternalVO; // 引用 api 模块的对象
import com.platform.common.api.R;
import com.platform.user.entity.User;
import com.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/internal")
@Hidden // 不在 Swagger 文档显示
public class UserInternalController {

    @Autowired
    private UserService userService;

    // 路径必须与 RemoteUserService 中的 @GetMapping 一致
    @GetMapping("/info/{id}")
    public R<UserInternalVO> getUserInfo(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }

        // 转换 Entity -> VO
        UserInternalVO vo = new UserInternalVO();
        BeanUtils.copyProperties(user, vo);
        return R.ok(vo);
    }
}