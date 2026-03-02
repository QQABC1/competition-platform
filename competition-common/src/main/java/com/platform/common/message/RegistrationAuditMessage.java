package com.platform.common.message;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
public class RegistrationAuditMessage  {

    private Long registrationId;
    private Long userId;          // 接收通知的用户ID
    private Long competitionId;   // 关联的竞赛ID
    private Integer status;       // 审核结果：2已通过, 3已驳回
    private String auditReason;   // 审核理由
    private Long auditTime;     //审核时间戳
}