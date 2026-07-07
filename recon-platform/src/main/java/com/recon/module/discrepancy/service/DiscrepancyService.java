package com.recon.module.discrepancy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recon.ai.dto.DiscrepancyClassifyRequest;
import com.recon.ai.dto.RootCauseRequest;
import com.recon.ai.dto.RootCauseResult;
import com.recon.ai.service.AIService;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.datasource.entity.RawRecord;
import com.recon.module.datasource.repository.RawRecordMapper;
import com.recon.module.discrepancy.entity.ReconAdjustment;
import com.recon.module.discrepancy.entity.ReconDiscrepancy;
import com.recon.module.discrepancy.repository.ReconAdjustmentMapper;
import com.recon.module.discrepancy.repository.ReconDiscrepancyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 差异处理服务 — 差异分类、根因分析、处理分配、解决关闭、调整单管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscrepancyService {

    private final ReconDiscrepancyMapper discrepancyMapper;
    private final ReconAdjustmentMapper adjustmentMapper;
    private final AIService aiService;
    private final RawRecordMapper rawRecordMapper;

    // ==================== 差异查询 ====================

    /**
     * 分页查询差异列表，支持多条件筛选
     */
    public IPage<ReconDiscrepancy> pageDiscrepancies(int page, int size, Long orgId, Long taskId,
                                                     String category, String riskLevel,
                                                     String status, Long handlerId) {
        LambdaQueryWrapper<ReconDiscrepancy> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(ReconDiscrepancy::getOrgId, orgId);
        }
        if (taskId != null) {
            wrapper.eq(ReconDiscrepancy::getTaskId, taskId);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(ReconDiscrepancy::getCategory, category);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            wrapper.eq(ReconDiscrepancy::getRiskLevel, riskLevel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ReconDiscrepancy::getStatus, status);
        }
        if (handlerId != null) {
            wrapper.eq(ReconDiscrepancy::getHandlerId, handlerId);
        }
        wrapper.orderByDesc(ReconDiscrepancy::getRiskLevel);
        wrapper.orderByDesc(ReconDiscrepancy::getCreatedAt);

        Page<ReconDiscrepancy> pageObj = new Page<>(page, size);
        return discrepancyMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根据ID获取差异详情
     */
    public ReconDiscrepancy getById(Long id) {
        ReconDiscrepancy discrepancy = discrepancyMapper.selectById(id);
        if (discrepancy == null) {
            throw new BusinessException(ResultCode.DISCREPANCY_NOT_FOUND);
        }
        return discrepancy;
    }

    // ==================== AI分类与根因分析 ====================

    /**
     * 对单条差异执行AI分类与根因分析
     */
    public ReconDiscrepancy classifyAndAnalyze(Long discrepancyId) {
        ReconDiscrepancy discrepancy = getById(discrepancyId);

        // 构建分类请求 — 优先使用原始记录数据构建丰富请求
        DiscrepancyClassifyRequest classifyReq = buildClassifyRequest(discrepancy);

        // AI分类
        String category = aiService.classifyDiscrepancy(classifyReq);
        discrepancy.setCategory(category);
        log.info("差异 {} 分类结果: {}", discrepancyId, category);

        // 构建根因分析请求
        RootCauseRequest rootCauseReq = buildRootCauseRequest(discrepancy, category);

        // AI根因分析
        RootCauseResult rootCauseResult = aiService.analyzeRootCause(rootCauseReq);
        discrepancy.setAiRootCause(rootCauseResult.getRootCauseCategory());
        discrepancy.setAiSuggestion(rootCauseResult.getSuggestion());
        discrepancy.setRiskLevel(rootCauseResult.getRiskLevel());
        log.info("差异 {} 根因分析完成, 风险等级: {}, 置信度: {}%",
                discrepancyId, rootCauseResult.getRiskLevel(), rootCauseResult.getConfidence());

        discrepancyMapper.updateById(discrepancy);
        return discrepancy;
    }

    /**
     * 批量分类 — 对任务下所有未分类的差异执行AI分类与分析
     */
    public void batchClassify(Long taskId) {
        List<ReconDiscrepancy> unclassified = discrepancyMapper.selectList(
                new LambdaQueryWrapper<ReconDiscrepancy>()
                        .eq(ReconDiscrepancy::getTaskId, taskId)
                        .and(w -> w.isNull(ReconDiscrepancy::getCategory)
                                .or()
                                .eq(ReconDiscrepancy::getCategory, ""))
        );

        log.info("任务 {} 发现 {} 条未分类差异, 开始批量分类", taskId, unclassified.size());
        int successCount = 0;
        int failCount = 0;

        for (ReconDiscrepancy d : unclassified) {
            try {
                classifyAndAnalyze(d.getId());
                successCount++;
            } catch (Exception e) {
                log.error("差异 {} 分类失败", d.getId(), e);
                failCount++;
            }
        }

        log.info("任务 {} 批量分类完成: 成功 {} 条, 失败 {} 条", taskId, successCount, failCount);
    }

    // ==================== 处理分配与解决 ====================

    /**
     * 分配处理人
     */
    @Transactional
    public ReconDiscrepancy assignHandler(Long id, Long handlerId, String handlerName) {
        ReconDiscrepancy discrepancy = getById(id);
        discrepancy.setHandlerId(handlerId);
        discrepancy.setHandlerName(handlerName);
        if (discrepancy.getStatus() == null || "PENDING".equals(discrepancy.getStatus())) {
            discrepancy.setStatus("PROCESSING");
        }
        discrepancyMapper.updateById(discrepancy);
        log.info("差异 {} 已分配给处理人 {}({})", id, handlerName, handlerId);
        return discrepancy;
    }

    /**
     * 解决差异
     */
    @Transactional
    public ReconDiscrepancy resolveDiscrepancy(Long id, String resolution, String resolutionNote, Long resolvedBy) {
        ReconDiscrepancy discrepancy = getById(id);
        if ("RESOLVED".equals(discrepancy.getStatus())) {
            throw new BusinessException(ResultCode.DISCREPANCY_ALREADY_RESOLVED);
        }
        discrepancy.setResolution(resolution);
        discrepancy.setResolutionNote(resolutionNote);
        discrepancy.setResolvedBy(resolvedBy);
        discrepancy.setResolvedAt(LocalDateTime.now());
        discrepancy.setStatus("RESOLVED");
        discrepancyMapper.updateById(discrepancy);
        log.info("差异 {} 已解决, 解决方式: {}", id, resolution);
        return discrepancy;
    }

    /**
     * 关闭差异
     */
    @Transactional
    public ReconDiscrepancy closeDiscrepancy(Long id, Long resolvedBy) {
        ReconDiscrepancy discrepancy = getById(id);
        discrepancy.setStatus("CLOSED");
        discrepancy.setResolvedBy(resolvedBy);
        discrepancy.setResolvedAt(LocalDateTime.now());
        discrepancyMapper.updateById(discrepancy);
        log.info("差异 {} 已关闭", id);
        return discrepancy;
    }

    /**
     * 获取SLA已超期的差异列表
     */
    public List<ReconDiscrepancy> getSlaOverdue(Long orgId) {
        LambdaQueryWrapper<ReconDiscrepancy> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(ReconDiscrepancy::getOrgId, orgId);
        }
        wrapper.lt(ReconDiscrepancy::getSlaDeadline, LocalDateTime.now());
        wrapper.notIn(ReconDiscrepancy::getStatus, "RESOLVED", "CLOSED");
        return discrepancyMapper.selectList(wrapper);
    }

    // ==================== 调整单管理 ====================

    /**
     * 创建调整单
     */
    @Transactional
    public ReconAdjustment createAdjustment(ReconAdjustment adjustment) {
        if (adjustment.getStatus() == null || adjustment.getStatus().isEmpty()) {
            adjustment.setStatus("DRAFT");
        }
        adjustmentMapper.insert(adjustment);
        log.info("调整单 {} 已创建 (草稿), 关联差异: {}", adjustment.getId(), adjustment.getDiscrepancyId());
        return adjustment;
    }

    /**
     * 审批通过调整单 — 同时将关联差异标记为已解决
     */
    @Transactional
    public ReconAdjustment approveAdjustment(Long id, Long approvedBy) {
        ReconAdjustment adjustment = adjustmentMapper.selectById(id);
        if (adjustment == null) {
            throw new BusinessException(ResultCode.APPROVAL_NOT_FOUND);
        }
        adjustment.setStatus("APPROVED");
        adjustment.setApprovedBy(approvedBy);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustmentMapper.updateById(adjustment);
        log.info("调整单 {} 已审批通过", id);

        // 同步更新关联差异状态
        if (adjustment.getDiscrepancyId() != null) {
            ReconDiscrepancy discrepancy = discrepancyMapper.selectById(adjustment.getDiscrepancyId());
            if (discrepancy != null && !"RESOLVED".equals(discrepancy.getStatus())) {
                discrepancy.setStatus("RESOLVED");
                discrepancy.setResolvedBy(approvedBy);
                discrepancy.setResolvedAt(LocalDateTime.now());
                discrepancyMapper.updateById(discrepancy);
                log.info("关联差异 {} 已同步标记为已解决", adjustment.getDiscrepancyId());
            }
        }

        return adjustment;
    }

    /**
     * 分页查询调整单
     */
    public IPage<ReconAdjustment> pageAdjustments(int page, int size, Long orgId, Long discrepancyId) {
        LambdaQueryWrapper<ReconAdjustment> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(ReconAdjustment::getOrgId, orgId);
        }
        if (discrepancyId != null) {
            wrapper.eq(ReconAdjustment::getDiscrepancyId, discrepancyId);
        }
        wrapper.orderByDesc(ReconAdjustment::getCreatedAt);
        return adjustmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    // ==================== 统计 ====================

    /**
     * 统计差异数量，支持按组织ID和状态筛选
     */
    public long countDiscrepancies(Long orgId, String status) {
        LambdaQueryWrapper<ReconDiscrepancy> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(ReconDiscrepancy::getOrgId, orgId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ReconDiscrepancy::getStatus, status);
        }
        return discrepancyMapper.selectCount(wrapper);
    }

    /**
     * 获取指定任务下差异的分类统计
     */
    public Map<String, Long> getDiscrepancyStats(Long taskId) {
        QueryWrapper<ReconDiscrepancy> wrapper = new QueryWrapper<>();
        wrapper.select("COALESCE(category, 'UNCATEGORIZED') AS category", "COUNT(*) AS cnt")
                .eq("task_id", taskId)
                .groupBy("category");
        List<Map<String, Object>> maps = discrepancyMapper.selectMaps(wrapper);
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Map<String, Object> row : maps) {
            String cat = (String) row.get("category");
            Long cnt = ((Number) row.get("cnt")).longValue();
            stats.put(cat, cnt);
        }
        return stats;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从差异记录及其关联的原始记录构建分类请求
     */
    private DiscrepancyClassifyRequest buildClassifyRequest(ReconDiscrepancy d) {
        DiscrepancyClassifyRequest.DiscrepancyClassifyRequestBuilder builder = DiscrepancyClassifyRequest.builder()
                .amountDiff(d.getAmountDiff())
                .currency(d.getCurrency());

        // 尝试获取原始记录A
        RawRecord recordA = null;
        if (d.getRecordId() != null) {
            recordA = rawRecordMapper.selectById(d.getRecordId());
        }
        if (recordA != null) {
            builder.recordId(recordA.getId())
                    .amountA(recordA.getAmount())
                    .dateA(recordA.getTransactionDate())
                    .descriptionA(recordA.getDescription())
                    .partyA(recordA.getCounterParty())
                    .transactionRefA(recordA.getTransactionRef())
                    .direction(recordA.getDirection())
                    .hasFee(recordA.getFeeAmount() != null
                            && recordA.getFeeAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        // 尝试获取原始记录B (关联记录)
        RawRecord recordB = null;
        if (d.getRelatedRecordId() != null) {
            recordB = rawRecordMapper.selectById(d.getRelatedRecordId());
        }
        if (recordB != null) {
            builder.amountB(recordB.getAmount())
                    .dateB(recordB.getTransactionDate())
                    .descriptionB(recordB.getDescription())
                    .partyB(recordB.getCounterParty())
                    .transactionRefB(recordB.getTransactionRef());
            if (recordA == null) {
                builder.direction(recordB.getDirection());
            }
        }

        return builder.build();
    }

    /**
     * 从差异记录构建根因分析请求
     */
    private RootCauseRequest buildRootCauseRequest(ReconDiscrepancy d, String category) {
        return RootCauseRequest.builder()
                .discrepancyId(d.getId())
                .category(category)
                .currency(d.getCurrency())
                .build();
    }
}
