package com.recon.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.common.utils.LicenseUtil;
import com.recon.module.system.entity.SysLicense;
import com.recon.module.system.entity.SysUser;
import com.recon.module.system.repository.SysLicenseMapper;
import com.recon.module.system.repository.SysUserMapper;
import com.recon.module.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 许可证服务 — 激活、验证、功能检查
 *
 * @author recon-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private static final String LICENSE_CACHE_KEY = "license:%d";
    private static final int LICENSE_CACHE_TTL_MINUTES = 10;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final SysLicenseMapper licenseMapper;
    private final SysUserMapper sysUserMapper;

    /** 许可证密钥 — 无默认值，必须在 application.yml 中配置，否则启动失败 */
    @Value("${license.secret}")
    private String licenseSecret;

    @Value("${license.enabled:true}")
    private boolean licenseEnabled;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询组织当前许可证
     */
    public SysLicense getLicense(Long orgId) {
        return licenseMapper.selectOne(
                new LambdaQueryWrapper<SysLicense>()
                        .eq(SysLicense::getOrgId, orgId)
                        .orderByDesc(SysLicense::getId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 获取并解密许可证载荷
     */
    public Map<String, Object> getLicensePayload(Long orgId) {
        SysLicense sysLicense = getLicense(orgId);
        if (sysLicense == null) {
            return null;
        }
        try {
            return LicenseUtil.decrypt(sysLicense.getLicenseData(), licenseSecret);
        } catch (Exception e) {
            log.error("解密许可证失败: orgId={}", orgId, e);
            return null;
        }
    }

    /**
     * 获取缓存中的许可证载荷
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedLicensePayload(Long orgId) {
        String cacheKey = String.format(LICENSE_CACHE_KEY, orgId);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }

        Map<String, Object> payload = getLicensePayload(orgId);
        if (payload != null) {
            redisTemplate.opsForValue().set(cacheKey, payload,
                    Duration.ofMinutes(LICENSE_CACHE_TTL_MINUTES));
        }
        return payload;
    }

    /**
     * 校验许可证 (供拦截器调用)
     */
    public void validateLicense(Long orgId) {
        if (!licenseEnabled) {
            return;
        }

        Map<String, Object> payload = getCachedLicensePayload(orgId);
        if (payload == null) {
            throw new BusinessException(ResultCode.LICENSE_NOT_FOUND);
        }

        if (LicenseUtil.isExpired(payload)) {
            throw new BusinessException(ResultCode.LICENSE_EXPIRED);
        }
    }

    /**
     * 校验许可证功能
     */
    public void validateFeature(Long orgId, String feature) {
        if (!licenseEnabled) {
            return;
        }

        Map<String, Object> payload = getCachedLicensePayload(orgId);
        if (payload == null) {
            throw new BusinessException(ResultCode.LICENSE_NOT_FOUND);
        }

        if (!LicenseUtil.isFeatureEnabled(payload, feature)) {
            throw new BusinessException(ResultCode.LICENSE_FEATURE_DENIED);
        }
    }

    /**
     * 激活许可证
     */
    @Transactional
    public Map<String, Object> activate(Long orgId, String licenseData) {
        log.info("激活许可证: orgId={}", orgId);

        // 解密并验证
        Map<String, Object> payload;
        try {
            payload = LicenseUtil.decrypt(licenseData, licenseSecret);
        } catch (Exception e) {
            log.error("许可证解密失败", e);
            throw new BusinessException(ResultCode.LICENSE_INVALID);
        }

        // 检查是否过期
        if (LicenseUtil.isExpired(payload)) {
            throw new BusinessException(ResultCode.LICENSE_EXPIRED);
        }

        // 删除旧许可证
        licenseMapper.delete(
                new LambdaQueryWrapper<SysLicense>()
                        .eq(SysLicense::getOrgId, orgId)
        );

        // 创建新许可证
        SysLicense sysLicense = new SysLicense();
        sysLicense.setOrgId(orgId);
        sysLicense.setLicenseData(licenseData);
        sysLicense.setOrgName(LicenseUtil.getString(payload, "orgName"));
        sysLicense.setExpireDate(LocalDate.parse(LicenseUtil.getString(payload, "expireDate")));
        sysLicense.setMaxUsers(LicenseUtil.getInt(payload, "maxUsers", 0));
        sysLicense.setFeatures(extractFeaturesJson(payload));
        sysLicense.setMachineId(LicenseUtil.getString(payload, "machineId"));
        sysLicense.setStatus("ACTIVE");
        sysLicense.setActivatedAt(LocalDateTime.now());

        licenseMapper.insert(sysLicense);

        // 清除缓存
        String cacheKey = String.format(LICENSE_CACHE_KEY, orgId);
        redisTemplate.delete(cacheKey);

        log.info("许可证激活成功: orgId={}, expireDate={}", orgId, sysLicense.getExpireDate());

        return buildStatusResult(orgId, payload);
    }

    /**
     * 获取许可证状态
     */
    public Map<String, Object> getStatus(Long orgId) {
        if (!licenseEnabled) {
            Map<String, Object> result = new HashMap<>();
            result.put("licensed", true);
            result.put("licenseEnabled", false);
            result.put("status", "DISABLED");
            result.put("message", "许可证校验已关闭");
            return result;
        }

        SysLicense sysLicense = getLicense(orgId);
        if (sysLicense == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("licensed", false);
            result.put("licenseEnabled", true);
            result.put("status", "UNLICENSED");
            result.put("message", "未找到许可证，请激活系统");
            return result;
        }

        Map<String, Object> payload;
        try {
            payload = LicenseUtil.decrypt(sysLicense.getLicenseData(), licenseSecret);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("licensed", false);
            result.put("licenseEnabled", true);
            result.put("status", "INVALID");
            result.put("message", "许可证数据无效");
            return result;
        }

        return buildStatusResult(orgId, payload);
    }

    /**
     * 吊销许可证
     */
    @Transactional
    public void revoke(Long orgId) {
        log.info("吊销许可证: orgId={}", orgId);
        SysLicense sysLicense = getLicense(orgId);
        if (sysLicense != null) {
            sysLicense.setStatus("REVOKED");
            licenseMapper.updateById(sysLicense);
        }
        String cacheKey = String.format(LICENSE_CACHE_KEY, orgId);
        redisTemplate.delete(cacheKey);
    }

    /**
     * 校验用户数限制
     *
     * <p>TODO: 该方法存在 TOCTOU (Time-of-Check-Time-of-Use) 竞态条件风险：
     * 先查询当前用户数，再判断是否超限，在高并发创建用户场景下，
     * 多个请求可能同时通过检查从而导致超限。生产环境建议使用
     * Redis 分布式锁或数据库行级锁（如 SELECT ... FOR UPDATE）
     * 来保证原子性。</p>
     */
    @Transactional
    public void checkUserLimit(Long orgId) {
        if (!licenseEnabled) {
            return;
        }

        SysLicense sysLicense = getLicense(orgId);
        if (sysLicense == null) {
            throw new BusinessException(ResultCode.LICENSE_NOT_FOUND);
        }

        int maxUsers = sysLicense.getMaxUsers();
        if (maxUsers <= 0) {
            return; // 无限制
        }

        long currentUserCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getOrgId, orgId)
        );

        if (currentUserCount >= maxUsers) {
            throw new BusinessException(ResultCode.LICENSE_USER_EXCEEDED);
        }
    }

    /**
     * 清除许可证缓存
     */
    public void clearCache(Long orgId) {
        String cacheKey = String.format(LICENSE_CACHE_KEY, orgId);
        redisTemplate.delete(cacheKey);
    }

    // ============== 私有方法 ==============

    private String extractFeaturesJson(Map<String, Object> payload) {
        try {
            Object features = payload.get("features");
            return OBJECT_MAPPER.writeValueAsString(features);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildStatusResult(Long orgId, Map<String, Object> payload) {
        boolean expired = LicenseUtil.isExpired(payload);
        boolean expiringSoon = LicenseUtil.isExpiringSoon(payload);

        Map<String, Object> result = new HashMap<>();
        result.put("licensed", true);
        result.put("licenseEnabled", licenseEnabled);
        result.put("status", expired ? "EXPIRED" : (expiringSoon ? "EXPIRING_SOON" : "ACTIVE"));
        result.put("orgName", LicenseUtil.getString(payload, "orgName"));
        result.put("orgCode", LicenseUtil.getString(payload, "orgCode"));
        result.put("maxUsers", LicenseUtil.getInt(payload, "maxUsers", 0));
        result.put("expireDate", LicenseUtil.getString(payload, "expireDate"));
        result.put("remainingDays", LicenseUtil.getRemainingDays(payload));
        result.put("features", payload.getOrDefault("features", List.of()));
        result.put("issuedAt", LicenseUtil.getString(payload, "issuedAt"));
        result.put("message", expired ? "许可证已过期" : (expiringSoon ?
                "许可证将在 " + LicenseUtil.getRemainingDays(payload) + " 天后到期" : "许可证有效"));

        return result;
    }
}
