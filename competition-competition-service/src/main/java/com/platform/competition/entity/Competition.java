package com.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_competition")
@Schema(description = "竞赛主表实体")
public class Competition {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "竞赛名称")
    private String title;

    @Schema(description = "主办方ID")
    private Long organizerId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态: 0:待审核, 1:已发布, 2:已驳回, 3:已结束")
    private Integer status;

    @Schema(description = "报名开始时间")
    private LocalDateTime regStartTime;

    @Schema(description = "报名结束时间")
    private LocalDateTime regEndTime;

    @Schema(description = "比赛开始时间")
    private LocalDateTime compStartTime;

    @Schema(description = "比赛结束时间")
    private LocalDateTime compEndTime;

    @Schema(description = "详情")
    private String description;

    @Schema(description = "联系方式")
    private String contactInfo;

    @Schema(description = "比赛地点")
    private String location;

    @Schema(description = "封面图")
    private String coverImg;

    @Schema(description = "奖项设置")
    private String awardSetting;

    @Schema(description = "附件URL")
    private String attachment;

    @Schema(description = "发布人ID")
    private Long publisherId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}