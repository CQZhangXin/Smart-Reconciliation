package com.recon.module.engine.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.recon.ai.dto.MatchScoreRequest;
import com.recon.ai.dto.MatchScoreResult;
import com.recon.ai.service.AIService;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.datasource.entity.RawRecord;
import com.recon.module.datasource.repository.RawRecordMapper;
import com.recon.module.engine.entity.ReconMatch;
import com.recon.module.engine.entity.ReconTask;
import com.recon.module.engine.repository.ReconMatchMapper;
import com.recon.module.engine.repository.ReconTaskMapper;
import com.recon.module.rule.entity.ReconRuleConfig;
import com.recon.module.rule.repository.ReconRuleConfigMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心匹配引擎服务 — 实现四层匹配架构
 *
 * <pre>
 * Layer 1: 精准匹配 (amount + ref + date 完全相等)
 * Layer 2: 规则匹配 (容忍度范围)
 * Layer 3: AI语义匹配 (LLM + Embedding)
 * Layer 4: 1:N 拆单匹配 (子集求和)
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingEngineService {

    private final RawRecordMapper rawRecordMapper;
    private final ReconMatchMapper reconMatchMapper;
    private final ReconTaskMapper reconTaskMapper;
    private final ReconRuleConfigMapper ruleConfigMapper;
    private final AIService aiService;

    @Value("${recon.engine.batch-size:1000}")
    private int batchSize;

    @Value("${recon.engine.date-window-days:7}")
    private int dateWindowDays;

    @Value("${recon.engine.amount-tolerance-pct:0.01}")
    private BigDecimal amountTolerancePct;

    @Value("${recon.engine.max-split-combinations:100}")
    private int maxSplitCombinations;

    /** AI语义匹配候选数量上限 */
    private static final int AI_CANDIDATE_LIMIT = 100;

    /** AI语义匹配精排数量上限 */
    private static final int AI_FINE_RANK_LIMIT = 3;

    /** AI自动确认阈值 */
    private static final BigDecimal AI_AUTO_CONFIRM_THRESHOLD = new BigDecimal("0.85");

    /** AI推荐匹配阈值 */
    private static final BigDecimal AI_RECOMMEND_THRESHOLD = new BigDecimal("0.70");

    // ========================================================================
    // 1. 主入口
    // ========================================================================

    /**
     * 执行对账匹配 — 完整流水线
     *
     * @param task 对账任务
     * @return 匹配结果,包含统计信息和未匹配的记录
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconResult executeReconciliation(ReconTask task) {
        Long taskId = task.getId();
        Long orgId = task.getOrgId();
        log.info("=== 开始对账任务 [{}] taskId={}, sourceA={}, sourceB={} ===",
                task.getTaskName(), taskId, task.getSourceAId(), task.getSourceBId());

        long startTime = System.currentTimeMillis();

        try {
            // 更新任务状态为运行中
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            reconTaskMapper.updateById(task);

            // 查询待匹配的原始记录
            List<RawRecord> recordsA = rawRecordMapper.selectPendingRecords(task.getSourceAId(), batchSize);
            List<RawRecord> recordsB = rawRecordMapper.selectPendingRecords(task.getSourceBId(), batchSize);

            task.setTotalACount(recordsA.size());
            task.setTotalBCount(recordsB.size());
            log.info("加载待匹配记录: A方={}条, B方={}条", recordsA.size(), recordsB.size());

            if (CollUtil.isEmpty(recordsA) || CollUtil.isEmpty(recordsB)) {
                log.warn("一方或双方无待匹配记录, 任务结束");
                task.setMatchedCount(0);
                task.setUnmatchedCount(recordsA.size() + recordsB.size());
                return ReconResult.builder()
                        .unmatchedA(recordsA)
                        .unmatchedB(recordsB)
                        .build();
            }

            int totalMatched = 0;
            int exactCount = 0;
            int ruleCount = 0;
            int aiCount = 0;
            int splitCount = 0;
            int layer = 0;

            // ========== Layer 1: 精准匹配 ==========
            layer++;
            exactCount = exactMatch(recordsA, recordsB, taskId);
            totalMatched += exactCount;
            log.info("[Layer {}] 精准匹配完成: {} 对, A剩余={}, B剩余={}",
                    layer, exactCount, recordsA.size(), recordsB.size());

            // ========== Layer 2: 规则匹配 ==========
            if (CollUtil.isNotEmpty(recordsA) && CollUtil.isNotEmpty(recordsB)) {
                layer++;
                ruleCount = ruleMatch(recordsA, recordsB, taskId, orgId, task);
                totalMatched += ruleCount;
                log.info("[Layer {}] 规则匹配完成: {} 对, A剩余={}, B剩余={}",
                        layer, ruleCount, recordsA.size(), recordsB.size());
            }

            // ========== Layer 3: AI语义匹配 ==========
            if (CollUtil.isNotEmpty(recordsA) && CollUtil.isNotEmpty(recordsB)) {
                layer++;
                aiCount = aiSemanticMatch(recordsA, recordsB, taskId);
                totalMatched += aiCount;
                log.info("[Layer {}] AI语义匹配完成: {} 对, A剩余={}, B剩余={}",
                        layer, aiCount, recordsA.size(), recordsB.size());
            }

            // ========== 1:N 拆单匹配 ==========
            if (CollUtil.isNotEmpty(recordsA) && CollUtil.isNotEmpty(recordsB)) {
                layer++;
                splitCount = splitMatch(recordsA, recordsB, taskId);
                totalMatched += splitCount;
                log.info("[Layer {}] 拆单匹配完成: {} 对, A剩余={}, B剩余={}",
                        layer, splitCount, recordsA.size(), recordsB.size());
            }

            int unmatchedCount = recordsA.size() + recordsB.size();

            // 设置任务统计信息（不保存，由调用方统一完成）
            task.setMatchedCount(totalMatched);
            task.setUnmatchedCount(unmatchedCount);
            log.info("匹配引擎统计: totalMatched={}, unmatchedCount={}", totalMatched, unmatchedCount);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== 对账任务完成 [{}] 总匹配={}, 未匹配={}, 耗时={}ms ===",
                    task.getTaskName(), totalMatched, unmatchedCount, elapsed);

            return ReconResult.builder()
                    .unmatchedA(new ArrayList<>(recordsA))
                    .unmatchedB(new ArrayList<>(recordsB))
                    .exactMatched(exactCount)
                    .ruleMatched(ruleCount)
                    .aiMatched(aiCount)
                    .splitMatched(splitCount)
                    .build();

        } catch (Exception e) {
            log.error("对账任务执行失败 taskId={}", taskId, e);
            // 不在此处更新任务状态，由调用方统一处理
            throw new BusinessException(ResultCode.RECON_TASK_FAILED, "对账任务执行失败，请稍后重试");
        }
    }

    // ========================================================================
    // 2. Layer 1: 精准匹配
    // ========================================================================

    /**
     * Layer 1 - 精准匹配: 金额、流水号、日期完全相等
     *
     * @param recordsA A方记录列表 (会被修改: 移除已匹配项)
     * @param recordsB B方记录列表 (会被修改: 移除已匹配项)
     * @param taskId   任务ID
     * @return 匹配到的对数
     */
    public int exactMatch(List<RawRecord> recordsA, List<RawRecord> recordsB, Long taskId) {
        if (CollUtil.isEmpty(recordsA) || CollUtil.isEmpty(recordsB)) {
            return 0;
        }

        // 为B方记录构建复合键索引: (amount, transactionRef, transactionDate)
        // 使用 List 存储同键的多条记录
        Map<String, List<RawRecord>> bIndex = new HashMap<>();
        for (RawRecord b : recordsB) {
            String key = buildExactKey(b);
            bIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
        }

        List<ReconMatch> matches = new ArrayList<>();
        Set<Long> matchedBIds = new HashSet<>();

        Iterator<RawRecord> aIter = recordsA.iterator();
        while (aIter.hasNext()) {
            RawRecord a = aIter.next();
            String key = buildExactKey(a);

            List<RawRecord> candidates = bIndex.get(key);
            if (CollUtil.isEmpty(candidates)) {
                continue;
            }

            // 取第一个未被匹配的候选
            RawRecord matchedB = null;
            for (RawRecord b : candidates) {
                if (!matchedBIds.contains(b.getId())) {
                    matchedB = b;
                    break;
                }
            }

            if (matchedB == null) {
                continue;
            }

            // 构建匹配记录
            ReconMatch match = buildMatchRecord(taskId, a.getOrgId(), a, matchedB,
                    "EXACT", new BigDecimal("100.00"), "AUTO_CONFIRMED",
                    "精准匹配: 金额=" + a.getAmount() + ", 流水号=" + a.getTransactionRef() + ", 日期=" + a.getTransactionDate());

            matches.add(match);
            matchedBIds.add(matchedB.getId());
            aIter.remove();
        }

        // 从B列表中移除已匹配的记录
        recordsB.removeIf(b -> matchedBIds.contains(b.getId()));

        // 批量保存匹配
        // 注意: Db.saveBatch() 会创建独立的SQL会话，绕过Spring @Transactional事务管理。
        // 在需要事务一致性的场景下，建议改用 baseMapper.insert() 逐条插入
        // 或让Service继承IService使用其saveBatch()方法
        if (CollUtil.isNotEmpty(matches)) {
            Db.saveBatch(matches);
        }

        return matches.size();
    }

    // ========================================================================
    // 3. Layer 2: 规则匹配
    // ========================================================================

    /**
     * Layer 2 - 规则匹配: 基于配置的容忍度规则进行匹配
     *
     * @param recordsA A方记录列表 (会被修改)
     * @param recordsB B方记录列表 (会被修改)
     * @param taskId   任务ID
     * @param orgId    组织ID
     * @param task     对账任务（用于按 ruleIds 过滤规则，可为 null）
     * @return 匹配到的对数
     */
    public int ruleMatch(List<RawRecord> recordsA, List<RawRecord> recordsB, Long taskId,
                         Long orgId, ReconTask task) {
        if (CollUtil.isEmpty(recordsA) || CollUtil.isEmpty(recordsB)) {
            return 0;
        }

        // 加载活动规则(按优先级排序)，若任务指定了 ruleIds 则仅使用这些规则
        List<ReconRuleConfig> rules = ruleConfigMapper.selectActiveRules(orgId);
        Set<Long> selectedRuleIds = parseRuleIdSet(task != null ? task.getRuleIds() : null);
        if (CollUtil.isNotEmpty(selectedRuleIds)) {
            rules = rules.stream()
                    .filter(r -> selectedRuleIds.contains(r.getId()))
                    .collect(Collectors.toList());
            log.info("任务指定规则过滤后剩余 {} 条: {}", rules.size(), selectedRuleIds);
        }
        if (CollUtil.isEmpty(rules)) {
            log.info("无活动规则配置, 跳过规则匹配层");
            return 0;
        }

        List<ReconMatch> matches = new ArrayList<>();
        Set<Long> matchedBIds = new HashSet<>();

        for (ReconRuleConfig rule : rules) {
            // 只处理精确匹配和规则匹配类型的规则
            String ruleType = rule.getRuleType();
            if (!"EXACT_MATCH".equals(ruleType) && !"RULE_MATCH".equals(ruleType)) {
                continue;
            }

            // 解析规则条件
            List<Map<String, Object>> conditions = parseRuleConfig(rule.getMatchConfig());
            if (CollUtil.isEmpty(conditions)) {
                log.warn("规则 [{}] matchConfig 为空, 跳过", rule.getRuleName());
                continue;
            }

            // 解析容忍度配置
            Map<String, Object> tolerance = parseTolerance(rule.getTolerance());

            // Pre-filter B candidates that are not yet matched
            List<RawRecord> availableB = recordsB.stream()
                    .filter(b -> !matchedBIds.contains(b.getId()))
                    .collect(Collectors.toList());

            Iterator<RawRecord> aIter = recordsA.iterator();
            while (aIter.hasNext()) {
                RawRecord a = aIter.next();

                RawRecord bestB = null;
                double bestScore = 0;

                for (RawRecord b : availableB) {

                    // 评估所有条件
                    boolean allPassed = true;
                    double totalWeight = 0;
                    double weightedScore = 0;

                    for (Map<String, Object> condition : conditions) {
                        double weight = getConditionWeight(condition);
                        totalWeight += weight;
                        boolean passed = evaluateRuleCondition(a, b, condition, tolerance);
                        if (!passed) {
                            allPassed = false;
                            break;
                        }
                        weightedScore += weight;
                    }

                    if (allPassed && totalWeight > 0) {
                        double score = weightedScore / totalWeight;
                        if (score > bestScore) {
                            bestScore = score;
                            bestB = b;
                        }
                    }
                }

                if (bestB != null) {
                    // 根据条件匹配度计算置信度: 90-98 范围
                    BigDecimal confidence = BigDecimal.valueOf(90.00 + bestScore * 8.00)
                            .setScale(2, RoundingMode.HALF_UP);

                    ReconMatch match = buildMatchRecord(taskId, rule.getOrgId(), a, bestB,
                            "RULE", confidence, "AUTO_CONFIRMED",
                            "规则匹配: " + rule.getRuleName() + " (置信度=" + confidence + ")");

                    matches.add(match);
                    matchedBIds.add(bestB.getId());
                    availableB.remove(bestB);
                    aIter.remove();
                }
            }
        }

        // 从B列表中移除已匹配的记录
        recordsB.removeIf(b -> matchedBIds.contains(b.getId()));

        // 批量保存
        if (CollUtil.isNotEmpty(matches)) {
            Db.saveBatch(matches);
        }

        return matches.size();
    }

    /**
     * 兼容旧签名：不按任务规则过滤
     *
     * @param recordsA A方记录
     * @param recordsB B方记录
     * @param taskId   任务ID
     * @param orgId    组织ID
     * @return 匹配对数
     */
    public int ruleMatch(List<RawRecord> recordsA, List<RawRecord> recordsB, Long taskId, Long orgId) {
        return ruleMatch(recordsA, recordsB, taskId, orgId, null);
    }

    // ========================================================================
    // 4. Layer 3: AI语义匹配
    // ========================================================================

    /**
     * Layer 3 - AI语义匹配: 使用LLM和Embedding进行语义相似度匹配
     *
     * @param recordsA A方记录列表 (会被修改)
     * @param recordsB B方记录列表 (会被修改)
     * @param taskId   任务ID
     * @return 匹配到的对数
     */
    public int aiSemanticMatch(List<RawRecord> recordsA, List<RawRecord> recordsB, Long taskId) {
        if (CollUtil.isEmpty(recordsA) || CollUtil.isEmpty(recordsB)) {
            return 0;
        }

        List<ReconMatch> matches = new ArrayList<>();
        Set<Long> matchedBIds = new HashSet<>();

        Iterator<RawRecord> aIter = recordsA.iterator();
        while (aIter.hasNext()) {
            RawRecord a = aIter.next();

            // === 候选过滤: 日期窗口 + 金额容忍度 ===
            List<RawRecord> candidates = recordsB.stream()
                    .filter(b -> !matchedBIds.contains(b.getId()))
                    .filter(b -> isWithinDateWindow(a.getTransactionDate(), b.getTransactionDate(), dateWindowDays))
                    .filter(b -> isWithinAmountTolerance(a.getAmount(), b.getAmount(), amountTolerancePct))
                    .limit(AI_CANDIDATE_LIMIT)
                    .collect(Collectors.toList());

            if (CollUtil.isEmpty(candidates)) {
                continue;
            }

            // === 精排: 对前N个候选调用AI服务 ===
            List<AIScoreResult> scoredCandidates = new ArrayList<>();

            for (int i = 0; i < Math.min(candidates.size(), AI_FINE_RANK_LIMIT); i++) {
                RawRecord b = candidates.get(i);

                try {
                    MatchScoreRequest request = buildMatchScoreRequest(a, b);
                    MatchScoreResult result = aiService.semanticMatchScore(request);

                    if (result != null && result.getOverallScore() != null) {
                        scoredCandidates.add(new AIScoreResult(b, result));
                    }
                } catch (Exception e) {
                    log.warn("AI语义匹配调用失败 recordA={}, recordB={}: {}", a.getId(), b.getId(), e.getMessage());
                }
            }

            if (CollUtil.isEmpty(scoredCandidates)) {
                continue;
            }

            // 按综合分数降序排列
            scoredCandidates.sort((x, y) -> y.result.getOverallScore().compareTo(x.result.getOverallScore()));

            AIScoreResult best = scoredCandidates.get(0);
            BigDecimal overallScore = best.result.getOverallScore();

            String matchStatus;
            if (overallScore.compareTo(AI_AUTO_CONFIRM_THRESHOLD) >= 0) {
                matchStatus = "AUTO_CONFIRMED";
            } else if (overallScore.compareTo(AI_RECOMMEND_THRESHOLD) >= 0) {
                matchStatus = "PENDING_REVIEW";
            } else {
                continue; // 分数过低,不匹配
            }

            BigDecimal confidence = overallScore.multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);

            ReconMatch match = buildMatchRecord(taskId, a.getOrgId(), a, best.b,
                    "AI_SEMANTIC", confidence, matchStatus,
                    best.result.getExplanation() != null ? best.result.getExplanation() : "AI语义匹配");

            matches.add(match);
            matchedBIds.add(best.b.getId());
            aIter.remove();
        }

        // 从B列表中移除已匹配的记录
        recordsB.removeIf(b -> matchedBIds.contains(b.getId()));

        // 批量保存
        if (CollUtil.isNotEmpty(matches)) {
            Db.saveBatch(matches);
        }

        return matches.size();
    }

    // ========================================================================
    // 5. 1:N 拆单匹配
    // ========================================================================

    /**
     * 1:N 拆单匹配: 使用子集求和算法,查找一组B记录其金额之和等于A单笔金额
     *
     * @param recordsA A方记录列表 (会被修改)
     * @param recordsB B方记录列表 (会被修改)
     * @param taskId   任务ID
     * @return 匹配到的对数 (B方被匹配的记录数)
     */
    public int splitMatch(List<RawRecord> recordsA, List<RawRecord> recordsB, Long taskId) {
        if (CollUtil.isEmpty(recordsA) || CollUtil.isEmpty(recordsB)) {
            return 0;
        }

        List<ReconMatch> matches = new ArrayList<>();
        Set<Long> matchedAIds = new HashSet<>();
        Set<Long> matchedBIds = new HashSet<>();

        // 为B方记录按金额建立频繁查找
        List<RawRecord> sortedB = new ArrayList<>(recordsB);
        sortedB.sort(Comparator.comparing(RawRecord::getAmount));

        for (RawRecord a : recordsA) {
            if (matchedAIds.contains(a.getId())) {
                continue;
            }

            // 过滤出未被匹配的B记录
            List<RawRecord> availableB = sortedB.stream()
                    .filter(b -> !matchedBIds.contains(b.getId()))
                    .collect(Collectors.toList());

            if (availableB.isEmpty()) {
                continue;
            }

            // 子集求和
            SubsetSumResult ssResult = findSubsetSum(
                    a.getAmount(),
                    availableB,
                    amountTolerancePct,
                    maxSplitCombinations
            );

            if (ssResult != null && CollUtil.isNotEmpty(ssResult.matchedRecords)) {
                log.info("拆单匹配: A[{}] 金额={} 匹配 {} 条B记录",
                        a.getId(), a.getAmount(), ssResult.matchedRecords.size());

                for (RawRecord b : ssResult.matchedRecords) {
                    ReconMatch match = buildMatchRecord(taskId, a.getOrgId(), a, b,
                            "AI_SPLIT", new BigDecimal("85.00"), "AUTO_CONFIRMED",
                            "拆单匹配: A方金额=" + a.getAmount()
                                    + " = 子集求和=" + ssResult.sum
                                    + " (共" + ssResult.matchedRecords.size() + "笔)");
                    matches.add(match);
                    matchedBIds.add(b.getId());
                }
                matchedAIds.add(a.getId());
            }
        }

        // 从列表中移除已匹配的记录
        recordsA.removeIf(a -> matchedAIds.contains(a.getId()));
        recordsB.removeIf(b -> matchedBIds.contains(b.getId()));

        // 批量保存
        if (CollUtil.isNotEmpty(matches)) {
            Db.saveBatch(matches);
        }

        return matchedBIds.size();
    }

    // ========================================================================
    // 6. 构建匹配记录
    // ========================================================================

    /**
     * 构建 ReconMatch 实体
     */
    public ReconMatch buildMatchRecord(Long taskId, Long orgId,
                                        RawRecord recordA, RawRecord recordB,
                                        String matchType, BigDecimal confidence,
                                        String status, String explanation) {
        ReconMatch match = new ReconMatch();
        match.setTaskId(taskId);
        match.setOrgId(orgId);
        match.setRecordAId(recordA.getId());
        match.setRecordBId(recordB.getId());
        match.setMatchType(matchType);
        match.setConfidence(confidence);

        // 金额信息
        match.setAmountA(recordA.getAmount());
        match.setAmountB(recordB.getAmount());
        if (recordA.getAmount() != null && recordB.getAmount() != null) {
            match.setAmountDiff(recordA.getAmount().subtract(recordB.getAmount()).abs());
        }

        // 日期差异天数
        if (recordA.getTransactionDate() != null && recordB.getTransactionDate() != null) {
            match.setDateDiffDays((int) ChronoUnit.DAYS.between(
                    recordA.getTransactionDate(), recordB.getTransactionDate()));
        }

        // 匹配维度 (JSON)
        Map<String, Object> dimensions = new LinkedHashMap<>();
        dimensions.put("amount", recordA.getAmount() != null && recordB.getAmount() != null
                && recordA.getAmount().compareTo(recordB.getAmount()) == 0);
        dimensions.put("transactionDate", recordA.getTransactionDate() != null
                && recordA.getTransactionDate().equals(recordB.getTransactionDate()));
        dimensions.put("transactionRef", StrUtil.isNotEmpty(recordA.getTransactionRef())
                && StrUtil.isNotEmpty(recordB.getTransactionRef())
                && recordA.getTransactionRef().equals(recordB.getTransactionRef()));
        match.setMatchDimensions(JSONUtil.toJsonStr(dimensions));

        match.setAiExplanation(explanation);
        match.setStatus(status);
        match.setCreatedAt(LocalDateTime.now());
        match.setUpdatedAt(LocalDateTime.now());

        return match;
    }

    // ========================================================================
    // 7. 解析规则配置
    // ========================================================================

    /**
     * 解析规则 matchConfig JSON 为条件列表
     *
     * matchConfig 格式:
     * <pre>
     * [
     *   {"field": "amount", "operator": "tolerance", "weight": 1.0},
     *   {"field": "transaction_date", "operator": "within_days", "days": 3, "weight": 1.0},
     *   {"field": "description", "operator": "fuzzy", "threshold": 0.6, "weight": 0.5},
     *   {"field": "transaction_ref", "operator": "exact_or_contains", "weight": 1.0}
     * ]
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseRuleConfig(String matchConfigJson) {
        if (StrUtil.isBlank(matchConfigJson)) {
            return Collections.emptyList();
        }

        try {
            String trimmed = matchConfigJson.trim();
            // 兼容 {"conditions":[...]} 与 顶层数组 [...]
            if (trimmed.startsWith("{")) {
                JSONObject root = JSONUtil.parseObj(trimmed);
                Object conditionsObj = root.get("conditions");
                if (conditionsObj == null) {
                    log.warn("matchConfig 对象缺少 conditions 字段: {}", matchConfigJson);
                    return Collections.emptyList();
                }
                trimmed = conditionsObj instanceof String
                        ? (String) conditionsObj
                        : JSONUtil.toJsonStr(conditionsObj);
            }
            JSONArray jsonArray = JSONUtil.parseArray(trimmed);
            List<Map<String, Object>> conditions = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Map<String, Object> condition = new LinkedHashMap<>();
                for (String key : obj.keySet()) {
                    condition.put(key, obj.get(key));
                }
                conditions.add(condition);
            }
            return conditions;
        } catch (Exception e) {
            log.error("解析规则 matchConfig 失败: {}", matchConfigJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析任务绑定的规则ID集合
     *
     * @param ruleIdsJson 规则ID JSON（数组或逗号分隔）
     * @return 规则ID集合
     */
    private Set<Long> parseRuleIdSet(String ruleIdsJson) {
        if (StrUtil.isBlank(ruleIdsJson)) {
            return Collections.emptySet();
        }
        try {
            String trimmed = ruleIdsJson.trim();
            Set<Long> ids = new HashSet<>();
            if (trimmed.startsWith("[")) {
                JSONArray arr = JSONUtil.parseArray(trimmed);
                for (int i = 0; i < arr.size(); i++) {
                    Object val = arr.get(i);
                    if (val instanceof Number) {
                        ids.add(((Number) val).longValue());
                    } else if (val != null) {
                        ids.add(Long.parseLong(val.toString()));
                    }
                }
            } else {
                for (String part : trimmed.split(",")) {
                    if (StrUtil.isNotBlank(part)) {
                        ids.add(Long.parseLong(part.trim()));
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("解析任务 ruleIds 失败: {}", ruleIdsJson, e);
            return Collections.emptySet();
        }
    }

    /**
     * 解析容忍度配置 JSON
     *
     * tolerance 格式:
     * <pre>
     * {
     *   "amount": {"abs": 0.01, "pct": 0.01},
     *   "dateWindow": 3,
     *   "description": {"threshold": 0.6}
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTolerance(String toleranceJson) {
        if (StrUtil.isBlank(toleranceJson)) {
            return Collections.emptyMap();
        }

        try {
            JSONObject json = JSONUtil.parseObj(toleranceJson);
            Map<String, Object> result = new LinkedHashMap<>();
            for (String key : json.keySet()) {
                result.put(key, json.get(key));
            }
            return result;
        } catch (Exception e) {
            log.error("解析容忍度配置失败: {}", toleranceJson, e);
            return Collections.emptyMap();
        }
    }

    // ========================================================================
    // 8. 规则条件评估
    // ========================================================================

    /**
     * 评估单条规则条件
     *
     * @param recordA   A方记录
     * @param recordB   B方记录
     * @param condition 条件定义
     * @param tolerance 容忍度配置 (可为空)
     * @return 条件是否通过
     */
    public boolean evaluateRuleCondition(RawRecord recordA, RawRecord recordB,
                                          Map<String, Object> condition,
                                          Map<String, Object> tolerance) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");

        if (StrUtil.isBlank(field)) {
            return true; // 空字段条件默认通过
        }

        switch (field) {
            case "amount":
                return evaluateAmountCondition(recordA, recordB, condition, tolerance);

            case "transaction_date":
                return evaluateDateCondition(recordA, recordB, condition, tolerance);

            case "description":
                return evaluateDescriptionCondition(recordA, recordB, condition, tolerance);

            case "transaction_ref":
                return evaluateRefCondition(recordA, recordB, operator);

            case "currency":
                return evaluateStringField(recordA.getCurrency(), recordB.getCurrency(), operator);

            case "counter_party":
                return evaluateStringField(recordA.getCounterParty(), recordB.getCounterParty(), operator);

            case "counter_acct":
                return evaluateStringField(recordA.getCounterAcct(), recordB.getCounterAcct(), operator);

            case "direction":
                return evaluateStringField(recordA.getDirection(), recordB.getDirection(), operator);

            default:
                log.warn("未知规则字段: {}, 跳过校验", field);
                return false;
        }
    }

    // ========================================================================
    // 条件评估子方法
    // ========================================================================

    /**
     * 评估金额条件
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateAmountCondition(RawRecord a, RawRecord b,
                                             Map<String, Object> condition,
                                             Map<String, Object> tolerance) {
        BigDecimal amountA = a.getAmount();
        BigDecimal amountB = b.getAmount();

        if (amountA == null || amountB == null) {
            return false;
        }

        // 先从条件中获取容忍度
        BigDecimal absTolerance = getBigDecimalParam(condition, "absTolerance");
        BigDecimal pctTolerance = getBigDecimalParam(condition, "pctTolerance");

        // 如果条件中没有,从全局容忍度中获取
        if (absTolerance == null && tolerance.containsKey("amount")) {
            Object amtTol = tolerance.get("amount");
            if (amtTol instanceof Map) {
                Map<String, Object> amtMap = (Map<String, Object>) amtTol;
                absTolerance = getBigDecimalFromObject(amtMap.get("abs"));
                pctTolerance = getBigDecimalFromObject(amtMap.get("pct"));
            }
        }

        // 默认容忍度
        if (absTolerance == null) {
            absTolerance = BigDecimal.ZERO;
        }
        if (pctTolerance == null) {
            pctTolerance = amountTolerancePct;
        }

        BigDecimal diff = amountA.subtract(amountB).abs();
        BigDecimal pctThreshold = amountA.abs().multiply(pctTolerance);
        BigDecimal threshold = absTolerance.add(pctThreshold);

        return diff.compareTo(threshold) <= 0;
    }

    /**
     * 评估日期条件
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateDateCondition(RawRecord a, RawRecord b,
                                           Map<String, Object> condition,
                                           Map<String, Object> tolerance) {
        LocalDate dateA = a.getTransactionDate();
        LocalDate dateB = b.getTransactionDate();

        if (dateA == null || dateB == null) {
            // 如果一方有日期另一方没有,尝试使用其他日期字段
            dateA = dateA != null ? dateA : a.getBookingDate();
            dateB = dateB != null ? dateB : b.getBookingDate();
        }

        if (dateA == null || dateB == null) {
            return false;
        }

        int allowedDays = dateWindowDays;

        // 从条件中获取日期间隔
        Object daysObj = condition.get("days");
        if (daysObj instanceof Number) {
            allowedDays = ((Number) daysObj).intValue();
        }

        // 从容忍度中获取
        if (tolerance.containsKey("dateWindow")) {
            Object dwObj = tolerance.get("dateWindow");
            if (dwObj instanceof Number) {
                allowedDays = ((Number) dwObj).intValue();
            }
        }

        long daysDiff = Math.abs(ChronoUnit.DAYS.between(dateA, dateB));
        return daysDiff <= allowedDays;
    }

    /**
     * 评估描述模糊匹配条件
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateDescriptionCondition(RawRecord a, RawRecord b,
                                                  Map<String, Object> condition,
                                                  Map<String, Object> tolerance) {
        String descA = a.getDescription();
        String descB = b.getDescription();

        if (StrUtil.isBlank(descA) || StrUtil.isBlank(descB)) {
            // 两边都为空则视为匹配
            return StrUtil.isBlank(descA) == StrUtil.isBlank(descB);
        }

        double threshold = 0.5;

        // 从条件中获取阈值
        Object thObj = condition.get("threshold");
        if (thObj instanceof Number) {
            threshold = ((Number) thObj).doubleValue();
        }

        // 从容忍度中获取
        if (tolerance.containsKey("description")) {
            Object descTol = tolerance.get("description");
            if (descTol instanceof Map) {
                Map<String, Object> descMap = (Map<String, Object>) descTol;
                Object t = descMap.get("threshold");
                if (t instanceof Number) {
                    threshold = ((Number) t).doubleValue();
                }
            }
        }

        // 多种模糊匹配策略
        // 1. 包含匹配
        if (descA.contains(descB) || descB.contains(descA)) {
            return true;
        }

        // 2. 去空白后包含匹配
        String normalizedA = descA.replaceAll("\\s+", "").toLowerCase();
        String normalizedB = descB.replaceAll("\\s+", "").toLowerCase();
        if (normalizedA.contains(normalizedB) || normalizedB.contains(normalizedA)) {
            return true;
        }

        // 3. Levenshtein相似度 (使用Hutool)
        double similarity = StrUtil.similar(descA.toLowerCase(), descB.toLowerCase());
        return similarity >= threshold;
    }

    /**
     * 评估流水号匹配条件
     */
    private boolean evaluateRefCondition(RawRecord a, RawRecord b, String operator) {
        String refA = a.getTransactionRef();
        String refB = b.getTransactionRef();

        if (StrUtil.isBlank(refA) || StrUtil.isBlank(refB)) {
            // 如果一方为空,不强制匹配流水号 (但也不要直接拒绝,让其他条件决定)
            return true;
        }

        if ("contains".equals(operator)) {
            return refA.contains(refB) || refB.contains(refA);
        }

        // 默认: 精确匹配
        return refA.equals(refB);
    }

    /**
     * 通用字符串字段评估
     */
    private boolean evaluateStringField(String valueA, String valueB, String operator) {
        if (StrUtil.isBlank(valueA) || StrUtil.isBlank(valueB)) {
            return true; // 空值不强制匹配
        }

        if ("contains".equals(operator)) {
            return valueA.contains(valueB) || valueB.contains(valueA);
        }

        if ("equals_ignore_case".equals(operator)) {
            return valueA.equalsIgnoreCase(valueB);
        }

        // 默认精确匹配
        return valueA.equals(valueB);
    }

    // ========================================================================
    // 子集求和算法
    // ========================================================================

    /**
     * 查找子集求和结果
     *
     * @param target     目标金额
     * @param candidates 候选记录列表
     * @param tolerance  容忍度百分比
     * @param maxCombos  最大组合数
     * @return 匹配的子集结果,未找到返回 null
     */
    private SubsetSumResult findSubsetSum(BigDecimal target,
                                           List<RawRecord> candidates,
                                           BigDecimal tolerance,
                                           int maxCombos) {
        if (target == null || CollUtil.isEmpty(candidates)) {
            return null;
        }

        BigDecimal threshold = target.abs().multiply(tolerance).max(new BigDecimal("0.01"));
        BigDecimal upperBound = target.add(threshold);
        BigDecimal lowerBound = target.subtract(threshold);

        // 过滤掉大于目标+容忍度的单个记录,但保留它们因为可能组合后正好
        // 对候选排序以优化
        List<RawRecord> sorted = candidates.stream()
                .filter(r -> r.getAmount() != null)
                .sorted(Comparator.comparing(RawRecord::getAmount))
                .collect(Collectors.toList());

        // 使用回溯查找
        List<RawRecord> bestSubset = new ArrayList<>();
        int[] combinationCount = {0};

        findSubsetSumBacktrack(sorted, 0, BigDecimal.ZERO, lowerBound, upperBound,
                new ArrayList<>(), bestSubset, combinationCount, maxCombos, threshold);

        if (bestSubset.isEmpty()) {
            return null;
        }

        BigDecimal sum = bestSubset.stream()
                .map(RawRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SubsetSumResult(bestSubset, sum);
    }

    private boolean findSubsetSumBacktrack(List<RawRecord> candidates,
                                            int start,
                                            BigDecimal currentSum,
                                            BigDecimal lowerBound,
                                            BigDecimal upperBound,
                                            List<RawRecord> current,
                                            List<RawRecord> bestSubset,
                                            int[] count,
                                            int maxCombos,
                                            BigDecimal threshold) {
        // 检查当前和是否在容忍范围内
        if (currentSum.compareTo(lowerBound) >= 0 && currentSum.compareTo(upperBound) <= 0
                && !current.isEmpty()) {
            // 找到了有效解
            if (bestSubset.isEmpty() || current.size() < bestSubset.size()) {
                // 更优解 (更少的记录数)
                bestSubset.clear();
                bestSubset.addAll(current);
                return true;
            }
        }

        // 超出上限,剪枝
        if (currentSum.compareTo(upperBound) > 0) {
            return false;
        }

        // 达到最大组合数限制
        if (count[0] >= maxCombos) {
            return false;
        }

        // 已达最大候选深度 (防止1:N中N过大)
        if (current.size() >= 20) {
            return false;
        }

        for (int i = start; i < candidates.size(); i++) {
            RawRecord candidate = candidates.get(i);
            BigDecimal nextSum = currentSum.add(candidate.getAmount());

            count[0]++;

            current.add(candidate);
            boolean found = findSubsetSumBacktrack(candidates, i + 1, nextSum,
                    lowerBound, upperBound, current, bestSubset, count, maxCombos, threshold);
            current.remove(current.size() - 1);

            if (found) {
                return true; // 找到一个解就返回 (贪婪: 优先最少记录数的解)
            }

            // 如果当前和已经超过上限,后面的候选金额更大,直接剪枝
            if (nextSum.compareTo(upperBound) > 0) {
                break;
            }
        }

        return !bestSubset.isEmpty();
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    /**
     * 构建精准匹配的复合键
     */
    private String buildExactKey(RawRecord record) {
        String amount = record.getAmount() != null ? record.getAmount().stripTrailingZeros().toPlainString() : "";
        String ref = StrUtil.emptyToDefault(record.getTransactionRef(), "").trim();
        String date = record.getTransactionDate() != null ? record.getTransactionDate().toString() : "";
        return amount + "|" + ref + "|" + date;
    }

    /**
     * 检查日期是否在窗口内
     */
    private boolean isWithinDateWindow(LocalDate dateA, LocalDate dateB, int windowDays) {
        if (dateA == null || dateB == null) {
            return true; // 无日期不限制
        }
        long diff = Math.abs(ChronoUnit.DAYS.between(dateA, dateB));
        return diff <= windowDays;
    }

    /**
     * 检查金额是否在容忍度内
     */
    private boolean isWithinAmountTolerance(BigDecimal amountA, BigDecimal amountB,
                                             BigDecimal tolerancePct) {
        if (amountA == null || amountB == null) {
            return true; // 无金额不限制
        }
        if (amountA.compareTo(BigDecimal.ZERO) == 0) {
            return amountB.compareTo(BigDecimal.ZERO) == 0;
        }
        BigDecimal diff = amountA.subtract(amountB).abs();
        BigDecimal threshold = amountA.abs().multiply(tolerancePct);
        return diff.compareTo(threshold) <= 0;
    }

    /**
     * 构建AI匹配请求
     */
    private MatchScoreRequest buildMatchScoreRequest(RawRecord a, RawRecord b) {
        return MatchScoreRequest.builder()
                .recordAText(buildRecordText(a))
                .recordBText(buildRecordText(b))
                .amountA(a.getAmount())
                .amountB(b.getAmount())
                .dateA(a.getTransactionDate())
                .dateB(b.getTransactionDate())
                .partyA(a.getCounterParty())
                .partyB(b.getCounterParty())
                .descriptionA(a.getDescription())
                .descriptionB(b.getDescription())
                .refA(a.getTransactionRef())
                .refB(b.getTransactionRef())
                .currency(a.getCurrency())
                .accountType(a.getCounterAcct())
                .build();
    }

    /**
     * 构建记录的标准文本表示 (供AI使用)
     */
    private String buildRecordText(RawRecord record) {
        StringBuilder sb = new StringBuilder();
        if (record.getAmount() != null) {
            sb.append("金额:").append(record.getAmount().stripTrailingZeros().toPlainString()).append(" ");
        }
        if (StrUtil.isNotEmpty(record.getCurrency())) {
            sb.append("币种:").append(record.getCurrency()).append(" ");
        }
        if (record.getTransactionDate() != null) {
            sb.append("交易日期:").append(record.getTransactionDate()).append(" ");
        }
        if (StrUtil.isNotEmpty(record.getTransactionRef())) {
            sb.append("流水号:").append(record.getTransactionRef()).append(" ");
        }
        if (StrUtil.isNotEmpty(record.getCounterParty())) {
            sb.append("交易方:").append(record.getCounterParty()).append(" ");
        }
        if (StrUtil.isNotEmpty(record.getDescription())) {
            sb.append("摘要:").append(record.getDescription()).append(" ");
        }
        if (StrUtil.isNotEmpty(record.getDirection())) {
            sb.append("方向:").append(record.getDirection());
        }
        return sb.toString().trim();
    }

    /**
     * 从条件Map中获取 BigDecimal 参数
     */
    private BigDecimal getBigDecimalParam(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return getBigDecimalFromObject(val);
    }

    /**
     * 将Object安全转为BigDecimal
     */
    private BigDecimal getBigDecimalFromObject(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            log.warn("无法转换为 BigDecimal: {}", val);
            return null;
        }
    }

    /**
     * 获取条件权重
     */
    private double getConditionWeight(Map<String, Object> condition) {
        Object weight = condition.get("weight");
        if (weight instanceof Number) {
            return ((Number) weight).doubleValue();
        }
        return 1.0; // 默认权重
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }

    // ========================================================================
    // 内部类
    // ========================================================================

    /**
     * 对账执行结果
     */
    @Getter
    @Builder
    public static class ReconResult {
        /** 未匹配的A方记录 */
        private final List<RawRecord> unmatchedA;
        /** 未匹配的B方记录 */
        private final List<RawRecord> unmatchedB;
        /** 精准匹配数 */
        @Builder.Default
        private final int exactMatched = 0;
        /** 规则匹配数 */
        @Builder.Default
        private final int ruleMatched = 0;
        /** AI语义匹配数 */
        @Builder.Default
        private final int aiMatched = 0;
        /** 拆单匹配数 */
        @Builder.Default
        private final int splitMatched = 0;
    }

    /**
     * AI打分结果包装 (内部使用)
     */
    private static class AIScoreResult {
        final RawRecord b;
        final MatchScoreResult result;

        AIScoreResult(RawRecord b, MatchScoreResult result) {
            this.b = b;
            this.result = result;
        }
    }

    /**
     * 子集求和结果 (内部使用)
     */
    private static class SubsetSumResult {
        final List<RawRecord> matchedRecords;
        final BigDecimal sum;

        SubsetSumResult(List<RawRecord> matchedRecords, BigDecimal sum) {
            this.matchedRecords = matchedRecords;
            this.sum = sum;
        }
    }
}
