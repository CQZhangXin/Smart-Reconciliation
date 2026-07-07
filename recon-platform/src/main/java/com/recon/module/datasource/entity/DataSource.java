package com.recon.module.datasource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据源
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "datasource", autoResultMap = true)
public class DataSource extends BaseEntity {

    private Long ledgerId;

    private String dsName;

    private String dsType;

    private String dsCategory;

    private String provider;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String connConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String fieldMapping;

    private String syncStrategy;

    private String syncCron;

    private LocalDateTime lastSyncAt;

    private String lastSyncStatus;

    private String healthStatus;

    private String status;

    private String description;

    private Long createdBy;

    private Long updatedBy;
}
