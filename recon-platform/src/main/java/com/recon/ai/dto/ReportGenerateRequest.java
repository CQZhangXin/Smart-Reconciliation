package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 报告生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerateRequest {
    private Long orgId;
    private Long taskId;
    private String reportType;    // DAILY/MONTHLY/QUARTERLY
    private String period;        // 报告期间
    private Long matchCount;
    private Long discrepancyCount;
    private String matchRate;
    private String trendData;     // 趋势数据JSON
    private String language;      // zh_CN/en_US
}
