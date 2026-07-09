package com.recon.module.engine.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "recon_task", autoResultMap = true)
public class ReconTask extends BaseEntity {

    private Long ledgerId;

    private String taskName;

    private String taskType;

    private Long sourceAId;

    private Long sourceBId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String ruleIds;

    private String reconPeriod;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String status;

    private Integer totalACount;

    private Integer totalBCount;

    private Integer matchedCount;

    private Integer unmatchedCount;

    private Integer discrepancyCount;

    private BigDecimal matchRate;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String matchSummary;

    private String errorMsg;

    private String priority;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Long durationMs;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime scheduledAt;

    private Integer retryCount;

    private Long parentTaskId;
}
