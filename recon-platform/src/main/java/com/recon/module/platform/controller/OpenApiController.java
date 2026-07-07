package com.recon.module.platform.controller;

import com.recon.common.response.ApiResponse;
import com.recon.module.discrepancy.service.DiscrepancyService;
import com.recon.module.engine.entity.ReconTask;
import com.recon.module.engine.service.ReconTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 开放平台控制器
 *
 * @author recon-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/open")
@RequiredArgsConstructor
@Tag(name = "开放平台")
public class OpenApiController {

    private final ReconTaskService reconTaskService;
    private final DiscrepancyService discrepancyService;

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK");
    }

    @Operation(summary = "查询任务状态")
    @GetMapping("/task/{id}/status")
    public ApiResponse<Map<String, Object>> taskStatus(@PathVariable Long id) {
        log.info("开放API查询任务状态: taskId={}", id);
        ReconTask task = reconTaskService.getById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", id);
        result.put("status", task.getStatus());
        result.put("matchRate", task.getMatchRate());
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询差异数量")
    @GetMapping("/discrepancy/count")
    public ApiResponse<Long> discrepancyCount(
            @RequestParam Long orgId,
            @RequestParam(required = false) String status) {
        log.info("开放API查询差异数量: orgId={}, status={}", orgId, status);
        long count = discrepancyService.countDiscrepancies(orgId, status);
        return ApiResponse.success(count);
    }

    @Operation(summary = "注册Webhook")
    @PostMapping("/webhook/register")
    public ApiResponse<String> registerWebhook(@RequestBody Map<String, Object> body) {
        log.info("注册Webhook: body={}", body);
        return ApiResponse.success("Webhook registered successfully");
    }

    @Operation(summary = "导出任务数据")
    @GetMapping("/export/task/{id}")
    public ApiResponse<String> exportTask(@PathVariable Long id) {
        log.info("开放API导出任务: taskId={}", id);
        return ApiResponse.success("导出任务已提交");
    }
}
