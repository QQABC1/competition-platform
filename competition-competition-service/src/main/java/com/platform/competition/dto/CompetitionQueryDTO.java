package com.platform.competition.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "C端竞赛列表查询参数")
public class CompetitionQueryDTO {

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "搜索关键词")
    private String keyword;

    @ApiModelProperty(value = "分类ID")
    private Long categoryId;

    @ApiModelProperty(value = "主办方ID")
    private Long organizerId;

    @ApiModelProperty(value = "时间状态: 1-报名中, 2-进行中, 3-已结束")
    private Integer timeStatus;
}