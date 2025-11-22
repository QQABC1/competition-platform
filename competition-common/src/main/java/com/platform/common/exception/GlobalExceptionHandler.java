package com.platform.common.exception;

import com.platform.common.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 作用：拦截所有Controller层抛出的异常，统一转为JSON格式返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 拦截业务异常 (BusinessException)
     * 场景：我们代码里手动 throw new BusinessException("xxx") 的情况
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 2. 拦截参数校验异常 (Validation)
     * 场景：DTO中的 @NotNull, @Size 等校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidationException(MethodArgumentNotValidException e) {
        // 获取所有字段的错误信息，拼接成字符串
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("参数校验异常: {}", message);
        return R.fail(400, "参数错误: " + message);
    }

    /**
     * 3. 拦截所有未知异常 (Exception) - 兜底策略
     * 场景：空指针(NPE)、数组越界、数据库连接失败等未捕获的错误
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        // 打印完整的堆栈日志，方便后端排查BUG (非常重要！)
        log.error("系统未知异常", e);

        // 返回给前端模糊的提示，不要把具体的报错堆栈返给前端（安全考虑）
        return R.fail(500, "系统繁忙，请稍后再试");
    }
}