package com.recon.common.enums;

/**
 * 差异分类枚举
 */
public enum DiscrepancyCategory {

    TIME_DIFF("TIME_DIFF", "时间差"),
    FEE_DIFF("FEE_DIFF", "手续费差异"),
    EXCHANGE_DIFF("EXCHANGE_DIFF", "汇率差异"),
    DATA_ENTRY_ERROR("DATA_ENTRY_ERROR", "人为录入错误"),
    UNREACHED("UNREACHED", "未达账项"),
    DUPLICATE("DUPLICATE", "重复记账"),
    OTHER_SIDE_UNRECORDED("OTHER_SIDE_UNRECORDED", "对方未入账"),
    UNKNOWN("UNKNOWN", "未知差异");

    private final String code;
    private final String description;

    DiscrepancyCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DiscrepancyCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DiscrepancyCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
