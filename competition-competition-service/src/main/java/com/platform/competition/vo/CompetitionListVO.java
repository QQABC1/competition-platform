package com.platform.competition.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "C端竞赛列表展示对象")
public class CompetitionListVO {

    @ApiModelProperty(value = "竞赛ID")
    private Long id;

    @ApiModelProperty(value = "竞赛名称")
    private String title;

    @ApiModelProperty(value = "封面图URL")
    private String coverImg;

    @ApiModelProperty(value = "分类名称")
    private String categoryName;

    @ApiModelProperty(value = "是否置顶")
    private Integer isTop;

    @ApiModelProperty(value = "状态文本(报名中/进行中/已结束)")
    private String statusText;

    @ApiModelProperty(value = "报名截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regEndTime;
}