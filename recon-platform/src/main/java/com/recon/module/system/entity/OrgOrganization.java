package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 组织
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_organization")
public class OrgOrganization extends BaseEntity {

    private Long parentId;

    private String orgName;

    private String orgCode;

    private String orgType;

    private String contactName;

    private String contactPhone;

    private String address;

    private String status;

    private Integer sortOrder;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String extraInfo;

    private Long createdBy;

    private Long updatedBy;
}
