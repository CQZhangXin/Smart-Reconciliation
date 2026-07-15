package com.recon.module.discrepancy.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.discrepancy.entity.ReconAdjustment;
import com.recon.module.discrepancy.entity.ReconDiscrepancy;
import com.recon.module.discrepancy.service.DiscrepancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 差异管理中心控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/discrepancy")
@RequiredArgsConstructor
@Tag(name = "差异管理中心")
public class DiscrepancyController {

    private final DiscrepancyService discrepancyService;

    @Operation(summary = "分页查询差异记录")
    @GetMapping("/page")
    public ApiResponse<PageResult<ReconDiscrepancy>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long handlerId) {
        log.info("分页查询差异记录: page={}, size={}, orgId={}, taskId={}", page, size, orgId, taskId);
        return ApiResponse.success(PageResult.of(
                discrepancyService.pageDiscrepancies(page, size, orgId, taskId, category, riskLevel, status, handlerId)));
    }

    @Operation(summary = "根据ID查询差异记录")
    @GetMapping("/{id}")
    public ApiResponse<ReconDiscrepancy> getById(@PathVariable Long id) {
        log.info("查询差异记录: id={}", id);
        return ApiResponse.success(discrepancyService.getById(id));
    }

    @Operation(summary = "AI智能分类差异")
    @PostMapping("/{id}/classify")
    public ApiResponse<ReconDiscrepancy> classify(@PathVariable Long id) {
        log.info("AI智能分类差异: id={}", id);
        return ApiResponse.success(discrepancyService.classifyAndAnalyze(id));
    }

    @Operation(summary = "根因分析")
    @PostMapping("/{id}/root-cause")
    public ApiResponse<com.recon.ai.dto.RootCauseResult> rootCauseAnalysis(@PathVariable Long id) {
        log.info("根因分析: discrepancyId={}", id);
        com.recon.ai.dto.RootCauseResult result = discrepancyService.analyzeRootCause(id);
        return ApiResponse.success(result);
    }

    @Operation(summary = "批量AI分类差异")
    @PostMapping("/batch-classify")
    public ApiResponse<Void> batchClassify(@RequestBody Map<String, Object> body) {
        log.info("批量AI分类差异: body={}", body);
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) body.get("ids")).stream()
                .map(Number::longValue).toList();
        for (Long id : ids) {
            discrepancyService.classifyAndAnalyze(id);
        }
        return ApiResponse.success();
    }

    @Operation(summary = "分配处理人")
    @PutMapping("/{id}/assign")
    public ApiResponse<ReconDiscrepancy> assign(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> body) {
        Long handlerId = toLong(body.get("handlerId"));
        String handlerName = (String) body.get("handlerName");
        log.info("分配处理人: id={}, handlerId={}, handlerName={}", id, handlerId, handlerName);
        return ApiResponse.success(discrepancyService.assignHandler(id, handlerId, handlerName));
    }

    @Operation(summary = "解决差异")
    @PutMapping("/{id}/resolve")
    public ApiResponse<ReconDiscrepancy> resolve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("解决差异: id={}", id);
        String resolution = (String) body.getOrDefault("resolution", "MANUAL_MATCH");
        String resolutionNote = (String) body.getOrDefault("resolutionNote", "");
        Long resolvedBy = toLong(body.get("resolvedBy"));
        return ApiResponse.success(discrepancyService.resolveDiscrepancy(id, resolution, resolutionNote, resolvedBy));
    }

    @Operation(summary = "关闭差异")
    @PutMapping("/{id}/close")
    public ApiResponse<ReconDiscrepancy> close(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long resolvedBy = toLong(body.get("resolvedBy"));
        log.info("关闭差异: id={}, resolvedBy={}", id, resolvedBy);
        return ApiResponse.success(discrepancyService.closeDiscrepancy(id, resolvedBy));
    }

    @Operation(summary = "查询SLA超期差异")
    @GetMapping("/sla-overdue")
    public ApiResponse<List<ReconDiscrepancy>> slaOverdue(@RequestParam Long orgId) {
        log.info("查询SLA超期差异: orgId={}", orgId);
        return ApiResponse.success(discrepancyService.getSlaOverdue(orgId));
    }

    @Operation(summary = "查询差异统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats(@RequestParam Long taskId) {
        log.info("查询差异统计: taskId={}", taskId);
        return ApiResponse.success(discrepancyService.getDiscrepancyStats(taskId));
    }

    // ========== 调整管理 ==========

    @Operation(summary = "分页查询调整记录")
    @GetMapping("/adjustment/page")
    public ApiResponse<PageResult<ReconAdjustment>> adjustmentPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long discrepancyId) {
        log.info("分页查询调整记录: page={}, size={}, orgId={}, discrepancyId={}", page, size, orgId, discrepancyId);
        return ApiResponse.success(PageResult.of(discrepancyService.pageAdjustments(page, size, orgId, discrepancyId)));
    }

    @Operation(summary = "创建调整记录")
    @PostMapping("/adjustment")
    public ApiResponse<ReconAdjustment> createAdjustment(@RequestBody @Valid ReconAdjustment adjustment) {
        log.info("创建调整记录: discrepancyId={}, amount={}", adjustment.getDiscrepancyId(), adjustment.getAmount());
        return ApiResponse.success(discrepancyService.createAdjustment(adjustment));
    }

    @Operation(summary = "审批调整记录")
    @PutMapping("/adjustment/{id}/approve")
    public ApiResponse<ReconAdjustment> approveAdjustment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approvedBy = toLong(body.get("approvedBy"));
        log.info("审批调整记录: id={}, approvedBy={}", id, approvedBy);
        return ApiResponse.success(discrepancyService.approveAdjustment(id, approvedBy));
    }

    // ========== 开放平台接口 ==========

    @Operation(summary = "统计差异数量")
    @GetMapping("/count")
    public ApiResponse<Long> countDiscrepancies(@RequestParam Long orgId,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.success(discrepancyService.countDiscrepancies(orgId, status));
    }

    /** Safe Long conversion from Object */
    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
