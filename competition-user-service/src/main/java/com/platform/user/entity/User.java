package com.platform.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("tb_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String studentId;
    private String realName;
    private String major;
    private Integer isInfoComplete; // 0:未完善, 1:已完善
}