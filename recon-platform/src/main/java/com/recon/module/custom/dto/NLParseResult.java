package com.recon.module.custom.dto;

import com.recon.module.custom.entity.CustomReconDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 自然语言解析对账方案的返回结果 — 含已解析到 ID 的定义 + 未匹配项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NLParseResult {

    /** 解析后的方案定义（id = null，未持久化） */
    private CustomReconDefinition definition;

    /** 未能匹配到数据库的数据源名称 */
    @Builder.Default
    private List<String> unresolvedSources = new ArrayList<>();

    /** 未能匹配到数据库的规则名称 */
    @Builder.Default
    private List<String> unresolvedRules = new ArrayList<>();

    /** AI 解析说明，供用户确认时参考 */
    private String aiExplanation;
}
