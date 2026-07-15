package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志
 *
 * <p><b>安全警告:</b> 本实体的 {@code requestParams} 和 {@code requestBody} 字段会完整存储
 * HTTP请求的参数和请求体。服务层在调用审计日志记录之前<b>必须</b>对包含敏感信息
 * (如密码、token、密钥等) 的字段进行脱敏处理，严禁将明文敏感数据写入审计表。
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName(value = "sys_audit_log", autoResultMap = true)
public class SysAuditLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long userId;

    private String username;

    private String operation;

    private String module;

    private String targetType;

    private String targetId;

    private String requestUrl;

    private String requestMethod;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String requestParams;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String requestBody;

    private Integer responseStatus;

    private String clientIp;

    private String userAgent;

    private Long durationMs;

    private String detail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
