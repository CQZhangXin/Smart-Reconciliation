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
 * 对账调整
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "recon_adjustment", autoResultMap = true)
public class ReconAdjustment extends BaseEntity {

    private Long discrepancyId;

    private Long ledgerId;

    private String adjustmentType;

    private BigDecimal amount;

    private String currency;

    private String debitAccount;

    private String creditAccount;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String attachmentUrls;

    private String status;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long createdBy;
}
