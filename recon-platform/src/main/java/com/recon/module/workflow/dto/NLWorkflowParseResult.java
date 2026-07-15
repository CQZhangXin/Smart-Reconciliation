package com.recon.module.workflow.dto;

import com.recon.ai.dto.ProcessDefNLResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自然语言解析流程定义的返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NLWorkflowParseResult {

    /** 解析后的流程定义数据（未持久化） */
    private ProcessDefNLResult definition;

    /** 如有问题，给出提示 */
    private String warning;
}
