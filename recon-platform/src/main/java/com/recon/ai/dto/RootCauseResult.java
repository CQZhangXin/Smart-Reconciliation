package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 根因分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootCauseResult {
    /** 根因类别 */
    private String rootCauseCategory;
    /** 分析过程(Chain-of-Thought步骤) */
    private String analysisSteps;
    /** 处理建议 */
    private String suggestion;
    /** 风险等级 */
    private String riskLevel;
    /** 建议的调整分录 */
    private String suggestedAdjustment;
    /** 置信度 */
    private Integer confidence;
    /** 使用的模型 */
    private String modelUsed;
    /** 消耗Token数 */
    private Integer tokensUsed;
}
