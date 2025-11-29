package com.platform.competition.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "待审核竞赛列表VO")
public class CompetitionAuditVO {

    @Schema(description = "竞赛ID")
    private Long id;

    @Schema(description = "竞赛名称")
    private String title;

    @Schema(description = "主办方ID")
    private Long organizerId;

    @Schema(description = "主办方名称 (已转换)")
    private String organizerName;

    @Schema(description = "申请人ID")
    private Long publisherId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}