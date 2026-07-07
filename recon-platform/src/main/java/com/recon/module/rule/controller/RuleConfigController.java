package com.recon.module.rule.controller;

import com.recon.ai.dto.RuleGenerationResult;
import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.rule.entity.ReconRuleConfig;
import com.recon.module.rule.service.RuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 规则引擎控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
@Tag(name = "规则引擎")
public class RuleConfigController {

    private final RuleConfigService ruleConfigService;

    @Operation(summary = "分页查询规则配置")
    @GetMapping("/page")
    public ApiResponse<PageResult<ReconRuleConfig>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String status) {
        log.info("分页查询规则配置: page={}, size={}, orgId={}, ruleType={}, status={}", page, size, orgId, ruleType, status);
        return ApiResponse.success(PageResult.of(
                ruleConfigService.pageRules(page, size, orgId, ruleType, null, status)));
    }

    @Operation(summary = "根据ID查询规则配置")
    @GetMapping("/{id}")
    public ApiResponse<ReconRuleConfig> getById(@PathVariable Long id) {
        log.info("查询规则配置: id={}", id);
        return ApiResponse.success(ruleConfigService.getById(id));
    }

    @Operation(summary = "创建规则配置")
    @PostMapping
    public ApiResponse<ReconRuleConfig> create(@RequestBody @Valid ReconRuleConfig config) {
        log.info("创建规则配置: ruleName={}", config.getRuleName());
        return ApiResponse.success(ruleConfigService.createRule(config));
    }

    @Operation(summary = "更新规则配置")
    @PutMapping("/{id}")
    public ApiResponse<ReconRuleConfig> update(@PathVariable Long id, @RequestBody ReconRuleConfig config) {
        log.info("更新规则配置: id={}", id);
        return ApiResponse.success(ruleConfigService.updateRule(id, config));
    }

    @Operation(summary = "删除规则配置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除规则配置: id={}", id);
        ruleConfigService.deleteRule(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启用规则")
    @PutMapping("/{id}/enable")
    public ApiResponse<ReconRuleConfig> enable(@PathVariable Long id) {
        log.info("启用规则: id={}", id);
        return ApiResponse.success(ruleConfigService.enableRule(id));
    }

    @Operation(summary = "禁用规则")
    @PutMapping("/{id}/disable")
    public ApiResponse<ReconRuleConfig> disable(@PathVariable Long id) {
        log.info("禁用规则: id={}", id);
        return ApiResponse.success(ruleConfigService.disableRule(id));
    }

    @Operation(summary = "查询活跃规则列表")
    @GetMapping("/active")
    public ApiResponse<List<ReconRuleConfig>> listActive(@RequestParam Long orgId) {
        log.info("查询活跃规则: orgId={}", orgId);
        return ApiResponse.success(ruleConfigService.getActiveRules(orgId));
    }

    @Operation(summary = "自然语言生成规则")
    @PostMapping("/generate-from-nl")
    public ApiResponse<RuleGenerationResult> generateFromNL(@RequestBody Map<String, String> body) {
        String description = body.get("description");
        Long orgId = body.get("orgId") != null ? Long.parseLong(body.get("orgId")) : null;
        log.info("自然语言生成规则: orgId={}, description={}", orgId, description);
        return ApiResponse.success(ruleConfigService.generateRuleFromNL(description, orgId));
    }

    @Operation(summary = "保存AI生成的规则")
    @PostMapping("/save-generated")
    public ApiResponse<ReconRuleConfig> saveGenerated(@RequestBody Map<String, Object> body) {
        log.info("保存AI生成的规则");
        @SuppressWarnings("unchecked")
        Map<String, Object> genMap = (Map<String, Object>) body.get("generated");
        Long orgId = toLong(body.get("orgId"));
        Long createdBy = toLong(body.get("createdBy"));

        RuleGenerationResult generated = RuleGenerationResult.builder()
                .ruleName((String) genMap.get("ruleName"))
                .ruleCode((String) genMap.get("ruleCode"))
                .ruleType((String) genMap.get("ruleType"))
                .matchConfigJson((String) genMap.get("matchConfigJson"))
                .toleranceJson((String) genMap.get("toleranceJson"))
                .explanation((String) genMap.get("explanation"))
                .estimatedMatchRate(genMap.get("estimatedMatchRate") != null
                        ? ((Number) genMap.get("estimatedMatchRate")).intValue() : null)
                .hasConflict(genMap.get("hasConflict") != null
                        ? (Boolean) genMap.get("hasConflict") : false)
                .conflictDetail((String) genMap.get("conflictDetail"))
                .build();

        return ApiResponse.success(ruleConfigService.saveGeneratedRule(generated, orgId, createdBy));
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return null; }
    }
}
