package com.platform.registration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报名异步处理状态")
public class RegistrationStatusVO {

    @Schema(description = "状态: 0:排队中/处理中 1:名成功 (落库成功) -1:报名失败 (落库异常")
    private Integer status;

    @Schema(description = "状态描述信息")
    private String message;
}
