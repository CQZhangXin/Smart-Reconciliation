package com.recon.module.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recon.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 工作流流程定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("wf_process_definition")
public class WfProcessDefinition extends BaseEntity {

    private String processName;

    private String processKey;

    private String processType;

    private String bpmnXml;

    private Integer version;

    private String status;

    private Long createdBy;
}
