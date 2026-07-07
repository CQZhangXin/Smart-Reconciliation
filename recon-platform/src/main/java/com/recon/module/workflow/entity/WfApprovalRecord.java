package com.recon.module.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流审批记录
 */
@Data
@Accessors(chain = true)
@TableName(value = "wf_approval_record", autoResultMap = true)
public class WfApprovalRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long processDefId;

    private String businessType;

    private Long businessId;

    private String nodeName;

    private Long approverId;

    private String approverName;

    private String action;

    private String comment;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String attachments;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime slaDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
