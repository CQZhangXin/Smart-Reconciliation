package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 自然语言规则生成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleGenerationResult {
    private String ruleName;
    private String ruleCode;
    private String ruleType;
    private String matchConfigJson;   // 结构化规则配置JSON
    private String toleranceJson;     // 容差配置JSON
    private String explanation;       // 规则解释
    private Integer estimatedMatchRate; // 预估匹配率
    private Boolean hasConflict;      // 是否与已有规则冲突
    private String conflictDetail;    // 冲突详情
}
