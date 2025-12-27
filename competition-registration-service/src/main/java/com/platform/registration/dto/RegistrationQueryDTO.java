package com.platform.registration.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("报名列表查询参数")
public class RegistrationQueryDTO {

    @ApiModelProperty(value = "竞赛ID", required = true)
    private Long competitionId;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "状态筛选 (1待审核/2已通过/3已驳回)", example = "1")
    private Integer status;

    @ApiModelProperty(value = "关键词 (姓名/学号)")
    private String keyword;
}