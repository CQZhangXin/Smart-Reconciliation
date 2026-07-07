package com.recon.module.analytics.controller;

import com.recon.ai.dto.NLQueryResult;
import com.recon.ai.dto.ReportGenerateRequest;
import com.recon.common.response.ApiResponse;
import com.recon.module.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能分析控制器
 *
 * @author recon-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "智能分析")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "获取仪表盘数据")
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam Long orgId) {
        log.info("获取仪表盘数据: orgId={}", orgId);
        Map<String, Object> data = analyticsService.getDashboardData(orgId);
        return ApiResponse.success(data);
    }

    @Operation(summary = "获取任务健康度分析")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health(@RequestParam Long taskId) {
        log.info("获取任务健康度: taskId={}", taskId);
        Map<String, Object> data = analyticsService.getTaskHealthMetrics(taskId);
        return ApiResponse.success(data);
    }

    @Operation(summary = "获取对账趋势数据")
    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "12m") String months) {
        log.info("获取对账趋势: orgId={}, months={}", orgId, months);
        List<Map<String, Object>> data = analyticsService.getTrendData(orgId, months);
        return ApiResponse.success(data);
    }

    @Operation(summary = "环比分析")
    @PostMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(@RequestBody Map<String, Object> body) {
        log.info("环比分析: body={}", body);
        Long orgId = body.get("orgId") != null ? Long.parseLong(body.get("orgId").toString()) : null;
        String period1 = (String) body.get("period1");
        String period2 = (String) body.get("period2");
        Map<String, Object> data = analyticsService.comparePeriods(orgId, period1, period2);
        return ApiResponse.success(data);
    }

    @Operation(summary = "生成对账报告")
    @PostMapping("/report")
    public ApiResponse<String> generateReport(@RequestBody ReportGenerateRequest request) {
        log.info("生成对账报告: orgId={}, taskId={}, reportType={}",
                request.getOrgId(), request.getTaskId(), request.getReportType());
        String reportUrl = analyticsService.generateReport(request);
        return ApiResponse.success(reportUrl);
    }

    @Operation(summary = "自然语言查询")
    @PostMapping("/nl-query")
    public ApiResponse<NLQueryResult> nlQuery(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        Long orgId = body.get("orgId") != null ? Long.parseLong(body.get("orgId").toString()) : null;
        log.info("自然语言查询: orgId={}, question={}", orgId, question);
        NLQueryResult result = analyticsService.naturalLanguageQuery(question, orgId);
        return ApiResponse.success(result);
    }
}
