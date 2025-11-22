package com.platform.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

public class UserAuthDTO {

    @Data
    @Schema(description = "登录请求参数")
    public static class LoginRequest {
        // 修改点：使用 required = true
        @Schema(description = "手机号", required = true, example = "13800138000")
        private String phone;

        @Schema(description = "验证码", required = true, example = "123456")
        private String code;
    }

    @Data
    @Schema(description = "登录响应数据")
    public static class LoginResponse {
        @Schema(description = "Token令牌")
        private String token;

        @Schema(description = "是否需要绑定信息(true:需跳转绑定页, false:直接进首页)")
        private boolean needBinding;
    }

    @Data
    @Schema(description = "完善信息请求参数")
    public static class BindRequest {
        @Schema(description = "学号", required = true)
        private String studentId;

        @Schema(description = "真实姓名", required = true)
        private String realName;

        @Schema(description = "专业班级", required = true)
        private String major;
    }
}