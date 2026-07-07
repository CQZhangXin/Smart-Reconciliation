package com.recon.common.utils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 许可证生成器 - 命令行工具
 *
 * 用于生成 .lic 许可证文件，分发给客户激活系统。
 *
 * 使用方法:
 *   java com.recon.common.utils.LicenseGenerator
 *
 * 或通过代码调用:
 *   LicenseGenerator.generateFile(...)
 *
 * @author recon-platform
 */
public class LicenseGenerator {

    /** 默认密钥种子 (与 application.yml 中 license.secret 一致) */
    private static final String SECRET = "recon-platform-license-default-secret-key";

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════");
        System.out.println("  智能 AI 对账平台 - 许可证生成器");
        System.out.println("═══════════════════════════════════════");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.print("组织名称: ");
            String orgName = scanner.nextLine().trim();

            System.out.print("组织编码: ");
            String orgCode = scanner.nextLine().trim();

            System.out.print("最大用户数 (0=无限制): ");
            int maxUsers = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("到期日期 (yyyy-MM-dd, 如 2027-12-31): ");
            String expireDateStr = scanner.nextLine().trim();
            LocalDate expireDate = LocalDate.parse(expireDateStr);

            System.out.print("功能列表 (逗号分隔, ALL=全部): ");
            System.out.println("[可选: ALL, BANK_RECON, THIRD_PAYMENT, AR_AP_RECON, CROSS_SYSTEM, AI_MATCH, NL_QUERY, AI_ANALYSIS, WORKFLOW, OPEN_API]");
            String featuresStr = scanner.nextLine().trim();
            List<String> features;
            if (featuresStr.isEmpty()) {
                features = List.of("ALL");
            } else {
                features = Arrays.stream(featuresStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            System.out.print("机器指纹 (可选, 直接回车跳过): ");
            String machineId = scanner.nextLine().trim();
            if (machineId.isEmpty()) {
                machineId = null;
            }

            // 生成许可证
            String license = LicenseUtil.generateLicense(
                    orgName, orgCode, maxUsers, expireDate, features, machineId, SECRET);

            System.out.println();
            System.out.println("═══════════════════════════════════════");
            System.out.println("  许可证已生成 (复制以下内容保存为 .lic 文件):");
            System.out.println("═══════════════════════════════════════");
            System.out.println(license);
            System.out.println("═══════════════════════════════════════");
            System.out.println();
            System.out.println("授权信息:");
            System.out.println("  组织名称: " + orgName);
            System.out.println("  组织编码: " + orgCode);
            System.out.println("  最大用户: " + (maxUsers > 0 ? String.valueOf(maxUsers) : "无限制"));
            System.out.println("  到期日期: " + expireDate);
            System.out.println("  功能授权: " + features);
            if (machineId != null) {
                System.out.println("  机器指纹: " + machineId);
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 通过代码生成许可证文件内容
     */
    public static String generateFile(String orgName, String orgCode, int maxUsers,
                                       LocalDate expireDate, List<String> features,
                                       String machineId) {
        return LicenseUtil.generateLicense(orgName, orgCode, maxUsers, expireDate,
                features, machineId, SECRET);
    }

    /**
     * 使用自定义密钥生成许可证
     */
    public static String generateFileWithSecret(String orgName, String orgCode, int maxUsers,
                                                 LocalDate expireDate, List<String> features,
                                                 String machineId, String secret) {
        return LicenseUtil.generateLicense(orgName, orgCode, maxUsers, expireDate,
                features, machineId, secret);
    }
}
