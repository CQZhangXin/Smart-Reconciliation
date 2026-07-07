package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 系统许可证
 *
 * @author recon-platform
 */
@Data
@Accessors(chain = true)
@TableName("sys_license")
public class SysLicense {

    private Long id;

    private Long orgId;

    /** 加密的许可证数据 */
    private String licenseData;

    /** 授权组织名称 */
    private String orgName;

    /** 到期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    /** 最大用户数 */
    private Integer maxUsers;

    /** 功能授权列表 (JSON) */
    private String features;

    /** 机器指纹 */
    private String machineId;

    /** 状态: ACTIVE/EXPIRED/REVOKED */
    private String status;

    /** 激活时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
