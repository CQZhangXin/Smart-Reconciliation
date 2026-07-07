package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 根因分析请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootCauseRequest {
    private Long discrepancyId;
    private String category;          // AI分类结果
    private String recordAJson;       // ERP记录JSON
    private String recordBJson;       // 银行记录JSON
    private String accountType;
    private String currency;
    private String industryType;      // 行业类型
}
