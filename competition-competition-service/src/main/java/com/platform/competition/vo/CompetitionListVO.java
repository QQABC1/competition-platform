package com.platform.competition.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "B端竞赛列表展示对象")
public class CompetitionListVO {

    @Schema(description = "竞赛ID")
    private Long id;

    @Schema(description = "竞赛名称")
    private String title;

    @Schema(description = "状态: 0:待审核, 1:已发布, 2:已驳回, 3:已结束")
    private Integer status;

    // 如果数据库还没加这个字段，VO里先留着，逻辑层暂时填0
    @Schema(description = "报名人数")
    private Integer regCount;

    @Schema(description = "驳回理由 (仅状态为2时显示)")
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}