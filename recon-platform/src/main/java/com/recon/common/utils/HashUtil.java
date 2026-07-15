package com.recon.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希工具类
 */
public class HashUtil {

    private HashUtil() {
        // 工具类不允许实例化
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * SHA-256哈希
     *
     * @param input 输入字符串
     * @return 十六进制哈希字符串
     */
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }

    /**
     * MD5哈希
     *
     * @param input  输入字符串
     * @return 十六进制哈希字符串
     * @deprecated MD5 已被证实存在碰撞漏洞，不应用于安全场景 (如密码哈希、数字签名)。
     *             对于新代码请使用 {@link #sha256(String)} 或更强的算法,
     *             仅在非安全用途 (如简单校验和) 中可继续使用。
     */
    @Deprecated
    public static String md5(String input) {
        return hash(input, "MD5");
    }

    private static String hash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + "算法不可用", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
