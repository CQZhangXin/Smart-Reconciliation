package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 系统角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;

    private String roleCode;

    private String roleType;

    private String description;

    private String status;
}
