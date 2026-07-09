package com.recon.common.enums;

/**
 * 风险等级枚举
 */
public enum RiskLevel {

    LOW("LOW", "低", "#67C23A"),
    MEDIUM("MEDIUM", "中", "#E6A23C"),
    HIGH("HIGH", "高", "#F56C6C"),
    CRITICAL("CRITICAL", "严重", "#FF0000");

    private final String code;
    private final String description;
    private final String color;

    RiskLevel(String code, String description, String color) {
        this.code = code;
        this.description = description;
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public static RiskLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
