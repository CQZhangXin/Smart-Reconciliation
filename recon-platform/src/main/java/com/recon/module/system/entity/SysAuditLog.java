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
