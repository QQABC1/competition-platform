package com.platform.competition.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建竞赛请求参数")
public class CompetitionCreateDTO {

    @NotBlank(message = "竞赛名称不能为空")
    @Schema(description = "竞赛名称", example = "2025年Java程序设计大赛")
    private String title;

    @NotNull(message = "主办单位不能为空")
    @Schema(description = "主办单位ID", example = "101")
    private Long organizerId;

    @NotNull(message = "竞赛类型不能为空")
    @Schema(description = "竞赛类型ID", example = "1")
    private Long categoryId;

    @NotNull(message = "报名开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "报名开始时间", example = "2025-05-01 08:00:00")
    private LocalDateTime regStartTime;

    @NotNull(message = "报名结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Future(message = "报名结束时间必须在将来")
    @Schema(description = "报名结束时间", example = "2025-05-15 18:00:00")
    private LocalDateTime regEndTime;

    @NotNull(message = "比赛开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "比赛开始时间", example = "2025-05-20 09:00:00")
    private LocalDateTime compStartTime;

    @NotNull(message = "比赛结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "比赛结束时间", example = "2025-05-20 12:00:00")
    private LocalDateTime compEndTime;

    @NotBlank(message = "竞赛简介不能为空")
    @Schema(description = "竞赛简介")
    private String description;

    @NotBlank(message = "联系方式不能为空")
    @Schema(description = "联系人及方式", example = "王老师 13800000000")
    private String contactInfo;

    @Schema(description = "比赛地点", example = "信息楼301")
    private String location;

    @Schema(description = "封面图URL")
    private String coverImg;

    @Schema(description = "奖项设置")
    private String awardSetting;

    @Schema(description = "附件URL")
    private String attachment;
}