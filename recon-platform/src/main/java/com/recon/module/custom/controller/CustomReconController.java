package com.recon.module.custom.controller;

import cn.hutool.core.util.StrUtil;
import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.custom.dto.CustomReconRunRequest;
import com.recon.module.custom.dto.CustomReconRunResult;
import com.recon.module.custom.dto.CustomReconValidateResult;
import com.recon.module.custom.dto.NLParseResult;
import com.recon.module.custom.entity.CustomReconDefinition;
import com.recon.module.custom.service.CustomReconService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 自定义对账控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/custom-recon")
@RequiredArgsConstructor
@Tag(name = "自定义对账")
public class CustomReconController {

    private final CustomReconService customReconService;

    @Operation(summary = "分页查询自定义对账方案")
    @GetMapping("/definition/page")
    public ApiResponse<PageResult<CustomReconDefinition>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        log.info("分页查询自定义对账方案: page={}, size={}, orgId={}, status={}, keyword={}",
                page, size, orgId, status, keyword);
        return ApiResponse.success(PageResult.of(
                customReconService.pageDefinitions(page, size, orgId, status, keyword)));
    }

    @Operation(summary = "根据ID查询自定义对账方案")
    @GetMapping("/definition/{id}")
    public ApiResponse<CustomReconDefinition> getById(@PathVariable Long id) {
        log.info("查询自定义对账方案: id={}", id);
        return ApiResponse.success(customReconService.getById(id));
    }

    @Operation(summary = "创建自定义对账方案")
    @PostMapping("/definition")
    public ApiResponse<CustomReconDefinition> create(@RequestBody @Valid CustomReconDefinition definition) {
        log.info("创建自定义对账方案: defName={}", definition.getDefName());
        return ApiResponse.success(customReconService.createDefinition(definition));
    }

    @Operation(summary = "更新自定义对账方案")
    @PutMapping("/definition/{id}")
    public ApiResponse<CustomReconDefinition> update(@PathVariable Long id,
                                                     @RequestBody CustomReconDefinition definition) {
        log.info("更新自定义对账方案: id={}", id);
        return ApiResponse.success(customReconService.updateDefinition(id, definition));
    }

    @Operation(summary = "删除自定义对账方案")
    @DeleteMapping("/definition/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除自定义对账方案: id={}", id);
        customReconService.deleteDefinition(id);
        return ApiResponse.success();
    }

    @Operation(summary = "自然语言解析对账方案定义")
    @PostMapping("/definition/nl-parse")
    public ApiResponse<NLParseResult> nlParse(@RequestBody Map<String, Object> request) {
        String description = request.get("description") != null ? request.get("description").toString() : null;
        Long orgId = request.get("orgId") != null ? Long.valueOf(request.get("orgId").toString()) : null;
        if (StrUtil.isBlank(description)) {
            return ApiResponse.badRequest("自然语言描述不能为空");
        }
        if (orgId == null) {
            return ApiResponse.badRequest("orgId不能为空");
        }
        log.info("NL解析对账方案: description={}, orgId={}", description, orgId);
        return ApiResponse.success(customReconService.parseFromNL(description, orgId));
    }

    @Operation(summary = "启用自定义对账方案")
    @PutMapping("/definition/{id}/enable")
    public ApiResponse<CustomReconDefinition> enable(@PathVariable Long id) {
        log.info("启用自定义对账方案: id={}", id);
        return ApiResponse.success(customReconService.enable(id));
    }

    @Operation(summary = "停用自定义对账方案")
    @PutMapping("/definition/{id}/disable")
    public ApiResponse<CustomReconDefinition> disable(@PathVariable Long id) {
        log.info("停用自定义对账方案: id={}", id);
        return ApiResponse.success(customReconService.disable(id));
    }

    @Operation(summary = "预检自定义对账方案")
    @PostMapping("/definition/{id}/validate")
    public ApiResponse<CustomReconValidateResult> validate(@PathVariable Long id) {
        log.info("预检自定义对账方案: id={}", id);
        return ApiResponse.success(customReconService.validate(id));
    }

    @Operation(summary = "执行自定义对账方案")
    @PostMapping("/definition/{id}/run")
    public ApiResponse<CustomReconRunResult> run(@PathVariable Long id,
                                                 @RequestBody(required = false) CustomReconRunRequest request) {
        log.info("执行自定义对账方案: id={}", id);
        if (request == null) {
            request = new CustomReconRunRequest();
        }
        return ApiResponse.success(customReconService.run(id, request));
    }
}
