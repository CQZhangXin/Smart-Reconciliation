package com.recon.module.discrepancy.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账差异
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "recon_discrepancy", autoResultMap = true)
public class ReconDiscrepancy extends BaseEntity {

    private Long taskId;

    private Long recordId;

    private Long relatedRecordId;

    private String side;

    private String category;

    private String aiRootCause;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String aiSuggestion;

    private BigDecimal amount;

    private BigDecimal amountDiff;

    private String currency;

    private String riskLevel;

    private Long handlerId;

    private String handlerName;

    private String status;

    private String resolution;

    private String resolutionNote;

    private Long resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime slaDeadline;
}
