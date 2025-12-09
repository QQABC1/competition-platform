package com.platform.registration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报名页初始化信息 (回显)")
public class RegistrationInitVO {

    @Schema(description = "是否已报名 (true:已报名, false:未报名)")
    private Boolean isRegistered;

    @Schema(description = "报名记录ID (若未报名则为null)")
    private Long registrationId;

    @Schema(description = "个人信息是否完善 (false时需弹窗提示)")
    private Boolean isProfileComplete;

    // --- 以下字段用于前端表单回显 (来自 User 服务) ---

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "学号")
    private String studentId;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "联系电话 (账号绑定)")
    private String phone;
}