package com.platform.api.domain;

import lombok.Data;

/**
 * 用户内部信息VO (供微服务间调用使用)
 */
@Data
public class UserInternalVO {
    private Long id;
    private String studentId;
    private String realName;
    private String major;
    private String phone;
    private Integer isInfoComplete;
}