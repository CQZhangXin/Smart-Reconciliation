package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI推理日志
 */
@Data
@Accessors(chain = true)
@TableName(value = "sys_ai_inference_log", autoResultMap = true)
public class SysAiInferenceLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String module;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String inputData;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String outputData;

    private String modelUsed;

    private Integer tokensUsed;

    private Integer latencyMs;

    private BigDecimal confidence;

    private BigDecimal costAmount;

    private String userFeedback;

    private String feedbackNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
