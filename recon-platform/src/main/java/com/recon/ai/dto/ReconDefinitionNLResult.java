package com.recon.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI 自然语言生成对账方案定义结果 — 仅包含名称，不包含数据库 ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconDefinitionNLResult {

    private String defName;

    private String defCode;

    private String description;

    /** AI 识别的主数据源名称（未解析为 ID） */
    private String sourceAName;

    /** AI 识别的对账方数据源名称列表（未解析为 ID） */
    private List<String> sourceBNames;

    /** AI 识别的规则名称列表（未解析为 ID） */
    private List<String> ruleNames;

    /** 匹配层启用情况 */
    private Map<String, Boolean> matchLayers;

    /** 期间类型: DAILY / MONTHLY / CUSTOM */
    private String periodType;

    /** 默认对账期间，如 "2026-07" */
    private String defaultPeriod;

    /** AI 解析思路说明 */
    private String aiExplanation;
}
