package com.recon.common.enums;

/**
 * 匹配类型枚举
 */
public enum MatchType {

    EXACT("EXACT", "精准匹配"),
    RULE("RULE", "规则匹配"),
    AI_SEMANTIC("AI_SEMANTIC", "AI语义匹配"),
    AI_SPLIT("AI_SPLIT", "拆单匹配"),
    MANUAL("MANUAL", "手工匹配");

    private final String code;
    private final String description;

    MatchType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MatchType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
