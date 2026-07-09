package com.recon.common.enums;

/**
 * 对账任务状态枚举
 */
public enum TaskStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "运行中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TaskStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
