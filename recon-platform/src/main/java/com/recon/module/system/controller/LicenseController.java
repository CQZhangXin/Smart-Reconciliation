package com.recon.module.system.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.utils.SecurityUtil;
import com.recon.module.system.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 许可证管理控制器
 *
 * @author recon-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/license")
@RequiredArgsConstructor
@Tag(name = "许可证管理")
public class LicenseController {

    private final LicenseService licenseService;

    @Operation(summary = "获取许可证状态")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Long orgId = SecurityUtil.getCurrentOrgId();
        if (orgId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        Map<String, Object> status = licenseService.getStatus(orgId);
        return ApiResponse.success(status);
    }

    @Operation(summary = "激活许可证 (上传 .lic 文件)")
    @PostMapping("/activate")
    public ApiResponse<Map<String, Object>> activate(@RequestParam("file") MultipartFile file) {
        Long orgId = SecurityUtil.getCurrentOrgId();
        if (orgId == null) {
            return ApiResponse.unauthorized("未登录");
        }

        try {
            String licenseData = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            // 移除可能的空白字符和换行
            licenseData = licenseData.replaceAll("\\s+", "");

            Map<String, Object> result = licenseService.activate(orgId, licenseData);
            log.info("许可证激活成功: orgId={}", orgId);
            return ApiResponse.success("许可证激活成功", result);
        } catch (IOException e) {
            log.error("读取许可证文件失败", e);
            return ApiResponse.badRequest("无法读取许可证文件");
        }
    }

    @Operation(summary = "激活许可证 (文本内容)")
    @PostMapping("/activate/text")
    public ApiResponse<Map<String, Object>> activateText(@RequestBody Map<String, String> body) {
        Long orgId = SecurityUtil.getCurrentOrgId();
        if (orgId == null) {
            return ApiResponse.unauthorized("未登录");
        }

        String licenseData = body.get("licenseData");
        if (licenseData == null || licenseData.isBlank()) {
            return ApiResponse.badRequest("许可证数据不能为空");
        }

        licenseData = licenseData.replaceAll("\\s+", "");
        Map<String, Object> result = licenseService.activate(orgId, licenseData);
        log.info("许可证激活成功: orgId={}", orgId);
        return ApiResponse.success("许可证激活成功", result);
    }

    @Operation(summary = "吊销许可证")
    @DeleteMapping("/revoke")
    public ApiResponse<String> revoke() {
        Long orgId = SecurityUtil.getCurrentOrgId();
        if (orgId == null) {
            return ApiResponse.unauthorized("未登录");
        }

        // 仅超级管理员可吊销
        // TODO: 当前 JWT Filter 未填充 authorities，hasRole 暂无法生效。
        // 修复 JWT Filter 从 token claims 或数据库加载角色后，以下校验将自动生效。
        if (!SecurityUtil.hasRole("ROLE_SUPER_ADMIN")) {
            return ApiResponse.forbidden("仅超级管理员可执行吊销操作");
        }
        licenseService.revoke(orgId);
        log.info("许可证已吊销: orgId={}", orgId);
        return ApiResponse.success("许可证已吊销");
    }
}
