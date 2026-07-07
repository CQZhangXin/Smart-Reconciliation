package com.recon.module.engine.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.engine.entity.ReconMatch;
import com.recon.module.engine.entity.ReconTask;
import com.recon.module.engine.service.ReconTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 对账工作台控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recon")
@RequiredArgsConstructor
@Tag(name = "对账工作台")
public class ReconTaskController {

    private final ReconTaskService reconTaskService;

    // ========== 任务管理 ==========

    @Operation(summary = "分页查询对账任务")
    @GetMapping("/task/page")
    public ApiResponse<PageResult<ReconTask>> taskPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        log.info("分页查询对账任务: page={}, size={}, orgId={}, status={}, taskType={}", page, size, orgId, status, taskType);
        return ApiResponse.success(PageResult.of(reconTaskService.pageTasks(page, size, orgId, status, taskType)));
    }

    @Operation(summary = "根据ID查询对账任务")
    @GetMapping("/task/{id}")
    public ApiResponse<ReconTask> getTaskById(@PathVariable Long id) {
        log.info("查询对账任务: id={}", id);
        return ApiResponse.success(reconTaskService.getById(id));
    }

    @Operation(summary = "查询对账任务汇总")
    @GetMapping("/task/{id}/summary")
    public ApiResponse<ReconTask> getTaskSummary(@PathVariable Long id) {
        log.info("查询对账任务汇总: id={}", id);
        return ApiResponse.success(reconTaskService.getTaskSummary(id));
    }

    @Operation(summary = "创建对账任务")
    @PostMapping("/task")
    public ApiResponse<ReconTask> createTask(@RequestBody @Valid ReconTask task) {
        log.info("创建对账任务: taskName={}", task.getTaskName());
        return ApiResponse.success(reconTaskService.createTask(task));
    }

    @Operation(summary = "更新对账任务")
    @PutMapping("/task/{id}")
    public ApiResponse<ReconTask> updateTask(@PathVariable Long id, @RequestBody ReconTask task) {
        log.info("更新对账任务: id={}", id);
        return ApiResponse.success(reconTaskService.updateTask(id, task));
    }

    @Operation(summary = "删除对账任务")
    @DeleteMapping("/task/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        log.info("删除对账任务: id={}", id);
        reconTaskService.deleteTask(id);
        return ApiResponse.success();
    }

    @Operation(summary = "同步执行对账任务")
    @PostMapping("/task/{id}/execute")
    public ApiResponse<ReconTask> executeTask(@PathVariable Long id) {
        log.info("同步执行对账任务: id={}", id);
        return ApiResponse.success(reconTaskService.executeTask(id));
    }

    @Operation(summary = "异步执行对账任务")
    @PostMapping("/task/{id}/execute-async")
    public ApiResponse<String> executeTaskAsync(@PathVariable Long id) {
        log.info("异步执行对账任务: id={}", id);
        reconTaskService.executeTaskAsync(id);
        return ApiResponse.success("任务已提交异步执行");
    }

    // ========== 匹配结果管理 ==========

    @Operation(summary = "分页查询匹配结果")
    @GetMapping("/match/page")
    public ApiResponse<PageResult<ReconMatch>> matchPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String matchType,
            @RequestParam(required = false) String status) {
        log.info("分页查询匹配结果: page={}, size={}, taskId={}, matchType={}, status={}", page, size, taskId, matchType, status);
        return ApiResponse.success(PageResult.of(reconTaskService.pageMatches(page, size, taskId, matchType, status)));
    }

    @Operation(summary = "确认匹配结果")
    @PutMapping("/match/{id}/confirm")
    public ApiResponse<String> confirmMatch(@PathVariable Long id, @RequestParam Long reviewedBy) {
        log.info("确认匹配结果: id={}, reviewedBy={}", id, reviewedBy);
        reconTaskService.confirmMatch(id, reviewedBy);
        return ApiResponse.success("匹配结果已确认");
    }

    @Operation(summary = "拒绝匹配结果")
    @PutMapping("/match/{id}/reject")
    public ApiResponse<String> rejectMatch(@PathVariable Long id,
                                            @RequestParam Long reviewedBy,
                                            @RequestParam(required = false) String comment) {
        log.info("拒绝匹配结果: id={}, reviewedBy={}, comment={}", id, reviewedBy, comment);
        reconTaskService.rejectMatch(id, reviewedBy, comment);
        return ApiResponse.success("匹配结果已拒绝");
    }

    @Operation(summary = "查询待审核匹配结果")
    @GetMapping("/match/pending-review")
    public ApiResponse<List<ReconMatch>> pendingReview(@RequestParam Long taskId) {
        log.info("查询待审核匹配结果: taskId={}", taskId);
        return ApiResponse.success(reconTaskService.getPendingReviewMatches(taskId));
    }
}
