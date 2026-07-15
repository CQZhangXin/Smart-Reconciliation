package com.recon.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 自然语言生成流程定义结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefNLResult {

    private String processName;

    private String processKey;

    private String description;

    /** 审批步骤列表 */
    private List<ProcessStepItem> steps;

    /** AI 解析思路说明 */
    private String aiExplanation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessStepItem {
        private Integer order;
        private String name;
        private String approverRole;
    }
}
