package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 差异分类请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyClassifyRequest {
    private Long recordId;
    private BigDecimal amountA;
    private BigDecimal amountB;
    private BigDecimal amountDiff;
    private String currency;
    private LocalDate dateA;
    private LocalDate dateB;
    private String descriptionA;
    private String descriptionB;
    private String partyA;
    private String partyB;
    private String transactionRefA;
    private String transactionRefB;
    private String direction;
    private String accountType;
    /** 是否跨境交易 */
    private Boolean crossBorder;
    /** 是否有手续费 */
    private Boolean hasFee;
}
