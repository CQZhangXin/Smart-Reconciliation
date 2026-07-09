package com.recon.common.enums;

/**
 * 匹配状态枚举
 */
public enum MatchStatus {

    AUTO_CONFIRMED("AUTO_CONFIRMED", "自动确认"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    MANUAL_CONFIRMED("MANUAL_CONFIRMED", "人工确认"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    MatchStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MatchStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
