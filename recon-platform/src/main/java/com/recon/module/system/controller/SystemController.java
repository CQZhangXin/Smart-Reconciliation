package com.recon.module.system.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.system.entity.*;
import com.recon.module.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 系统管理控制器
 *
 * @author recon-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "系统管理")
public class SystemController {

    private final SystemService systemService;

    // ========== 用户管理 ==========

    @Operation(summary = "分页查询用户")
    @GetMapping("/user/page")
    public ApiResponse<PageResult<SysUser>> userPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status) {
        log.info("分页查询用户: page={}, size={}, orgId={}, status={}", page, size, orgId, status);
        PageResult<SysUser> result = PageResult.of(systemService.pageUsers(page, size, orgId, status));
        return ApiResponse.success(result);
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/user/{id}")
    public ApiResponse<SysUser> getUserById(@PathVariable Long id) {
        log.info("查询用户: id={}", id);
        SysUser user = systemService.getUserById(id);
        return ApiResponse.success(user);
    }

    @Operation(summary = "创建用户")
    @PostMapping("/user")
    public ApiResponse<SysUser> createUser(@RequestBody @Valid SysUser user) {
        log.info("创建用户: username={}", user.getUsername());
        SysUser created = systemService.createUser(user);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/user/{id}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        log.info("更新用户: id={}", id);
        SysUser updated = systemService.updateUser(id, user);
        return ApiResponse.success(updated);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/user/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        log.info("删除用户: id={}", id);
        systemService.deleteUser(id);
        return ApiResponse.success();
    }

    @Operation(summary = "为用户分配角色")
    @PutMapping("/user/{id}/roles")
    public ApiResponse<Void> assignUserRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        log.info("分配用户角色: userId={}, roleIds={}", id, roleIds);
        systemService.assignRoles(id, roleIds);
        return ApiResponse.success();
    }

    @Operation(summary = "查询用户角色")
    @GetMapping("/user/{id}/roles")
    public ApiResponse<List<SysRole>> getUserRoles(@PathVariable Long id) {
        log.info("查询用户角色: userId={}", id);
        List<SysRole> roles = systemService.getUserRoles(id);
        return ApiResponse.success(roles);
    }

    @Operation(summary = "查询用户权限")
    @GetMapping("/user/{id}/permissions")
    public ApiResponse<List<SysPermission>> getUserPermissions(@PathVariable Long id) {
        log.info("查询用户权限: userId={}", id);
        List<SysPermission> permissions = systemService.getUserPermissions(id);
        return ApiResponse.success(permissions);
    }

    // ========== 角色管理 ==========

    @Operation(summary = "分页查询角色")
    @GetMapping("/role/page")
    public ApiResponse<PageResult<SysRole>> rolePage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId) {
        log.info("分页查询角色: page={}, size={}, orgId={}", page, size, orgId);
        PageResult<SysRole> result = PageResult.of(systemService.pageRoles(page, size, orgId));
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建角色")
    @PostMapping("/role")
    public ApiResponse<SysRole> createRole(@RequestBody @Valid SysRole role) {
        log.info("创建角色: roleName={}", role.getRoleName());
        SysRole created = systemService.createRole(role);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/role/{id}")
    public ApiResponse<SysRole> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        log.info("更新角色: id={}", id);
        SysRole updated = systemService.updateRole(id, role);
        return ApiResponse.success(updated);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/role/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        log.info("删除角色: id={}", id);
        systemService.deleteRole(id);
        return ApiResponse.success();
    }

    @Operation(summary = "为角色分配权限")
    @PutMapping("/role/{id}/permissions")
    public ApiResponse<Void> assignRolePermissions(@PathVariable Long id, @RequestBody List<Long> permIds) {
        log.info("分配角色权限: roleId={}, permIds={}", id, permIds);
        systemService.assignPermissions(id, permIds);
        return ApiResponse.success();
    }

    // ========== 权限管理 ==========

    @Operation(summary = "查询所有权限")
    @GetMapping("/permission/list")
    public ApiResponse<List<SysPermission>> permissionList() {
        log.info("查询所有权限");
        List<SysPermission> permissions = systemService.getAllPermissions();
        return ApiResponse.success(permissions);
    }

    // ========== 审计日志 ==========

    @Operation(summary = "分页查询审计日志")
    @GetMapping("/audit-log/page")
    public ApiResponse<PageResult<SysAuditLog>> auditLogPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("分页查询审计日志: page={}, size={}, orgId={}, userId={}, module={}, startTime={}, endTime={}",
                page, size, orgId, userId, module, startTime, endTime);
        LocalDateTime start = parseDateTime(startTime, true);
        LocalDateTime end = parseDateTime(endTime, false);
        PageResult<SysAuditLog> result = PageResult.of(
                systemService.pageAuditLogs(page, size, orgId, userId, module, start, end));
        return ApiResponse.success(result);
    }

    // ========== 组织管理 ==========

    @Operation(summary = "分页查询组织")
    @GetMapping("/org/page")
    public ApiResponse<PageResult<OrgOrganization>> orgPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("分页查询组织: page={}, size={}", page, size);
        PageResult<OrgOrganization> result = PageResult.of(systemService.pageOrgs(page, size));
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建组织")
    @PostMapping("/org")
    public ApiResponse<OrgOrganization> createOrg(@RequestBody OrgOrganization org) {
        log.info("创建组织: orgName={}", org.getOrgName());
        OrgOrganization created = systemService.createOrg(org);
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新组织")
    @PutMapping("/org/{id}")
    public ApiResponse<OrgOrganization> updateOrg(@PathVariable Long id, @RequestBody OrgOrganization org) {
        log.info("更新组织: id={}", id);
        OrgOrganization updated = systemService.updateOrg(id, org);
        return ApiResponse.success(updated);
    }

    @Operation(summary = "删除组织")
    @DeleteMapping("/org/{id}")
    public ApiResponse<Void> deleteOrg(@PathVariable Long id) {
        log.info("删除组织: id={}", id);
        systemService.deleteOrg(id);
        return ApiResponse.success();
    }

    // ========== 账户管理 ==========

    @Operation(summary = "分页查询账户")
    @GetMapping("/account/page")
    public ApiResponse<PageResult<OrgAccount>> accountPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ledgerId,
            @RequestParam(required = false) Long orgId) {
        log.info("分页查询账户: page={}, size={}, ledgerId={}, orgId={}", page, size, ledgerId, orgId);
        PageResult<OrgAccount> result = PageResult.of(systemService.pageAccounts(page, size, ledgerId, orgId));
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建账户")
    @PostMapping("/account")
    public ApiResponse<OrgAccount> createAccount(@RequestBody OrgAccount account) {
        log.info("创建账户: accountName={}", account.getAccountName());
        OrgAccount created = systemService.createAccount(account);
        return ApiResponse.success(created);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 解析日期时间字符串，支持 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss 格式。
     *
     * @param dateTimeStr 日期时间字符串，可能为null
     * @param startOfDay  true表示取当天00:00:00，false表示取当天23:59:59
     * @return LocalDateTime 或 null
     */
    private LocalDateTime parseDateTime(String dateTimeStr, boolean startOfDay) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            if (dateTimeStr.length() <= 10) {
                LocalDate date = LocalDate.parse(dateTimeStr.trim());
                return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            }
            return LocalDateTime.parse(dateTimeStr.trim().replace(" ", "T"));
        } catch (Exception e) {
            log.warn("日期时间解析失败: {}", dateTimeStr, e);
            return null;
        }
    }
}
