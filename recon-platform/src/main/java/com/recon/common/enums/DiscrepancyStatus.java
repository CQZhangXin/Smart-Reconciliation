package com.recon.common.enums;

/**
 * 差异处理状态枚举
 */
public enum DiscrepancyStatus {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RESOLVED("RESOLVED", "已解决"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String description;

    DiscrepancyStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DiscrepancyStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DiscrepancyStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
