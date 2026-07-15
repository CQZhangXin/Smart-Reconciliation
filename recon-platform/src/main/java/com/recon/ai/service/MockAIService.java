package com.recon.ai.service;

import cn.hutool.core.util.RandomUtil;
import com.recon.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI服务Mock实现 — 用于开发和测试环境
 *
 * <p>当 ai.llm.provider 配置为 deepseek / qwen / kimi / openai / custom 时，
 * 将自动注册 productionAIService（LlmAIService），本 Bean 不再生效。</p>
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "productionAIService")
public class MockAIService implements AIService {

    private static final String MODEL = "mock-ai-v1.0";

    @Override
    public List<FieldMappingSuggestion> suggestFieldMappings(List<String> sourceFields, List<String> targetFields) {
        log.info("[MockAI] 智能字段映射: sourceFields={}, targetFields={}", sourceFields.size(), targetFields.size());
        List<FieldMappingSuggestion> suggestions = new ArrayList<>();

        // 常用字段映射词典
        Map<String, String> mappingDict = new LinkedHashMap<>();
        mappingDict.put("amount", "交易金额");
        mappingDict.put("transaction_date", "交易日期");
        mappingDict.put("description", "交易摘要");
        mappingDict.put("counter_party", "对方名称");
        mappingDict.put("transaction_ref", "流水号");
        mappingDict.put("currency", "币种");
        mappingDict.put("direction", "借贷方向");
        mappingDict.put("fee", "手续费");

        for (String sf : sourceFields) {
            String sfLower = sf.toLowerCase();
            for (Map.Entry<String, String> entry : mappingDict.entrySet()) {
                if (sfLower.contains(entry.getKey()) || entry.getValue().contains(sf)) {
                    suggestions.add(FieldMappingSuggestion.builder()
                            .sourceField(sf)
                            .targetField(entry.getValue())
                            .confidence(80 + RandomUtil.randomInt(10, 20))
                            .fieldType("STRING")
                            .explanation("基于语义相似度自动匹配")
                            .build());
                    break;
                }
            }
        }
        return suggestions;
    }

    @Override
    public double computeSemanticSimilarity(String textA, String textB) {
        // 简化版: 基于公共子串长度计算相似度
        if (textA == null || textB == null) return 0.0;
        Set<String> wordsA = new HashSet<>(Arrays.asList(textA.split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(textB.split("\\s+")));
        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);
        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public MatchScoreResult semanticMatchScore(MatchScoreRequest request) {
        log.info("[MockAI] 语义匹配打分: {} vs {}", request.getRecordAText(), request.getRecordBText());

        // 模拟多维度打分
        BigDecimal amountScore = calcAmountScore(request.getAmountA(), request.getAmountB());
        BigDecimal dateScore = calcDateScore(request.getDateA(), request.getDateB());
        BigDecimal partyScore = BigDecimal.valueOf(computeSemanticSimilarity(request.getPartyA(), request.getPartyB()));
        BigDecimal descScore = BigDecimal.valueOf(computeSemanticSimilarity(request.getDescriptionA(), request.getDescriptionB()));
        BigDecimal refScore = calcRefScore(request.getRefA(), request.getRefB());

        // 加权综合得分: 金额30% + 日期20% + 交易方25% + 摘要15% + 流水号10%
        BigDecimal overall = amountScore.multiply(new BigDecimal("0.30"))
                .add(dateScore.multiply(new BigDecimal("0.20")))
                .add(partyScore.multiply(new BigDecimal("0.25")))
                .add(descScore.multiply(new BigDecimal("0.15")))
                .add(refScore.multiply(new BigDecimal("0.10")))
                .setScale(4, RoundingMode.HALF_UP);

        String action;
        if (overall.compareTo(new BigDecimal("0.85")) >= 0) {
            action = "AUTO_CONFIRM";
        } else if (overall.compareTo(new BigDecimal("0.70")) >= 0) {
            action = "RECOMMEND";
        } else {
            action = "UNMATCHED";
        }

        return MatchScoreResult.builder()
                .overallScore(overall)
                .amountScore(amountScore)
                .dateScore(dateScore)
                .partyScore(partyScore)
                .descriptionScore(descScore)
                .referenceScore(refScore)
                .explanation(String.format("金额匹配度=%.2f, 日期匹配度=%.2f, 交易方相似度=%.2f, 摘要相似度=%.2f",
                        amountScore, dateScore, partyScore, descScore))
                .recommendedAction(action)
                .modelUsed(MODEL)
                .tokensUsed(150)
                .latencyMs(50L)
                .build();
    }

    @Override
    public String classifyDiscrepancy(DiscrepancyClassifyRequest request) {
        log.info("[MockAI] 差异分类: amountDiff={}", request.getAmountDiff());
        BigDecimal diff = request.getAmountDiff().abs();

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            // 金额相同但未匹配 → 可能是日期差异
            if (request.getDateA() != null && request.getDateB() != null
                    && !request.getDateA().equals(request.getDateB())) {
                return "TIME_DIFF";
            }
            return "UNKNOWN";
        }

        // 小额差异 → 可能是手续费
        if (diff.compareTo(new BigDecimal("50")) <= 0) {
            return "FEE_DIFF";
        }

        // 跨境交易且差异在合理汇率范围内 → 汇率差异
        if (Boolean.TRUE.equals(request.getCrossBorder())) {
            BigDecimal amountA = request.getAmountA() != null ? request.getAmountA() : BigDecimal.ONE;
            BigDecimal ratio = diff.divide(amountA, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.0001")) >= 0 && ratio.compareTo(new BigDecimal("0.05")) <= 0) {
                return "EXCHANGE_DIFF";
            }
        }

        return "UNKNOWN";
    }

