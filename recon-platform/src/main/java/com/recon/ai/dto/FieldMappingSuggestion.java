package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 字段映射建议
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingSuggestion {
    private String sourceField;
    private String targetField;
    private Integer confidence;     // 0-100
    private String fieldType;       // STRING/DECIMAL/DATE/INTEGER
    private String formatHint;      // 格式提示: yyyyMMdd / "1,000.00"
    private String explanation;
}
