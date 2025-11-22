package com.platform.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * 用途：用于在业务代码中主动抛出已知错误
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    // 默认错误码 500
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    // 自定义错误码 (例如 401, 403)
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}