    @Override
    public RootCauseResult analyzeRootCause(RootCauseRequest request) {
        log.info("[MockAI] 根因分析: category={}, discrepancyId={}", request.getCategory(), request.getDiscrepancyId());
        return RootCauseResult.builder()
                .rootCauseCategory(request.getCategory())
                .analysisSteps("Step 1: 分析金额差异特征 → 差异金额在合理范围内\n"
                        + "Step 2: 检查交易时间 → 时间差在T+1结算周期内\n"
                        + "Step 3: 综合判断 → 属于正常结算时间差")
                .suggestion("建议等待T+1日自动到账后再次对账，或将此差异标记为'时间差'类别")
                .riskLevel("LOW")
                .confidence(85)
                .modelUsed(MODEL)
                .tokensUsed(300)
                .build();
    }

    @Override
    public String suggestResolution(DiscrepancyClassifyRequest request) {
        return "建议将该差异标记为已识别类型，待下个对账周期自动验证";
    }

    @Override
    public NLQueryResult naturalLanguageQuery(String question, Long orgId) {
        log.info("[MockAI] NL查询: question={}", question);
        return NLQueryResult.builder()
                .question(question)
                .generatedSql("-- TODO: 使用参数化查询，不应直接拼接 orgId 到 SQL 中\n" +
                        "            SELECT * FROM recon_discrepancy WHERE org_id = " + orgId + " LIMIT 10")
                .data(Collections.emptyList())
                .answer("这是模拟的NL查询回答。在生产环境中，此处将返回基于实际数据的查询结果和分析。")
                .extractedEntities(Map.of("intent", "QUERY"))
                .queryTimeMs(100L)
                .build();
    }

    @Override
    public RuleGenerationResult generateRuleFromNL(String naturalLanguageDesc) {
        log.info("[MockAI] NL规则生成: {}", naturalLanguageDesc);
        return RuleGenerationResult.builder()
                .ruleName("AI生成的规则")
                .ruleCode("AI_GEN_" + System.currentTimeMillis())
                .ruleType("RULE_MATCH")
                .matchConfigJson("{\"conditions\":[{\"field\":\"amount\",\"operator\":\"eq\"}]}")
                .toleranceJson("{\"amount_abs\":5,\"date_days\":2}")
                .explanation("根据自然语言描述自动生成的匹配规则")
                .estimatedMatchRate(85)
                .hasConflict(false)
                .build();
    }

