package com.recon.module.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账匹配结果
 */
@Data
@Accessors(chain = true)
@TableName(value = "recon_match", autoResultMap = true)
public class ReconMatch implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long orgId;

    private Long recordAId;

    private Long recordBId;

    private String matchType;

    private BigDecimal confidence;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String matchDimensions;

    private String aiExplanation;

    private BigDecimal amountA;

    private BigDecimal amountB;

    private BigDecimal amountDiff;

    private Integer dateDiffDays;

    private String status;

    private Long reviewedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    private String reviewComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
