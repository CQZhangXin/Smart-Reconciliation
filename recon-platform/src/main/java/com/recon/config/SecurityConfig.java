package com.recon.config;

import com.recon.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    /**
     * CORS allowed origins, loaded from application.yml.
     * In production, this should be set to specific frontend origins (not "*").
     */
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /**
     * 安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO: 在生产环境中，应对 /api/v1/auth/login 实施速率限制
        //       可通过 Nginx limit_req 或 Spring Cloud Gateway RequestRateLimiter 实现
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 本服务为无状态 JWT API，不依赖 Session，因此无需 CSRF 保护
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())                                    // X-Content-Type-Options: nosniff
                .frameOptions(frameOptions -> frameOptions.deny())                                // X-Frame-Options: DENY
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))  // X-XSS-Protection: 1; mode=block
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                // TODO: 生产环境中对 /api/v1/auth/login 实施速率限制（Nginx limit_req 或 Spring Cloud Gateway）
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/open/health").permitAll()
                .requestMatchers("/api/v1/license/status").permitAll()
                // Swagger/Knife4j
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/doc.html", "/webjars/**").permitAll()
                // Actuator
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
                // 静态资源
                .requestMatchers(HttpMethod.GET, "/static/**", "/*.html", "/*.css", "/*.js").permitAll()
                // 其他接口需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT 认证过滤器
     * - 无 Token：放行，由 Spring Security 对受保护端点返回 401 "Authentication required"
     * - 有 Token 但无效/过期：立即返回 401 "Invalid or expired token"
     * - 有 Token 且有效：设置认证上下文并放行
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String token = extractToken(request);

                if (!StringUtils.hasText(token)) {
                    // 无 Token：放行请求
                    //   公共端点（如 login、health）会正常响应
                    //   受保护端点会由 Spring Security 返回 401，Message 中为 "Authentication required"
                    filterChain.doFilter(request, response);
                    return;
                }

                if (!jwtUtil.validateToken(token)) {
                    // Token 无效或已过期：立即返回 401
                    log.warn("Invalid or expired JWT token received from request: {}", request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Invalid or expired token\"}");
                    return;
                }

                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                Long orgId = jwtUtil.getOrgIdFromToken(token);

                // 设置认证信息到 SecurityContext
                JwtAuthenticationToken authToken = new JwtAuthenticationToken(
                        userId, username, orgId, List.of());
                authToken.setAuthenticated(true);
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().setAuthentication(authToken);

                filterChain.doFilter(request, response);
            }
        };
    }

    /**
     * CORS 配置 -- 基于 application.yml 中 cors.allowed-origins 的值
     * 注意：当 allowCredentials=true 时，allowedOrigins 不能为 "*"（违反 CORS 规范），
     * 必须指定具体的 origins。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        boolean isWildcard = origins.contains("*");

        CorsConfiguration config = new CorsConfiguration();
        if (isWildcard && origins.size() == 1) {
            // 开发环境：使用 allowedOriginPatterns 配合通配符，但不允许 credentials
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(false);
        } else {
            // 生产环境：指定具体 origins，可以安全地开启 credentials
            config.setAllowedOrigins(origins);
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Trace-Id"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 从请求头提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