    @Override
    public ReconDefinitionNLResult generateReconDefinitionFromNL(
            String description, List<String> availableSources, List<String> availableRules) {
        log.info("[MockAI] NL对账方案生成: {}", description);

        String sourceA = availableSources != null && !availableSources.isEmpty()
                ? availableSources.get(0) : "";
        List<String> sourceBs = availableSources != null && availableSources.size() > 1
                ? List.of(availableSources.get(1)) : List.of();
        List<String> rules = availableRules != null && !availableRules.isEmpty()
                ? List.of(availableRules.get(0)) : List.of();

        return ReconDefinitionNLResult.builder()
                .defName("Mock生成-" + System.currentTimeMillis() % 10000)
                .defCode("MOCK_GEN_" + System.currentTimeMillis())
                .description("Mock生成: " + description)
                .sourceAName(sourceA)
                .sourceBNames(sourceBs)
                .ruleNames(rules)
                .matchLayers(Map.of("exact", true, "rule", true, "ai", true, "split", false))
                .periodType("MONTHLY")
                .defaultPeriod("")
                .aiExplanation("Mock模式: 默认取第一个可用数据源为A方，第二个为B方。请检查并调整。")
                .build();
    }

    @Override
    public ProcessDefNLResult generateProcessDefFromNL(String description) {
        log.info("[MockAI] NL流程定义生成: {}", description);

        List<ProcessDefNLResult.ProcessStepItem> steps = new ArrayList<>();
        steps.add(ProcessDefNLResult.ProcessStepItem.builder()
                .order(1).name("部门经理审批").approverRole("DEPT_MANAGER").build());
        steps.add(ProcessDefNLResult.ProcessStepItem.builder()
                .order(2).name("财务复核").approverRole("FINANCE").build());

        return ProcessDefNLResult.builder()
                .processName("Mock生成流程-" + System.currentTimeMillis() % 10000)
                .processKey("MOCK_PROCESS_" + System.currentTimeMillis())
                .description("Mock生成: " + description)
                .steps(steps)
                .aiExplanation("Mock模式: 默认生成两级审批流程（部门经理 → 财务复核）。请检查并调整。")
                .build();
    }

    @Override
    public String generateReconReport(ReportGenerateRequest request) {
        log.info("[MockAI] 报告生成: type={}, period={}", request.getReportType(), request.getPeriod());
        return String.format("""
                # 对账报告 - %s

                ## 概览
                - 对账期间: %s
                - 自动匹配率: %s
                - 匹配成功: %d 笔
                - 差异数量: %d 笔

                ## AI分析
                本期间对账运行正常，自动匹配率处于行业优秀水平。
                建议关注差异处理时效性，确保月结前完成所有差异处理。
                """, request.getReportType(), request.getPeriod(),
                request.getMatchRate(), request.getMatchCount(), request.getDiscrepancyCount());
    }

    @Override
    @Async
    public CompletableFuture<MatchScoreResult> semanticMatchScoreAsync(MatchScoreRequest request) {
        return CompletableFuture.completedFuture(semanticMatchScore(request));
    }

    @Override
    @Async
    public CompletableFuture<RootCauseResult> analyzeRootCauseAsync(RootCauseRequest request) {
        return CompletableFuture.completedFuture(analyzeRootCause(request));
    }

    // ========== 私有辅助方法 ==========

    private BigDecimal calcAmountScore(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || a.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal diff = a.subtract(b).abs();
        BigDecimal ratio = diff.divide(a.abs(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.001")) <= 0) return new BigDecimal("0.98");
        if (ratio.compareTo(new BigDecimal("0.01")) <= 0) return new BigDecimal("0.90");
        if (ratio.compareTo(new BigDecimal("0.05")) <= 0) return new BigDecimal("0.70");
        return new BigDecimal("0.30");
    }

    private BigDecimal calcDateScore(java.time.LocalDate a, java.time.LocalDate b) {
        if (a == null || b == null) return new BigDecimal("0.50");
        long days = Math.abs(a.toEpochDay() - b.toEpochDay());
        if (days == 0) return new BigDecimal("1.00");
        if (days == 1) return new BigDecimal("0.90");
        if (days <= 3) return new BigDecimal("0.70");
        if (days <= 7) return new BigDecimal("0.40");
        return BigDecimal.ZERO;
    }

    private BigDecimal calcRefScore(String refA, String refB) {
        if (refA == null || refB == null) return new BigDecimal("0.50");
        if (refA.equals(refB)) return new BigDecimal("1.00");
        if (refA.contains(refB) || refB.contains(refA)) return new BigDecimal("0.80");
        return new BigDecimal("0.20");
    }
}
