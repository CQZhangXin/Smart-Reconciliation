package com.recon.module.datasource.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 原始记录
 */
@Data
@Accessors(chain = true)
@TableName(value = "raw_record", autoResultMap = true)
public class RawRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceId;

    private Long orgId;

    private String batchId;

    private String traceId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String rawData;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String normalizedData;

    private BigDecimal amount;

    private String currency;

    private LocalDate transactionDate;

    private LocalDate bookingDate;

    private LocalDate valueDate;

    private String transactionRef;

    private String description;

    private String counterParty;

    private String counterAcct;

    private String direction;

    private BigDecimal balance;

    private BigDecimal feeAmount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String extraInfo;

    private String hashValue;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
