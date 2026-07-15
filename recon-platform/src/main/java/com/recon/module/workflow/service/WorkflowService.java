package com.recon.module.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recon.ai.dto.ProcessDefNLResult;
import com.recon.ai.service.AIService;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.system.entity.SysNotification;
import com.recon.module.system.repository.SysNotificationMapper;
import com.recon.module.workflow.entity.WfApprovalRecord;
import com.recon.module.workflow.entity.WfProcessDefinition;
import com.recon.module.workflow.dto.NLWorkflowParseResult;
import com.recon.module.workflow.repository.WfApprovalRecordMapper;
import com.recon.module.workflow.repository.WfProcessDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 工作流服务 — 流程定义、审批、通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WfProcessDefinitionMapper processDefMapper;
    private final WfApprovalRecordMapper approvalMapper;
    private final SysNotificationMapper notificationMapper;
    private final AIService aiService;

    // ============================================================
    // Process Definition
    // ============================================================

    /**
     * 创建流程定义 — 默认版本1, 草稿状态
     */
    public WfProcessDefinition createProcessDef(WfProcessDefinition pd) {
        if (pd.getVersion() == null) {
            pd.setVersion(1);
        }
        if (pd.getStatus() == null || pd.getStatus().isBlank()) {
            pd.setStatus("DRAFT");
        }
        processDefMapper.insert(pd);
        return pd;
    }

    /**
     * 发布流程定义
     */
    public WfProcessDefinition publishProcessDef(Long id) {
        WfProcessDefinition def = processDefMapper.selectById(id);
        if (def == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在");
        }
        def.setStatus("PUBLISHED");
        processDefMapper.updateById(def);
        return def;
    }

    /**
     * 分页查询流程定义
     */
    public IPage<WfProcessDefinition> pageProcessDefs(int page, int size, Long orgId) {
        Page<WfProcessDefinition> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(WfProcessDefinition::getOrgId, orgId);
        }
        wrapper.orderByDesc(WfProcessDefinition::getCreatedAt);
        return processDefMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 从自然语言解析流程定义
     *
     * @param description 用户自然语言描述
     * @return NLWorkflowParseResult 解析结果
     */
    public NLWorkflowParseResult parseFromNL(String description) {
        ProcessDefNLResult nlResult = aiService.generateProcessDefFromNL(description);

        String warning = null;
        if (nlResult.getSteps() == null || nlResult.getSteps().isEmpty()) {
            warning = "AI 未能解析出审批步骤，请手动配置";
        }

        return NLWorkflowParseResult.builder()
                .definition(nlResult)
                .warning(warning)
                .build();
    }

    // ============================================================
    // Approval
    // ============================================================

    /**
     * 提交审批 — 创建审批记录, 状态PENDING
     */
    public WfApprovalRecord submitApproval(Long orgId, String businessType,
                                            Long businessId, Long processDefId) {
        // 校验流程定义是否存在且已发布
        WfProcessDefinition processDef = processDefMapper.selectById(processDefId);
        if (processDef == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "流程定义不存在: " + processDefId);
        }
        if (!"PUBLISHED".equals(processDef.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "流程定义未发布，当前状态: " + processDef.getStatus());
        }

        WfApprovalRecord record = new WfApprovalRecord()
                .setOrgId(orgId)
                .setProcessDefId(processDefId)
                .setBusinessType(businessType)
                .setBusinessId(businessId)
                .setStatus("PENDING")
                .setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(record);
        return record;
    }

    /**
     * 审批操作 — 通过或驳回
     */
    @Transactional
    public WfApprovalRecord approve(Long approvalId, Long approverId,
                                     String approverName, String action,
                                     String comment) {
        WfApprovalRecord record = approvalMapper.selectById(approvalId);
        if (record == null) {
            throw new BusinessException(ResultCode.APPROVAL_NOT_FOUND);
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException(ResultCode.APPROVAL_ERROR,
                    "当前审批状态不允许操作: " + record.getStatus());
        }

        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setAction(action);
        record.setComment(comment);
        record.setApprovedAt(LocalDateTime.now());

        if ("APPROVE".equalsIgnoreCase(action)) {
            record.setStatus("APPROVED");
        } else if ("REJECT".equalsIgnoreCase(action)) {
            record.setStatus("REJECTED");
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "不支持的操作: " + action + ", 仅支持 APPROVE / REJECT");
        }

        approvalMapper.updateById(record);
        return record;
    }

    /**
     * 分页查询我的待审/已审
     */
    public IPage<WfApprovalRecord> pageMyApprovals(int page, int size,
                                                    Long approverId, String status) {
        Page<WfApprovalRecord> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<WfApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (approverId != null) {
            wrapper.eq(WfApprovalRecord::getApproverId, approverId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(WfApprovalRecord::getStatus, status);
        }
        wrapper.orderByDesc(WfApprovalRecord::getCreatedAt);
        return approvalMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 分页查询所有审批记录 — 支持业务类型和状态筛选
     */
    public IPage<WfApprovalRecord> pageApprovals(int page, int size, Long orgId,
                                                  String businessType, String status) {
        Page<WfApprovalRecord> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<WfApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(WfApprovalRecord::getOrgId, orgId);
        }
        if (businessType != null && !businessType.isBlank()) {
            wrapper.eq(WfApprovalRecord::getBusinessType, businessType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(WfApprovalRecord::getStatus, status);
        }
        wrapper.orderByDesc(WfApprovalRecord::getCreatedAt);
        return approvalMapper.selectPage(pageObj, wrapper);
    }

    // ============================================================
    // Notification
    // ============================================================

    /**
     * 发送通知 — 创建系统通知记录
     */
    public void sendNotification(Long orgId, Long userId, String notifyType,
                                  String title, String content, String channel,
                                  String relatedType, Long relatedId) {
        SysNotification notification = new SysNotification()
                .setOrgId(orgId)
                .setUserId(userId)
                .setNotifyType(notifyType)
                .setTitle(title)
                .setContent(content)
                .setChannel(channel)
                .setIsRead(0)
                .setRelatedType(relatedType)
                .setRelatedId(relatedId)
                .setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
