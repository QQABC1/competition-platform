package com.platform.competition.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "竞赛详情VO")
public class CompetitionDetailVO {

    @Schema(description = "竞赛ID")
    private Long id;

    @Schema(description = "竞赛名称")
    private String title;

    @Schema(description = "封面图")
    private String coverImg;

    @Schema(description = "主办方ID")
    private Long organizerId;

    @Schema(description = "主办方名称 (已转换)")
    private String organizerName;

    @Schema(description = "分类名称 (已转换)")
    private String categoryName;

    @Schema(description = "竞赛简介(富文本)")
    private String description;

    @Schema(description = "联系方式")
    private String contactInfo;

    @Schema(description = "比赛地点")
    private String location;

    @Schema(description = "奖项设置")
    private String awardSetting;

    @Schema(description = "附件URL")
    private String attachment;

    @Schema(description = "报名开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regStartTime;

    @Schema(description = "报名结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regEndTime;

    @Schema(description = "比赛时间段") // 可以拼接成一个字符串给前端，也可以分开
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime compStartTime;

    @Schema(description = "比赛结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime compEndTime;

    // --- 核心计算字段 ---

    @Schema(description = "能否报名 (true:按钮亮起, false:按钮变灰)")
    private Boolean canRegister;

    @Schema(description = "报名状态文本 (未开始 / 报名中 / 已截止)")
    private String regStatusText;

    @Schema(description = "浏览量 (来自Redis)")
    private Long viewCount;
}