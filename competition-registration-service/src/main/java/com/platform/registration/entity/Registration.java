package com.platform.registration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_registration")
@ApiModel(value = "Registration对象", description = "竞赛报名表")
public class Registration {

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("竞赛ID")
    private Long competitionId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("学生姓名(快照)")
    private String studentName;

    @ApiModelProperty("学号(快照)")
    private String studentId;

    @ApiModelProperty("联系电话")
    private String contactPhone;

    @ApiModelProperty("团队成员")
    private String teamMembers;

    @ApiModelProperty("备注/简介")
    private String description;

    @ApiModelProperty("附件URL")
    private String attachment;

    @ApiModelProperty("状态: 0-已取消, 1-待审核, 2-已通过, 3-已驳回")
    private Integer status;

    @ApiModelProperty("审核备注")
    private String auditReason;

    @ApiModelProperty("获奖名称")
    private String awardName;

    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}