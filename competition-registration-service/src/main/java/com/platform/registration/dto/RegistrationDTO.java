package com.platform.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 报名相关请求参数包装类
 */
public class RegistrationDTO {

    @Data
    @Schema(description = "提交报名请求参数")
    public static class Apply {

        @NotNull(message = "竞赛ID不能为空")
        @Schema(description = "竞赛ID", example = "1")
        private Long competitionId;

        @NotBlank(message = "联系电话不能为空")
        @Schema(description = "联系电话 (允许修改)", example = "13800000000")
        private String contactPhone;

        @Schema(description = "团队成员 (文本描述)", example = "张三(2021001), 李四(2021002)")
        private String teamMembers;

        @Schema(description = "备注/作品简介", example = "这是我的参赛思路...")
        private String description;

        @Schema(description = "附件/作品书URL", example = "http://minio.../plan.docx")
        private String attachmentUrl;
    }
}