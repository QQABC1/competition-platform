package com.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("tb_organizer")
@Schema(description = "主办单位实体")
public class Organizer {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主办方ID", example = "101")
    private Long id;

    @Schema(description = "单位名称", example = "计算机学院")
    private String name;

    @Schema(description = "默认联系人", example = "张老师")
    private String contactPerson;

    @Schema(description = "默认联系电话", example = "010-12345678")
    private String contactPhone;
}