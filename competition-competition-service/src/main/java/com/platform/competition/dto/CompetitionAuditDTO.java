package com.platform.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "竞赛审核请求参数")
public class CompetitionAuditDTO {

    @NotNull(message = "竞赛ID不能为空")
    @Schema(description = "竞赛ID", required = true, example = "1")
    private Long id;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "是否通过 (true:通过, false:驳回)", required = true, example = "true")
    private Boolean pass;

    @Schema(description = "驳回理由 (仅当pass=false时必填)", example = "信息填写不完整，请补充")
    private String reason;
}