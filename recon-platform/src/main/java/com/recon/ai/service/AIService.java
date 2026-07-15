package com.recon.ai.service;

import com.recon.ai.dto.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI服务统一接口 — 对所有AI能力的抽象
 *
 * <p>实现类根据部署模式选择:</p>
 * <ul>
 *   <li>MockAIService: 本地启发式 Mock（ai.llm.provider=mock）</li>
 *   <li>LlmAIService: OpenAI 兼容协议，可接 DeepSeek / 通义千问 / Kimi / OpenAI / 私有化网关</li>
 * </ul>
 */
public interface AIService {

    // ========== 字段映射 ==========

    /**
     * 智能字段映射: 给定源字段和目标字段列表,AI建议映射关系
     */
    List<FieldMappingSuggestion> suggestFieldMappings(List<String> sourceFields, List<String> targetFields);

    // ========== 语义匹配 ==========

    /**
     * 计算两条记录的语义相似度 (Embedding粗排)
     */
    double computeSemanticSimilarity(String textA, String textB);

    /**
     * LLM多维度精排打分
     * 返回5个维度的分数: 金额匹配度、日期匹配度、交易方相似度、摘要相似度、流水号匹配度
     */
    MatchScoreResult semanticMatchScore(MatchScoreRequest request);

    // ========== 差异分析 ==========

    /**
     * 差异自动分类
     */
    String classifyDiscrepancy(DiscrepancyClassifyRequest request);

    /**
     * 根因分析 (Chain-of-Thought)
     */
    RootCauseResult analyzeRootCause(RootCauseRequest request);

    /**
     * 处理建议推荐
     */
    String suggestResolution(DiscrepancyClassifyRequest request);

    // ========== 自然语言 ==========

    /**
     * 自然语言查询 → Text-to-SQL + 结果润色
     */
    NLQueryResult naturalLanguageQuery(String question, Long orgId);

    /**
     * 自然语言规则生成
     */
    RuleGenerationResult generateRuleFromNL(String naturalLanguageDesc);

    /**
     * 自然语言生成对账方案定义
     *
     * @param description       用户自然语言描述
     * @param availableSources  当前组织下可用的数据源名称列表
     * @param availableRules    当前组织下可用的规则名称列表
     * @return 对账方案定义结果（仅名称，不包含 ID）
     */
    ReconDefinitionNLResult generateReconDefinitionFromNL(
            String description, List<String> availableSources, List<String> availableRules);

    /**
     * 自然语言生成审批流程定义
     *
     * @param description 用户自然语言描述
     * @return 流程定义结果
     */
    ProcessDefNLResult generateProcessDefFromNL(String description);

    // ========== 报告生成 ==========

    /**
     * AI自动撰写对账报告
     */
    String generateReconReport(ReportGenerateRequest request);

    // ========== 异步接口 ==========

    CompletableFuture<MatchScoreResult> semanticMatchScoreAsync(MatchScoreRequest request);

    CompletableFuture<RootCauseResult> analyzeRootCauseAsync(RootCauseRequest request);
}
