package com.recon.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collection;
import java.util.Collections;

/**
 * JWT认证令牌 — 承载用户身份信息
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final String username;
    private final Long orgId;

    public JwtAuthenticationToken(Long userId, String username, Long orgId,
                                   Collection authorities) {
        super(authorities);
        this.userId = userId;
        this.username = username;
        this.orgId = orgId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.userId;
    }

    @Override
    public String getName() {
        return this.username;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    /**
     * 创建匿名令牌
     */
    public static JwtAuthenticationToken anonymous() {
        return new JwtAuthenticationToken(null, "anonymous", null, Collections.emptyList());
    }
}
