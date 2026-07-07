package com.recon.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 自然语言查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NLQueryResult {
    /** 用户原始问题 */
    private String question;
    /** 生成的SQL */
    private String generatedSql;
    /** 查询结果(行数据) */
    private List<Map<String, Object>> data;
    /** AI润色后的自然语言回答 */
    private String answer;
    /** 提取的实体 */
    private Map<String, Object> extractedEntities;
    /** 查询耗时(毫秒) */
    private Long queryTimeMs;
}
