package com.recon.module.rule.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 对账规则配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "recon_rule_config", autoResultMap = true)
public class ReconRuleConfig extends BaseEntity {

    private String ruleName;

    private String ruleCode;

    private String ruleType;

    private String templateType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String matchConfig;

    private Integer priority;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String tolerance;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String aiConfig;

    private String status;

    private String description;

    private Integer version;

    private Long createdBy;

    private Long updatedBy;
}
