package com.recon.common.exception;

import com.recon.common.enums.ResultCode;
import com.recon.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ApiResponse.error(ResultCode.FORBIDDEN.getCode(), "无访问权限");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : UUID.randomUUID().toString();
        log.error("系统异常 [traceId={}]: ", traceId, e);
        ApiResponse<Void> response = ApiResponse.error(ResultCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
        response.setTraceId(traceId);
        return response;
    }
}
