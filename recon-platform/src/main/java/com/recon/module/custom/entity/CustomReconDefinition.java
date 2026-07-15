package com.recon.module.custom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 自定义对账方案定义
 *
 * <p>用户可组合多数据源（银行API、HTTP接口、ERP、文件等）与自定义规则，
 * 形成可复用的对账方案并一键执行。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "custom_recon_definition", autoResultMap = true)
public class CustomReconDefinition extends BaseEntity {

    private Long ledgerId;

    private String defName;

    private String defCode;

    private String description;

    private Long sourceAId;

    /** 对账方数据源ID列表，支持多源 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> sourceBIds;

    /** 选用规则ID列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> ruleIds;

    /** 启用匹配层: exact/rule/ai/split */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Boolean> matchLayers;

    private String periodType;

    private String defaultPeriod;

    private String status;

    private Long lastRunTaskId;

    private LocalDateTime lastRunAt;

    private Long createdBy;

    private Long updatedBy;
}
