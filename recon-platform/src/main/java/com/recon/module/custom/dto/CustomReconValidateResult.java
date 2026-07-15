package com.recon.module.custom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 自定义对账方案预检结果
 */
@Data
@Builder
public class CustomReconValidateResult {

    private boolean valid;

    private List<String> errors;

    private List<String> warnings;

    private Map<String, Object> sourceAInfo;

    private List<Map<String, Object>> sourceBInfoList;

    private int activeRuleCount;

    private int pendingRecordA;

    private int pendingRecordB;
}
