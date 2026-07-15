package com.recon.config;

import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.common.utils.SecurityUtil;
import com.recon.module.system.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 许可证校验拦截器 — 拦截所有业务 API 请求，校验许可证有效性
 *
 * @author recon-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseInterceptor implements HandlerInterceptor {

    private final LicenseService licenseService;

    /** 跳过许可证校验的路径 */
    private static final List<String> SKIP_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/me",
            "/api/v1/auth/logout",
            "/api/v1/license",
            "/api/v1/open/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/doc.html",
            "/webjars",
            "/actuator",
            "/static"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        // 跳过不需要校验的路径
        if (isSkipPath(requestUri)) {
            return true;
        }

        // 获取当前用户组织ID
        Long orgId = SecurityUtil.getCurrentOrgId();
        if (orgId == null) {
            // 未获取到 orgId 但请求已通过认证。
            // 安全风险：系统级 API 不应在此被静默放行。
            // TODO: 生产环境应区分系统级 API（如 /api/system/health）并仅对白名单路径放行。
            log.warn("LicenseInterceptor: orgId 为空但请求已放行，URI={}", requestUri);
            return true;
        }

        try {
            licenseService.validateLicense(orgId);
        } catch (BusinessException e) {
            // 转换为 JSON 响应
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            String json = String.format(
                    "{\"code\":%d,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                    e.getCode(), e.getMessage(), System.currentTimeMillis()
            );
            response.getWriter().write(json);
            return false;
        }

        return true;
    }

    /**
     * 判断是否为跳过校验的路径
     */
    private boolean isSkipPath(String requestUri) {
        for (String skipPath : SKIP_PATHS) {
            if (requestUri.startsWith(skipPath)) {
                return true;
            }
        }
        return false;
    }
}
