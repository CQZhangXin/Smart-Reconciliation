package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 账户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_account")
public class OrgAccount extends BaseEntity {

    private Long ledgerId;

    private String accountCode;

    private String accountName;

    private String accountType;

    private String bankName;

    /**
     * 银行账号 — 敏感数据，序列化时屏蔽，生产环境建议对数据库存储做加密。
     */
    @JsonIgnore
    private String bankAccountNo;

    private String currency;

    private String status;
}
