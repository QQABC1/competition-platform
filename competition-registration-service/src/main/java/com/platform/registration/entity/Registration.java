package com.platform.registration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_registration")
public class Registration {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long competitionId;
    private Long userId;

    private String contactPhone;
    private String teamMembers;
    private String description;
    private String attachment;

    private Integer status; // 1:已报名, 0:已取消
    private String awardName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}