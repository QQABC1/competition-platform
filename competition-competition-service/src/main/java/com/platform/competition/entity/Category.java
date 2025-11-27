package com.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("tb_category")
@Schema(description = "竞赛分类实体")
public class Category {

    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "学科竞赛")
    private String name;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态(1启用 0停用)", example = "1")
    private Integer status;
}