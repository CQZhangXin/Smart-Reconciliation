package com.recon.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     */
    public String generateToken(Long userId, String username, Long orgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ORG_ID, orgId);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return buildToken(claims, expiration);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String username, Long orgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ORG_ID, orgId);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        return buildToken(claims, refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 校验令牌
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT令牌校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Number num) {
            return num.longValue();
        }
        if (userId instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从令牌中获取组织ID
     */
    public Long getOrgIdFromToken(String token) {
        Claims claims = parseClaims(token);
        Object orgId = claims.get(CLAIM_ORG_ID);
        if (orgId instanceof Number num) {
            return num.longValue();
        }
        if (orgId instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 启动时校验JWT密钥长度 (至少32字节)
     */
    @PostConstruct
    public void validateSecretKey() {
        if (secret == null || secret.length() < 32) {
            String msg = "JWT密钥长度不足: 当前" + (secret == null ? 0 : secret.length())
                    + "字节, 要求至少32字节。请在application.yml中设置jwt.secret";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * 校验令牌是否为刷新令牌 (拒绝访问令牌冒充刷新令牌)
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
