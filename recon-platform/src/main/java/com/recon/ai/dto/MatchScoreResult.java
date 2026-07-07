package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * AI语义匹配打分结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreResult {
    /** 综合加权得分 (0-1) */
    private BigDecimal overallScore;
    /** 金额匹配度 (0-1) */
    private BigDecimal amountScore;
    /** 日期匹配度 (0-1) */
    private BigDecimal dateScore;
    /** 交易方语义相似度 (0-1) */
    private BigDecimal partyScore;
    /** 摘要语义相似度 (0-1) */
    private BigDecimal descriptionScore;
    /** 流水号匹配度 (0-1) */
    private BigDecimal referenceScore;
    /** AI匹配解释 */
    private String explanation;
    /** 推荐动作: AUTO_CONFIRM / RECOMMEND / UNMATCHED */
    private String recommendedAction;
    /** 使用的模型 */
    private String modelUsed;
    /** 消耗Token数 */
    private Integer tokensUsed;
    /** 延迟(毫秒) */
    private Long latencyMs;
}
