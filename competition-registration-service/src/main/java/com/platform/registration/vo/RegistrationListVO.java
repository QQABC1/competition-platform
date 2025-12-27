package com.platform.registration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@ApiModel("报名列表展示对象")
public class RegistrationListVO {

    @ApiModelProperty("报名记录ID")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("姓名")
    private String studentName;

    @ApiModelProperty("学号")
    private String studentId;

    @ApiModelProperty("学院/专业 (需远程获取)")
    private String collegeInfo;

    @ApiModelProperty("联系电话")
    private String contactPhone;

    @ApiModelProperty("团队成员")
    private String teamMembers;

    @ApiModelProperty("状态: 0已取消, 1待审核, 2已通过, 3已驳回")
    private Integer status;

    @ApiModelProperty("审核/驳回理由")
    private String auditReason;

    @ApiModelProperty("报名时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty("附件链接")
    private String attachment;
}