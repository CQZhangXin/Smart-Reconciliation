package com.recon.module.workflow.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.workflow.dto.NLWorkflowParseResult;
import com.recon.module.workflow.entity.WfApprovalRecord;
import com.recon.module.workflow.entity.WfProcessDefinition;
import com.recon.module.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审批工作流控制器
 *
 * @author recon-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
@Tag(name = "审批工作流")
public class WorkflowController {

    private final WorkflowService workflowService;

    // ========== 流程定义 ==========

    @Operation(summary = "分页查询流程定义")
    @GetMapping("/process-def/page")
    public ApiResponse<PageResult<WfProcessDefinition>> processDefPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId) {
        log.info("分页查询流程定义: page={}, size={}, orgId={}", page, size, orgId);
        PageResult<WfProcessDefinition> result = PageResult.of(
                workflowService.pageProcessDefs(page, size, orgId));
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建流程定义")
    @PostMapping("/process-def")
    public ApiResponse<WfProcessDefinition> createProcessDef(@RequestBody WfProcessDefinition definition) {
        log.info("创建流程定义: processName={}", definition.getProcessName());
        WfProcessDefinition created = workflowService.createProcessDef(definition);
        return ApiResponse.success(created);
    }

    @Operation(summary = "发布流程定义")
    @PutMapping("/process-def/{id}/publish")
    public ApiResponse<WfProcessDefinition> publishProcessDef(@PathVariable Long id) {
        log.info("发布流程定义: id={}", id);
        WfProcessDefinition definition = workflowService.publishProcessDef(id);
        return ApiResponse.success(definition);
    }

    @Operation(summary = "自然语言解析流程定义")
    @PostMapping("/process-def/nl-parse")
    public ApiResponse<NLWorkflowParseResult> nlParseProcessDef(@RequestBody Map<String, String> body) {
        String description = body.get("description");
        log.info("自然语言解析流程定义: description={}", description);
        NLWorkflowParseResult result = workflowService.parseFromNL(description);
        return ApiResponse.success(result);
    }

    // ========== 审批记录 ==========

    @Operation(summary = "提交审批")
    @PostMapping("/approval/submit")
    public ApiResponse<WfApprovalRecord> submitApproval(@RequestBody Map<String, Object> body) {
        log.info("提交审批: body={}", body);
        Long orgId = toLong(body.get("orgId"));
        String businessType = (String) body.get("businessType");
        Long businessId = toLong(body.get("businessId"));
        Long processDefId = toLong(body.get("processDefId"));
        WfApprovalRecord record = workflowService.submitApproval(orgId, businessType, businessId, processDefId);
        return ApiResponse.success(record);
    }

    @Operation(summary = "审批操作")
    @PutMapping("/approval/{id}/approve")
    public ApiResponse<WfApprovalRecord> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("审批操作: id={}, body={}", id, body);
        Long approverId = toLong(body.get("approverId"));
        String approverName = (String) body.get("approverName");
        String action = (String) body.get("action");
        String comment = (String) body.get("comment");
        WfApprovalRecord record = workflowService.approve(id, approverId, approverName, action, comment);
        return ApiResponse.success(record);
    }

    @Operation(summary = "查询我的审批")
    @GetMapping("/approval/my")
    public ApiResponse<PageResult<WfApprovalRecord>> myApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Long approverId,
            @RequestParam(required = false) String status) {
        log.info("查询我的审批: page={}, size={}, approverId={}, status={}", page, size, approverId, status);
        PageResult<WfApprovalRecord> result = PageResult.of(
                workflowService.pageMyApprovals(page, size, approverId, status));
        return ApiResponse.success(result);
    }

    @Operation(summary = "分页查询审批记录")
    @GetMapping("/approval/page")
    public ApiResponse<PageResult<WfApprovalRecord>> approvalPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status) {
        log.info("分页查询审批记录: page={}, size={}, orgId={}, businessType={}, status={}",
                page, size, orgId, businessType, status);
        PageResult<WfApprovalRecord> result = PageResult.of(
                workflowService.pageApprovals(page, size, orgId, businessType, status));
        return ApiResponse.success(result);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 安全地将Object转为Long，兼容Integer/String类型
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("无法转换为Long: {}", value);
            return null;
        }
    }
}
