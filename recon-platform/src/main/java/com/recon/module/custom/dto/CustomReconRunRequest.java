package com.recon.module.custom.dto;

import lombok.Data;

import java.util.List;

/**
 * 自定义对账执行请求
 */
@Data
public class CustomReconRunRequest {

    /** 对账期间，如 2026-07；为空则用方案默认期间 */
    private String reconPeriod;

    /** 期间开始日期 yyyy-MM-dd */
    private String periodStart;

    /** 期间结束日期 yyyy-MM-dd */
    private String periodEnd;

    /** 是否异步执行，默认 true */
    private Boolean async = true;

    /** 临时覆盖规则ID；为空则用方案配置 */
    private List<Long> ruleIds;

    /** 临时覆盖 B 方数据源；为空则用方案配置 */
    private List<Long> sourceBIds;
}
