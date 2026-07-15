package com.recon.common.utils;

import com.recon.config.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtil {

    private SecurityUtil() {
        // 工具类不允许实例化
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getUserId();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getName();
        }
        return authentication.getName();
    }

    /**
     * 获取当前用户组织ID
     */
    public static Long getCurrentOrgId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getOrgId();
        }
        return null;
    }

    /**
     * 检查当前用户是否拥有指定角色。
     * 注意：当前 JWT Filter 创建 Token 时 authorities 为空，
     * TODO: 修复 JWT Filter 从 token claims 或数据库加载角色后，此方法才能生效。
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
}
