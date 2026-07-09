package com.recon.common.enums;

/**
 * 规则类型枚举
 */
public enum RuleType {

    EXACT_MATCH("EXACT_MATCH", "精确匹配"),
    RULE_MATCH("RULE_MATCH", "规则匹配"),
    AI_MATCH("AI_MATCH", "AI匹配"),
    TEMPLATE_MATCH("TEMPLATE_MATCH", "模板匹配");

    private final String code;
    private final String description;

    RuleType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RuleType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RuleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
