package com.recon.common.enums;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误 (1000-1999)
    DATA_SOURCE_ERROR(1001, "数据源操作失败"),
    DATA_SOURCE_CONNECT_FAILED(1002, "数据源连接失败"),
    DATA_SYNC_FAILED(1003, "数据同步失败"),

    RULE_CONFIG_ERROR(1101, "规则配置错误"),
    RULE_CONFLICT(1102, "规则冲突"),

    RECON_TASK_FAILED(1201, "对账任务执行失败"),
    RECON_TASK_RUNNING(1202, "对账任务正在运行中"),

    MATCH_ENGINE_ERROR(1301, "匹配引擎错误"),
    AI_SERVICE_ERROR(1302, "AI服务调用失败"),

    DISCREPANCY_NOT_FOUND(1401, "差异记录不存在"),
    DISCREPANCY_ALREADY_RESOLVED(1402, "差异已处理"),

    APPROVAL_ERROR(1501, "审批操作失败"),
    APPROVAL_NOT_FOUND(1502, "审批记录不存在"),

    USER_NOT_FOUND(1601, "用户不存在"),
    USER_PASSWORD_ERROR(1602, "密码错误"),
    USER_LOCKED(1603, "用户已锁定"),
    USER_EXISTS(1604, "用户已存在"),

    TENANT_NOT_FOUND(1701, "租户不存在"),
    TENANT_EXPIRED(1702, "租户已过期"),

    // 许可证相关 (1900-1999)
    LICENSE_NOT_FOUND(1901, "未找到许可证，请先激活系统"),
    LICENSE_EXPIRED(1902, "许可证已过期，请续期后继续使用"),
    LICENSE_INVALID(1903, "许可证无效或已损坏"),
    LICENSE_USER_EXCEEDED(1904, "用户数已达许可证上限"),
    LICENSE_FEATURE_DENIED(1905, "当前许可证未授权此功能"),
    LICENSE_MACHINE_MISMATCH(1906, "许可证与当前机器不匹配"),
    LICENSE_EXPIRING_SOON(1907, "许可证即将过期"),

    FILE_UPLOAD_ERROR(1801, "文件上传失败"),
    FILE_FORMAT_ERROR(1802, "文件格式不支持"),
    EXPORT_ERROR(1803, "导出失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
