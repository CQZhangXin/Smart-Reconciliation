package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AI语义匹配请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreRequest {
    /** 记录A的标准化文本 */
    private String recordAText;
    /** 记录B的标准化文本 */
    private String recordBText;
    /** A方金额 */
    private BigDecimal amountA;
    /** B方金额 */
    private BigDecimal amountB;
    /** A方日期 */
    private LocalDate dateA;
    /** B方日期 */
    private LocalDate dateB;
    /** A方交易方名称 */
    private String partyA;
    /** B方交易方名称 */
    private String partyB;
    /** A方交易摘要 */
    private String descriptionA;
    /** B方交易摘要 */
    private String descriptionB;
    /** A方流水号 */
    private String refA;
    /** B方流水号 */
    private String refB;
    /** 币种 */
    private String currency;
    /** 账户类型 */
    private String accountType;
}
