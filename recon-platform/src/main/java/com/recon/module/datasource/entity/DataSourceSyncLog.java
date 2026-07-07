package com.recon.module.datasource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据源同步日志
 */
@Data
@Accessors(chain = true)
@TableName("datasource_sync_log")
public class DataSourceSyncLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceId;

    private Long orgId;

    private String syncType;

    private Integer totalCount;

    private Integer successCount;

    private Integer errorCount;

    private String syncStatus;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    private Long durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
