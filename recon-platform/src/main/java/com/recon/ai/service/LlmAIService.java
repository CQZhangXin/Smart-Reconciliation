package com.recon.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.recon.ai.client.OpenAiCompatibleClient;
import com.recon.ai.config.AiProperties;
import com.recon.ai.dto.DiscrepancyClassifyRequest;
import com.recon.ai.dto.FieldMappingSuggestion;
import com.recon.ai.dto.MatchScoreRequest;
import com.recon.ai.dto.MatchScoreResult;
import com.recon.ai.dto.NLQueryResult;
import com.recon.ai.dto.ProcessDefNLResult;
import com.recon.ai.dto.ReportGenerateRequest;
import com.recon.ai.dto.RootCauseRequest;
import com.recon.ai.dto.RootCauseResult;
import com.recon.ai.dto.ReconDefinitionNLResult;
import com.recon.ai.dto.RuleGenerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 基于国内/国际大模型的 AI 服务实现（OpenAI 兼容协议）
 *
 * <p>支持 DeepSeek、通义千问、Kimi、OpenAI 等，由 ai.llm.provider 切换。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LlmAIService implements AIService {

    private final AiProperties properties;
    private final OpenAiCompatibleClient llmClient;

    @Override
    public List<FieldMappingSuggestion> suggestFieldMappings(List<String> sourceFields, List<String> targetFields) {
        String model = llmClient.resolveModel("simple");
        String userPrompt = """
                请将源字段映射到目标标准字段，只返回 JSON：
                {"mappings":[{"sourceField":"...","targetField":"...","confidence":0-100,"fieldType":"STRING","explanation":"..."}]}
                源字段: %s
                目标字段: %s
                """.formatted(JSONUtil.toJsonStr(sourceFields), JSONUtil.toJsonStr(targetFields));
        try {
            OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                    model,
                    "你是财务对账数据映射专家，只输出合法 JSON。",
                    userPrompt,
                    true);
            JSONObject root = parseJsonObject(chat.getContent());
            List<FieldMappingSuggestion> list = new ArrayList<>();
            if (root.getJSONArray("mappings") != null) {
                root.getJSONArray("mappings").forEach(item -> {
                    JSONObject obj = (JSONObject) item;
                    list.add(FieldMappingSuggestion.builder()
                            .sourceField(obj.getStr("sourceField"))
                            .targetField(obj.getStr("targetField"))
                            .confidence(obj.getInt("confidence", 80))
                            .fieldType(obj.getStr("fieldType", "STRING"))
                            .explanation(obj.getStr("explanation"))
                            .build());
                });
            }
            return list;
        } catch (Exception ex) {
            log.warn("LLM 字段映射失败，回退启发式: {}", ex.getMessage());
            return heuristicFieldMappings(sourceFields);
        }
    }

    @Override
    public double computeSemanticSimilarity(String textA, String textB) {
        if (StrUtil.isBlank(textA) || StrUtil.isBlank(textB)) {
            return 0.0;
        }
        Set<String> wordsA = new HashSet<>(Arrays.asList(textA.split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(textB.split("\\s+")));
        // 中文按字符 bigram 近似
        if (containsCjk(textA) || containsCjk(textB)) {
            wordsA = bigrams(textA);
            wordsB = bigrams(textB);
        }
        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);
        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public MatchScoreResult semanticMatchScore(MatchScoreRequest request) {
        String model = llmClient.resolveModel("reasoning");
        String userPrompt = """
                请对两条财务交易记录做语义匹配打分，只返回 JSON：
                {
                  "overallScore":0-1,
                  "amountScore":0-1,
                  "dateScore":0-1,
                  "partyScore":0-1,
                  "descriptionScore":0-1,
                  "referenceScore":0-1,
                  "explanation":"中文解释",
                  "recommendedAction":"AUTO_CONFIRM|RECOMMEND|UNMATCHED"
                }
                阈值建议：>=0.85 AUTO_CONFIRM，0.70-0.85 RECOMMEND，否则 UNMATCHED。
                记录A文本: %s
                记录B文本: %s
                金额A: %s, 金额B: %s
                日期A: %s, 日期B: %s
                交易方A: %s, 交易方B: %s
                摘要A: %s, 摘要B: %s
                流水号A: %s, 流水号B: %s
                """.formatted(
                nvl(request.getRecordAText()), nvl(request.getRecordBText()),
                request.getAmountA(), request.getAmountB(),
                request.getDateA(), request.getDateB(),
                nvl(request.getPartyA()), nvl(request.getPartyB()),
                nvl(request.getDescriptionA()), nvl(request.getDescriptionB()),
                nvl(request.getRefA()), nvl(request.getRefB()));

        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model,
                "你是资深财务对账专家，擅长中文摘要与交易方语义理解，只输出 JSON。",
                userPrompt,
                true);
        JSONObject root = parseJsonObject(chat.getContent());

        BigDecimal overall = decimal(root, "overallScore");
        String action = root.getStr("recommendedAction");
        if (StrUtil.isBlank(action)) {
            double auto = properties.getSemanticMatch().getLlmThresholdAuto();
            double recommend = properties.getSemanticMatch().getLlmThresholdRecommend();
            if (overall.doubleValue() >= auto) {
                action = "AUTO_CONFIRM";
            } else if (overall.doubleValue() >= recommend) {
                action = "RECOMMEND";
            } else {
                action = "UNMATCHED";
            }
        }

        return MatchScoreResult.builder()
                .overallScore(overall)
                .amountScore(decimal(root, "amountScore"))
                .dateScore(decimal(root, "dateScore"))
                .partyScore(decimal(root, "partyScore"))
                .descriptionScore(decimal(root, "descriptionScore"))
                .referenceScore(decimal(root, "referenceScore"))
                .explanation(root.getStr("explanation"))
                .recommendedAction(action)
                .modelUsed(chat.getModel())
                .tokensUsed(chat.getTotalTokens())
                .latencyMs(chat.getLatencyMs())
                .build();
    }

    @Override
    public String classifyDiscrepancy(DiscrepancyClassifyRequest request) {
        String model = llmClient.resolveModel("simple");
        String userPrompt = """
                对账差异分类，只返回 JSON：{"category":"TIME_DIFF|FEE_DIFF|EXCHANGE_DIFF|DATA_ENTRY_ERROR|UNREACHED|DUPLICATE|OTHER_SIDE_UNRECORDED|UNKNOWN"}
                金额差: %s, 币种: %s
                日期A: %s, 日期B: %s
                摘要A: %s, 摘要B: %s
                交易方A: %s, 交易方B: %s
                跨境: %s, 含手续费: %s
                """.formatted(
                request.getAmountDiff(), request.getCurrency(),
                request.getDateA(), request.getDateB(),
                nvl(request.getDescriptionA()), nvl(request.getDescriptionB()),
                nvl(request.getPartyA()), nvl(request.getPartyB()),
                request.getCrossBorder(), request.getHasFee());
        try {
            OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                    model, "你是财务差异分类助手，只输出 JSON。", userPrompt, true);
            JSONObject root = parseJsonObject(chat.getContent());
            String category = root.getStr("category", "UNKNOWN");
            return StrUtil.blankToDefault(category, "UNKNOWN");
        } catch (Exception ex) {
            log.warn("LLM 差异分类失败，回退启发式: {}", ex.getMessage());
            return heuristicClassify(request);
        }
    }

    @Override
    public RootCauseResult analyzeRootCause(RootCauseRequest request) {
        String model = llmClient.resolveModel("reasoning");
        String userPrompt = """
                请对对账差异做 Chain-of-Thought 根因分析，只返回 JSON：
                {
                  "rootCauseCategory":"...",
                  "analysisSteps":"分步推理，换行分隔",
                  "suggestion":"处理建议",
                  "riskLevel":"LOW|MEDIUM|HIGH|CRITICAL",
                  "suggestedAdjustment":"建议调整分录或留空",
                  "confidence":0-100
                }
                已知分类: %s
                记录A: %s
                记录B: %s
                账户类型: %s, 币种: %s, 行业: %s
                """.formatted(
                nvl(request.getCategory()),
                nvl(request.getRecordAJson()),
                nvl(request.getRecordBJson()),
                nvl(request.getAccountType()),
                nvl(request.getCurrency()),
                nvl(request.getIndustryType()));

        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model,
                "你是资深财务主管，用中文做可审计的根因分析，只输出 JSON。",
                userPrompt,
                true);
        JSONObject root = parseJsonObject(chat.getContent());
        return RootCauseResult.builder()
                .rootCauseCategory(root.getStr("rootCauseCategory", request.getCategory()))
                .analysisSteps(root.getStr("analysisSteps"))
                .suggestion(root.getStr("suggestion"))
                .riskLevel(root.getStr("riskLevel", "MEDIUM"))
                .suggestedAdjustment(root.getStr("suggestedAdjustment"))
                .confidence(root.getInt("confidence", 70))
                .modelUsed(chat.getModel())
                .tokensUsed(chat.getTotalTokens())
                .build();
    }

    @Override
    public String suggestResolution(DiscrepancyClassifyRequest request) {
        String model = llmClient.resolveModel("simple");
        String userPrompt = """
                给出对账差异处理建议，只返回 JSON：{"suggestion":"..."}
                金额差: %s, 摘要A: %s, 摘要B: %s
                """.formatted(request.getAmountDiff(),
                nvl(request.getDescriptionA()), nvl(request.getDescriptionB()));
        try {
            OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                    model, "你是财务对账处理顾问，只输出 JSON。", userPrompt, true);
            return parseJsonObject(chat.getContent()).getStr("suggestion",
                    "建议复核原始凭证并按分类处理");
        } catch (Exception ex) {
            return "建议复核原始凭证，确认差异类型后按流程处理";
        }
    }

    @Override
    public NLQueryResult naturalLanguageQuery(String question, Long orgId) {
        String model = llmClient.resolveModel("reasoning");
        String userPrompt = """
                将财务对账自然语言问题转为只读 SQL 建议，只返回 JSON：
                {
                  "generatedSql":"仅 SELECT，表可用 recon_task/recon_discrepancy/recon_match/raw_record，必须带 org_id 条件占位 :orgId",
                  "answer":"中文回答思路",
                  "extractedEntities":{"intent":"..."}
                }
                组织ID: %s
                问题: %s
                """.formatted(orgId, question);

        long start = System.currentTimeMillis();
        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model,
                "你是 Text-to-SQL 助手，只生成只读查询，禁止增删改，只输出 JSON。",
                userPrompt,
                true);
        JSONObject root = parseJsonObject(chat.getContent());
        Map<String, Object> entities = new LinkedHashMap<>();
        if (root.get("extractedEntities") instanceof JSONObject ent) {
            entities.putAll(ent);
        } else {
            entities.put("intent", "QUERY");
        }
        return NLQueryResult.builder()
                .question(question)
                .generatedSql(root.getStr("generatedSql"))
                .data(Collections.emptyList())
                .answer(root.getStr("answer"))
                .extractedEntities(entities)
                .queryTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public RuleGenerationResult generateRuleFromNL(String naturalLanguageDesc) {
        String model = llmClient.resolveModel("default");
        String userPrompt = """
                根据自然语言生成对账规则，只返回 JSON：
                {
                  "ruleName":"...",
                  "ruleCode":"大写字母数字下划线",
                  "ruleType":"RULE_MATCH|EXACT_MATCH|AI_SEMANTIC|AI_SPLIT",
                  "matchConfigJson":"{\\"conditions\\":[{\\"field\\":\\"amount\\",\\"operator\\":\\"eq\\"}]}",
                  "toleranceJson":"{\\"amount_abs\\":0,\\"amount_pct\\":0,\\"date_days\\":1}",
                  "explanation":"...",
                  "estimatedMatchRate":0-100,
                  "hasConflict":false,
                  "conflictDetail":""
                }
                描述: %s
                """.formatted(naturalLanguageDesc);

        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model,
                "你是对账规则引擎专家，只输出 JSON。",
                userPrompt,
                true);
        JSONObject root = parseJsonObject(chat.getContent());
        String matchConfig = root.getStr("matchConfigJson");
        if (StrUtil.isBlank(matchConfig) && root.get("matchConfig") != null) {
            matchConfig = JSONUtil.toJsonStr(root.get("matchConfig"));
        }
        String tolerance = root.getStr("toleranceJson");
        if (StrUtil.isBlank(tolerance) && root.get("tolerance") != null) {
            tolerance = JSONUtil.toJsonStr(root.get("tolerance"));
        }
        return RuleGenerationResult.builder()
                .ruleName(root.getStr("ruleName", "AI生成规则"))
                .ruleCode(root.getStr("ruleCode", "AI_GEN_" + System.currentTimeMillis()))
                .ruleType(root.getStr("ruleType", "RULE_MATCH"))
                .matchConfigJson(StrUtil.blankToDefault(matchConfig,
                        "{\"conditions\":[{\"field\":\"amount\",\"operator\":\"eq\"}]}"))
                .toleranceJson(StrUtil.blankToDefault(tolerance,
                        "{\"amount_abs\":0,\"date_days\":1}"))
                .explanation(root.getStr("explanation"))
                .estimatedMatchRate(root.getInt("estimatedMatchRate", 80))
                .hasConflict(root.getBool("hasConflict", false))
                .conflictDetail(root.getStr("conflictDetail"))
                .build();
    }

    @Override
    public ReconDefinitionNLResult generateReconDefinitionFromNL(
            String description, List<String> availableSources, List<String> availableRules) {
        String model = llmClient.resolveModel("default");
        String sourcesJson = JSONUtil.toJsonStr(availableSources != null ? availableSources : List.of());
        String rulesJson = JSONUtil.toJsonStr(availableRules != null ? availableRules : List.of());
        String userPrompt = """
                根据以下信息生成对账方案配置，只返回JSON：

                可用的数据源名称列表：
                %s

                可用的规则名称列表：
                %s

                用户的自然语言描述：
                %s

                返回格式必须严格遵循：
                {
                  "defName": "方案名称（简洁有辨识度，中文）",
                  "defCode": "方案编码（大写字母数字下划线，如CUSTOM_BANK_SAP）",
                  "description": "方案说明（包含用户原文）",
                  "sourceAName": "主数据源名称（从可用数据源列表中语义匹配选择，若无可留空字符串）",
                  "sourceBNames": ["对账方数据源名称列表（从可用数据源列表中选择，支持多个）"],
                  "ruleNames": ["规则名称列表（从可用规则列表中选择，若无匹配可留空数组）"],
                  "matchLayers": {"exact": true, "rule": true, "ai": true, "split": true},
                  "periodType": "DAILY/MONTHLY/CUSTOM（根据描述判断，默认MONTHLY）",
                  "defaultPeriod": "对账期间如2026-07",
                  "aiExplanation": "中文解释AI的解析思路"
                }
                """.formatted(sourcesJson, rulesJson, description);

        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model,
                "你是资深财务对账方案设计专家。只能从提供的可用数据源和规则列表中选择匹配项。如果找不到精确匹配，选择语义最接近的。只输出合法JSON。",
                userPrompt,
                true);
        JSONObject root = parseJsonObject(chat.getContent());

        List<String> sourceBNames = new ArrayList<>();
        if (root.getJSONArray("sourceBNames") != null) {
            root.getJSONArray("sourceBNames").forEach(item -> sourceBNames.add(item.toString()));
        }
        List<String> ruleNames = new ArrayList<>();
        if (root.getJSONArray("ruleNames") != null) {
            root.getJSONArray("ruleNames").forEach(item -> ruleNames.add(item.toString()));
        }
        Map<String, Boolean> matchLayers = new LinkedHashMap<>();
        JSONObject layers = root.getJSONObject("matchLayers");
        if (layers != null) {
            layers.forEach((k, v) -> matchLayers.put(k, Boolean.valueOf(v.toString())));
        } else {
            matchLayers.put("exact", true);
            matchLayers.put("rule", true);
            matchLayers.put("ai", true);
            matchLayers.put("split", true);
        }

        return ReconDefinitionNLResult.builder()
                .defName(root.getStr("defName", "AI生成方案"))
                .defCode(root.getStr("defCode", "NL_GEN_" + System.currentTimeMillis()))
                .description(root.getStr("description", description))
                .sourceAName(root.getStr("sourceAName", ""))
                .sourceBNames(sourceBNames)
                .ruleNames(ruleNames)
                .matchLayers(matchLayers)
                .periodType(root.getStr("periodType", "MONTHLY"))
                .defaultPeriod(root.getStr("defaultPeriod", ""))
                .aiExplanation(root.getStr("aiExplanation", ""))
                .build();
    }

    @Override
    public ProcessDefNLResult generateProcessDefFromNL(String description) {
        String model = llmClient.resolveModel("default");
        String userPrompt = """
                根据用户的自然语言描述，设计一个审批流程定义，只返回JSON：

                用户的自然语言描述：
                %s

                返回格式必须严格遵循：
                {
                  "processName": "流程名称（简洁有辨识度，中文，如'差异处理审批流程'）",
                  "processKey": "流程标识（小写字母数字下划线，如discrepancy_approval）",
                  "description": "流程说明（总结用户需求，中文）",
                  "steps": [
                    {"order": 1, "name": "步骤名称（如'部门经理审批'）", "approverRole": "审批角色（如DEPT_MANAGER、FINANCE、DIRECTOR、ADMIN）"},
                    {"order": 2, "name": "步骤名称", "approverRole": "审批角色"}
                  ],
                  "aiExplanation": "中文解释AI的解析思路，说明为什么这样设计流程步骤"
                }
                """.formatted(description);

        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model,
                "你是企业审批流程设计专家。根据用户描述设计合理的多级审批流程。通常企业审批需要1-3级。只输出合法JSON。",
                userPrompt,
                true);
        JSONObject root = parseJsonObject(chat.getContent());

        List<ProcessDefNLResult.ProcessStepItem> steps = new ArrayList<>();
        if (root.getJSONArray("steps") != null) {
            root.getJSONArray("steps").forEach(item -> {
                JSONObject stepObj = (JSONObject) item;
                steps.add(ProcessDefNLResult.ProcessStepItem.builder()
                        .order(stepObj.getInt("order", steps.size() + 1))
                        .name(stepObj.getStr("name", ""))
                        .approverRole(stepObj.getStr("approverRole", ""))
                        .build());
            });
        }

        return ProcessDefNLResult.builder()
                .processName(root.getStr("processName", "AI生成流程"))
                .processKey(root.getStr("processKey", "NL_PROCESS_" + System.currentTimeMillis()))
                .description(root.getStr("description", description))
                .steps(steps)
                .aiExplanation(root.getStr("aiExplanation", ""))
                .build();
    }

    @Override
    public String generateReconReport(ReportGenerateRequest request) {
        String model = llmClient.resolveModel("default");
        String lang = "en_US".equalsIgnoreCase(request.getLanguage()) ? "英文" : "中文";
        String userPrompt = """
                撰写对账%s报告，返回 JSON：{"report":"完整报告正文"}
                类型: %s, 期间: %s
                匹配数: %s, 差异数: %s, 匹配率: %s
                趋势: %s
                """.formatted(lang, request.getReportType(), request.getPeriod(),
                request.getMatchCount(), request.getDiscrepancyCount(),
                request.getMatchRate(), nvl(request.getTrendData()));
        OpenAiCompatibleClient.ChatResult chat = llmClient.chat(
                model, "你是财务报告撰写助手，只输出 JSON。", userPrompt, true);
        return parseJsonObject(chat.getContent()).getStr("report", chat.getContent());
    }

    @Async
    @Override
    public CompletableFuture<MatchScoreResult> semanticMatchScoreAsync(MatchScoreRequest request) {
        return CompletableFuture.completedFuture(semanticMatchScore(request));
    }

    @Async
    @Override
    public CompletableFuture<RootCauseResult> analyzeRootCauseAsync(RootCauseRequest request) {
        return CompletableFuture.completedFuture(analyzeRootCause(request));
    }

    private JSONObject parseJsonObject(String content) {
        String json = extractJson(content);
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法解析 LLM JSON 响应: " + content, ex);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private BigDecimal decimal(JSONObject root, String key) {
        Object val = root.get(key);
        if (val == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return new BigDecimal(val.toString()).setScale(4, RoundingMode.HALF_UP);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private boolean containsCjk(String text) {
        for (char ch : text.toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private Set<String> bigrams(String text) {
        String cleaned = text.replaceAll("\\s+", "");
        Set<String> set = new HashSet<>();
        for (int i = 0; i < cleaned.length() - 1; i++) {
            set.add(cleaned.substring(i, i + 2));
        }
        if (cleaned.length() == 1) {
            set.add(cleaned);
        }
        return set;
    }

    private List<FieldMappingSuggestion> heuristicFieldMappings(List<String> sourceFields) {
        Map<String, String> dict = new LinkedHashMap<>();
        dict.put("amount", "交易金额");
        dict.put("date", "交易日期");
        dict.put("desc", "交易摘要");
        dict.put("party", "对方名称");
        dict.put("ref", "流水号");
        List<FieldMappingSuggestion> list = new ArrayList<>();
        for (String sf : sourceFields) {
            String lower = sf.toLowerCase();
            for (Map.Entry<String, String> e : dict.entrySet()) {
                if (lower.contains(e.getKey())) {
                    list.add(FieldMappingSuggestion.builder()
                            .sourceField(sf)
                            .targetField(e.getValue())
                            .confidence(75)
                            .fieldType("STRING")
                            .explanation("启发式回退映射")
                            .build());
                    break;
                }
            }
        }
        return list;
    }

    private String heuristicClassify(DiscrepancyClassifyRequest request) {
        if (request.getAmountDiff() == null) {
            return "UNKNOWN";
        }
        BigDecimal diff = request.getAmountDiff().abs();
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return "TIME_DIFF";
        }
        if (diff.compareTo(new BigDecimal("50")) <= 0) {
            return "FEE_DIFF";
        }
        if (Boolean.TRUE.equals(request.getCrossBorder())) {
            return "EXCHANGE_DIFF";
        }
        return "UNKNOWN";
    }
}
