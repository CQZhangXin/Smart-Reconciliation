package com.recon.module.custom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 自定义对账执行结果
 */
@Data
@Builder
public class CustomReconRunResult {

    private Long definitionId;

    private String defName;

    /** 主任务（A vs 第一个B） */
    private Long primaryTaskId;

    /** 多源场景下创建的全部任务ID */
    private List<Long> taskIds;

    private boolean async;

    private String message;
}
