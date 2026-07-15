package com.recon.module.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recon.ai.dto.NLQueryResult;
import com.recon.ai.dto.ReportGenerateRequest;
import com.recon.ai.service.AIService;
import com.recon.module.discrepancy.entity.ReconDiscrepancy;
import com.recon.module.discrepancy.repository.ReconDiscrepancyMapper;
import com.recon.module.engine.entity.ReconTask;
import com.recon.module.engine.repository.ReconTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析服务 — 仪表盘看板、趋势、对比、报告生成、自然语言查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReconTaskMapper taskMapper;
    private final ReconDiscrepancyMapper discrepancyMapper;
    private final AIService aiService;

    // ==================== 仪表盘 ====================

    /**
     * 获取仪表盘综合数据
     *
     * <p><b>性能警告 (TODO):</b> 此方法会一次性加载所有差异记录至内存，在大数据量
     * 场景下可能导致 OOM。建议改为使用 COUNT 聚合查询替代全量加载，
     * 或在查询层面添加数据量上限。</p>
     */
    public Map<String, Object> getDashboardData(Long orgId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // ---- autoMatchRate: 近期已完成任务的平均匹配率 ----
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<ReconTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(orgId != null, ReconTask::getOrgId, orgId);
        taskWrapper.eq(ReconTask::getStatus, "COMPLETED");
        taskWrapper.ge(ReconTask::getCompletedAt, thirtyDaysAgo);
        List<ReconTask> recentTasks = taskMapper.selectList(taskWrapper);
        double autoMatchRate = recentTasks.stream()
                .map(ReconTask::getMatchRate)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        dashboard.put("autoMatchRate", BigDecimal.valueOf(autoMatchRate)
                .setScale(2, RoundingMode.HALF_UP));

        // ---- discrepancyResolveRate: 已解决/总差异 ----
        LambdaQueryWrapper<ReconDiscrepancy> discWrapper = new LambdaQueryWrapper<>();
        discWrapper.eq(orgId != null, ReconDiscrepancy::getOrgId, orgId);
        List<ReconDiscrepancy> allDiscs = discrepancyMapper.selectList(discWrapper);
        long totalDisc = allDiscs.size();
        long resolvedDisc = allDiscs.stream()
                .filter(d -> "RESOLVED".equals(d.getStatus()))
                .count();
        double resolveRate = totalDisc == 0 ? 0.0 : (double) resolvedDisc / totalDisc * 100;
        dashboard.put("discrepancyResolveRate", BigDecimal.valueOf(resolveRate)
                .setScale(2, RoundingMode.HALF_UP));

        // ---- avgCloseDays: 平均关单天数 ----
        double avgCloseDays = allDiscs.stream()
                .filter(d -> d.getResolvedAt() != null && d.getCreatedAt() != null)
                .mapToLong(d -> ChronoUnit.DAYS.between(
                        d.getCreatedAt().toLocalDate(),
                        d.getResolvedAt().toLocalDate()))
                .average()
                .orElse(0.0);
        dashboard.put("avgCloseDays", BigDecimal.valueOf(avgCloseDays)
                .setScale(1, RoundingMode.HALF_UP));

        // ---- trendData: 最近12个月匹配率趋势 ----
        List<Map<String, Object>> trendData = buildMonthlyTrend(orgId, 12);
        dashboard.put("trendData", trendData);

        // ---- categoryDistribution: 差异分类分布 ----
        Map<String, Long> categoryDist = allDiscs.stream()
                .filter(d -> d.getCategory() != null && !d.getCategory().isBlank())
                .collect(Collectors.groupingBy(
                        ReconDiscrepancy::getCategory,
                        LinkedHashMap::new,
                        Collectors.counting()));
        dashboard.put("categoryDistribution", categoryDist);

        // ---- riskDistribution: 风险等级分布 ----
        Map<String, Long> riskDist = allDiscs.stream()
                .filter(d -> d.getRiskLevel() != null && !d.getRiskLevel().isBlank())
                .collect(Collectors.groupingBy(
                        ReconDiscrepancy::getRiskLevel,
                        LinkedHashMap::new,
                        Collectors.counting()));
        dashboard.put("riskDistribution", riskDist);

        return dashboard;
    }

    // ==================== 任务健康指标 ====================

    /**
     * 获取单个任务的健康指标
     */
    public Map<String, Object> getTaskHealthMetrics(Long taskId) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        ReconTask task = taskMapper.selectById(taskId);
        if (task == null) {
            metrics.put("error", "Task not found");
            return metrics;
        }

        // matchRate
        metrics.put("matchRate", task.getMatchRate());

        // discrepancy counts by status
        LambdaQueryWrapper<ReconDiscrepancy> discWrapper = new LambdaQueryWrapper<>();
        discWrapper.eq(ReconDiscrepancy::getTaskId, taskId);
        List<ReconDiscrepancy> discList = discrepancyMapper.selectList(discWrapper);

        long pendingCount = discList.stream()
                .filter(d -> "PENDING".equals(d.getStatus())).count();
        long resolvedCount = discList.stream()
                .filter(d -> "RESOLVED".equals(d.getStatus())).count();
        long closedCount = discList.stream()
                .filter(d -> "CLOSED".equals(d.getStatus())).count();

        metrics.put("discrepancyCount", discList.size());
        metrics.put("pendingCount", pendingCount);
        metrics.put("resolvedCount", resolvedCount);
        metrics.put("closedCount", closedCount);

        // avg handling days for resolved
        double avgHandlingDays = discList.stream()
                .filter(d -> d.getResolvedAt() != null && d.getCreatedAt() != null)
                .mapToLong(d -> ChronoUnit.DAYS.between(
                        d.getCreatedAt().toLocalDate(),
                        d.getResolvedAt().toLocalDate()))
                .average()
                .orElse(0.0);
        metrics.put("avgHandlingDays", BigDecimal.valueOf(avgHandlingDays)
                .setScale(1, RoundingMode.HALF_UP));

        return metrics;
    }

    // ==================== 趋势数据 ====================

    /**
     * 获取最近N个月的匹配率趋势数据
     */
    public List<Map<String, Object>> getTrendData(Long orgId, String months) {
        int monthCount = parseMonths(months);
        return buildMonthlyTrend(orgId, monthCount);
    }

    // ==================== 期间对比 ====================

    /**
     * 对比两个对账期间的指标差异
     */
    public Map<String, Object> comparePeriods(Long orgId, String period1, String period2) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> p1 = computePeriodMetrics(orgId, period1);
        Map<String, Object> p2 = computePeriodMetrics(orgId, period2);

        result.put("period1", p1);
        result.put("period2", p2);

        // 差值
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("matchRateDelta", subtractBigDecimal(
                (BigDecimal) p2.getOrDefault("matchRate", BigDecimal.ZERO),
                (BigDecimal) p1.getOrDefault("matchRate", BigDecimal.ZERO)));
        delta.put("discrepancyCountDelta",
                ((Number) p2.getOrDefault("discrepancyCount", 0L)).longValue()
                        - ((Number) p1.getOrDefault("discrepancyCount", 0L)).longValue());
        result.put("delta", delta);

        return result;
    }

    // ==================== AI报告与查询 ====================

    /**
     * AI生成对账报告
     */
    public String generateReport(ReportGenerateRequest request) {
        return aiService.generateReconReport(request);
    }

    /**
     * 自然语言查询
     */
    public NLQueryResult naturalLanguageQuery(String question, Long orgId) {
        return aiService.naturalLanguageQuery(question, orgId);
    }

    // ============================================================
    // Private helpers
    // ============================================================

    /**
     * 构建最近N个月的月度匹配率趋势
     */
    private List<Map<String, Object>> buildMonthlyTrend(Long orgId, int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        LambdaQueryWrapper<ReconTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(orgId != null, ReconTask::getOrgId, orgId);
        wrapper.eq(ReconTask::getStatus, "COMPLETED");
        wrapper.ge(ReconTask::getCompletedAt, since);
        List<ReconTask> tasks = taskMapper.selectList(wrapper);

        // 按月分组求平均匹配率
        Map<YearMonth, Double> monthlyAvg = tasks.stream()
                .filter(t -> t.getCompletedAt() != null && t.getMatchRate() != null)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getCompletedAt()),
                        Collectors.averagingDouble(t -> t.getMatchRate().doubleValue())));

        // 构造连续的月份序列
        List<Map<String, Object>> trendData = new ArrayList<>();
        YearMonth current = YearMonth.now().minusMonths(months - 1);
        YearMonth end = YearMonth.now();
        while (!current.isAfter(end)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("period", current.toString());
            double rate = monthlyAvg.getOrDefault(current, 0.0);
            point.put("matchRate", BigDecimal.valueOf(rate)
                    .setScale(2, RoundingMode.HALF_UP));
            trendData.add(point);
            current = current.plusMonths(1);
        }

        return trendData;
    }

    /**
     * 解析月数, e.g. "12m" -> 12
     */
    private int parseMonths(String months) {
        if (months == null || months.isBlank()) {
            return 12;
        }
        try {
            if (months.endsWith("m") || months.endsWith("M")) {
                return Integer.parseInt(months.substring(0, months.length() - 1));
            }
            return Integer.parseInt(months);
        } catch (NumberFormatException e) {
            return 12;
        }
    }

    /**
     * 计算某个时间段的指标
     * period 格式: "yyyy-MM,yyyy-MM" 或 "yyyy-MM-dd,yyyy-MM-dd"
     */
    private Map<String, Object> computePeriodMetrics(Long orgId, String period) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        LocalDateTime[] range = parsePeriodRange(period);
        if (range == null) {
            metrics.put("error", "Invalid period format: " + period);
            return metrics;
        }
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        // 该期间内已完成的任务
        LambdaQueryWrapper<ReconTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(orgId != null, ReconTask::getOrgId, orgId);
        taskWrapper.eq(ReconTask::getStatus, "COMPLETED");
        taskWrapper.ge(ReconTask::getCompletedAt, start);
        taskWrapper.le(ReconTask::getCompletedAt, end);
        List<ReconTask> tasks = taskMapper.selectList(taskWrapper);

        double avgMatchRate = tasks.stream()
                .map(ReconTask::getMatchRate)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        metrics.put("matchRate", BigDecimal.valueOf(avgMatchRate)
                .setScale(2, RoundingMode.HALF_UP));
        metrics.put("taskCount", tasks.size());

        // 该期间内的差异
        LambdaQueryWrapper<ReconDiscrepancy> discWrapper = new LambdaQueryWrapper<>();
        discWrapper.eq(orgId != null, ReconDiscrepancy::getOrgId, orgId);
        discWrapper.ge(ReconDiscrepancy::getCreatedAt, start);
        discWrapper.le(ReconDiscrepancy::getCreatedAt, end);
        List<ReconDiscrepancy> discs = discrepancyMapper.selectList(discWrapper);

        metrics.put("discrepancyCount", discs.size());
        metrics.put("resolvedCount",
                discs.stream().filter(d -> "RESOLVED".equals(d.getStatus())).count());

        double avgHours = discs.stream()
                .filter(d -> d.getResolvedAt() != null && d.getCreatedAt() != null)
                .mapToLong(d -> ChronoUnit.HOURS.between(d.getCreatedAt(), d.getResolvedAt()))
                .average()
                .orElse(0.0);
        metrics.put("avgHandlingTimeHours", BigDecimal.valueOf(avgHours)
                .setScale(1, RoundingMode.HALF_UP));

        return metrics;
    }

    /**
     * 解析时间段 "yyyy-MM,yyyy-MM" 或 "yyyy-MM-dd,yyyy-MM-dd"
     */
    private LocalDateTime[] parsePeriodRange(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        String[] parts = period.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            LocalDateTime start;
            LocalDateTime end;
            if (parts[0].trim().length() == 7) {
                YearMonth ymStart = YearMonth.parse(parts[0].trim());
                YearMonth ymEnd = YearMonth.parse(parts[1].trim());
                start = ymStart.atDay(1).atStartOfDay();
                end = ymEnd.atEndOfMonth().atTime(23, 59, 59);
            } else {
                start = LocalDateTime.parse(parts[0].trim() + "T00:00:00");
                end = LocalDateTime.parse(parts[1].trim() + "T23:59:59");
            }
            return new LocalDateTime[]{start, end};
        } catch (Exception e) {
            log.warn("Failed to parse period range: {}", period, e);
            return null;
        }
    }

    private BigDecimal subtractBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.subtract(b).setScale(2, RoundingMode.HALF_UP);
    }
}
