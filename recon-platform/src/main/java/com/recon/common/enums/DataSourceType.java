package com.recon.common.enums;

/**
 * 数据来源类型枚举
 */
public enum DataSourceType {

    BANK_API("BANK_API", "银行API"),
    THIRD_PAYMENT("THIRD_PAYMENT", "第三方支付"),
    ERP("ERP", "ERP系统"),
    FILE_IMPORT("FILE_IMPORT", "文件导入"),
    DATABASE("DATABASE", "数据库直连"),
    HTTP_API("HTTP_API", "HTTP接口"),
    MANUAL("MANUAL", "手工录入");

    private final String code;
    private final String description;

    DataSourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataSourceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
