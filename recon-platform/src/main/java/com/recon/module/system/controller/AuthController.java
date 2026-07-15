package com.recon.module.system.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.utils.JwtUtil;
import com.recon.module.system.entity.SysPermission;
import com.recon.module.system.entity.SysRole;
import com.recon.module.system.entity.SysUser;
import com.recon.module.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证授权控制器
 *
 * @author recon-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证授权")
public class AuthController {

    private final SystemService systemService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        log.info("用户登录: username={}", username);

        // 查找用户
        SysUser user = systemService.getByUsername(username);
        if (user == null) {
            return ApiResponse.unauthorized("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ApiResponse.unauthorized("用户名或密码错误");
        }

        // 检查账号状态
        if (!"ACTIVE".equals(user.getStatus())) {
            return ApiResponse.forbidden("账号已停用");
        }

        // 生成令牌
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getOrgId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getOrgId());

        // 更新最后登录信息
        String clientIp = getClientIp(request);
        systemService.updateLoginInfo(user.getId(), LocalDateTime.now(), clientIp);

        // 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("orgId", user.getOrgId());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("userInfo", userInfo);

        log.info("用户登录成功: username={}, userId={}", username, user.getId());
        return ApiResponse.success(result);
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ApiResponse.unauthorized("token无效或已过期");
        }

        // 拒绝访问令牌冒充刷新令牌
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            return ApiResponse.unauthorized("仅允许刷新令牌进行刷新操作");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        Long orgId = jwtUtil.getOrgIdFromToken(refreshToken);

        String newAccessToken = jwtUtil.generateToken(userId, username, orgId);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        log.info("刷新令牌成功: userId={}", userId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.unauthorized("未登录");
        }

        com.recon.config.JwtAuthenticationToken jwtAuth =
                (com.recon.config.JwtAuthenticationToken) authentication;
        Long userId = jwtAuth.getUserId();

        SysUser user = systemService.getUserById(userId);
        if (user == null) {
            return ApiResponse.unauthorized("用户不存在");
        }

        List<SysRole> roles = systemService.getUserRoles(userId);
        List<SysPermission> permissions = systemService.getUserPermissions(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("orgId", user.getOrgId());
        result.put("status", user.getStatus());
        result.put("roles", roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        result.put("permissions", permissions.stream().map(SysPermission::getPermCode).collect(Collectors.toList()));

        return ApiResponse.success(result);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        log.info("用户退出登录");
        return ApiResponse.success("已退出");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
