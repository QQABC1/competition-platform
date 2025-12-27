package com.platform.registration.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("报名审核请求参数")
public class RegistrationAuditDTO {

    @NotNull(message = "报名记录ID不能为空")
    @ApiModelProperty(value = "报名记录ID", required = true, example = "1001")
    private Long id;

    @NotNull(message = "审核结果不能为空")
    @ApiModelProperty(value = "是否通过 (true:通过, false:驳回)", required = true, example = "true")
    private Boolean pass;

    @ApiModelProperty(value = "驳回理由 (驳回时必填)")
    private String reason;
}