package com.recon.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 许可证工具类 — AES-256-GCM 加密/解密、许可证生成与校验
 *
 * @author recon-platform
 */
@Slf4j
public class LicenseUtil {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int AES_KEY_LENGTH = 32; // 256 bits

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private LicenseUtil() {
        // 工具类不允许实例化
    }

    /**
     * 从密钥种子派生 AES-256 密钥
     */
    public static byte[] deriveKey(String secretSeed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secretSeed.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("派生密钥失败", e);
        }
    }

    /**
     * 使用 AES-256-GCM 加密许可证数据
     *
     * @param payload    许可证载荷 (Map)
     * @param secretSeed 密钥种子
     * @return Base64 编码的加密数据
     */
    public static String encrypt(Map<String, Object> payload, String secretSeed) {
        try {
            byte[] key = deriveKey(secretSeed);
            String plainText = OBJECT_MAPPER.writeValueAsString(payload);

            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将 IV 附加到密文前面
            byte[] combined = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("许可证加密失败", e);
            throw new RuntimeException("许可证加密失败", e);
        }
    }

    /**
     * 使用 AES-256-GCM 解密许可证数据
     *
     * @param encrypted  Base64 编码的加密数据
     * @param secretSeed 密钥种子
     * @return 许可证载荷 Map
     */
    public static Map<String, Object> decrypt(String encrypted, String secretSeed) {
        try {
            byte[] key = deriveKey(secretSeed);
            byte[] combined = Base64.getDecoder().decode(encrypted);

            if (combined.length < GCM_IV_LENGTH + 1) {
                throw new IllegalArgumentException("无效的许可证数据");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            String json = new String(plainText, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("许可证解密失败: {}", e.getMessage());
            throw new RuntimeException("许可证解密失败，许可证文件可能已损坏或密钥不匹配", e);
        }
    }

    /**
     * 生成许可证文件内容 (.lic)
     *
     * @param orgName    授权组织名称
     * @param orgCode    组织编码
     * @param maxUsers   最大用户数
     * @param expireDate 到期日期
     * @param features   功能列表
     * @param machineId  机器指纹（可选）
     * @param secretSeed 密钥种子
     * @return Base64 编码的加密许可证字符串
     */
    public static String generateLicense(String orgName, String orgCode, int maxUsers,
                                          LocalDate expireDate, List<String> features,
                                          String machineId, String secretSeed) {
        Map<String, Object> payload = Map.of(
                "orgName", orgName,
                "orgCode", orgCode,
                "maxUsers", maxUsers,
                "expireDate", expireDate.toString(),
                "features", features,
                "machineId", machineId != null ? machineId : "",
                "issuedAt", LocalDate.now().toString()
        );
        return encrypt(payload, secretSeed);
    }

    /**
     * 检查许可证是否过期
     */
    public static boolean isExpired(Map<String, Object> payload) {
        try {
            Object expireDateObj = payload.get("expireDate");
            if (expireDateObj == null) {
                return true;
            }
            LocalDate expireDate = LocalDate.parse(expireDateObj.toString());
            return LocalDate.now().isAfter(expireDate);
        } catch (Exception e) {
            log.error("解析到期日期失败", e);
            return true;
        }
    }

    /**
     * 检查许可证即将过期 (30天内)
     */
    public static boolean isExpiringSoon(Map<String, Object> payload) {
        try {
            Object expireDateObj = payload.get("expireDate");
            if (expireDateObj == null) {
                return false;
            }
            LocalDate expireDate = LocalDate.parse(expireDateObj.toString());
            return !isExpired(payload) && LocalDate.now().plusDays(30).isAfter(expireDate);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查功能是否已授权
     */
    @SuppressWarnings("unchecked")
    public static boolean isFeatureEnabled(Map<String, Object> payload, String feature) {
        try {
            Object featuresObj = payload.get("features");
            if (featuresObj instanceof List<?> features) {
                if (features.isEmpty()) {
                    return true; // 空列表 = 全部功能
                }
                return features.stream().anyMatch(f ->
                        "ALL".equalsIgnoreCase(f.toString())
                                || feature.equalsIgnoreCase(f.toString()));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取许可证过期剩余天数
     */
    public static long getRemainingDays(Map<String, Object> payload) {
        try {
            Object expireDateObj = payload.get("expireDate");
            if (expireDateObj == null) {
                return 0;
            }
            LocalDate expireDate = LocalDate.parse(expireDateObj.toString());
            return LocalDate.now().until(expireDate).getDays();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 从载荷中获取字符串值
     */
    public static String getString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从载荷中获取整数值
     */
    public static int getInt(Map<String, Object> payload, String key, int defaultValue) {
        Object value = payload.get(key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
