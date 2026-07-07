package com.recon.module.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账规则执行日志
 */
@Data
@Accessors(chain = true)
@TableName("recon_rule_exec_log")
public class ReconRuleExecLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Long taskId;

    private Integer inputCount;

    private Integer matchedCount;

    private Integer unmatchedCount;

    private Long durationMs;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;
}
