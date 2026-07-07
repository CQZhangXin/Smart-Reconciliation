package com.recon.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 账套
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_ledger")
public class OrgLedger extends BaseEntity {

    private String ledgerName;

    private String ledgerCode;

    private String currency;

    private String accountingStd;

    private LocalDate fiscalYearStart;

    private String status;
}
