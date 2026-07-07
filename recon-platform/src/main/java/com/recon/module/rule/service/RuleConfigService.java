package com.recon.module.rule.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recon.ai.dto.RuleGenerationResult;
import com.recon.ai.service.AIService;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.rule.entity.ReconRuleConfig;
import com.recon.module.rule.repository.ReconRuleConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 规则配置服务 — 规则CRUD、启用/禁用、AI规则生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleConfigService {

    private final ReconRuleConfigMapper ruleConfigMapper;
    private final AIService aiService;

    // ==================== 规则查询 ====================

    /**
     * 分页查询规则配置，支持多条件筛选，按优先级降序排列
     */
    public IPage<ReconRuleConfig> pageRules(int page, int size, Long orgId,
                                            String ruleType, String templateType, String status) {
        LambdaQueryWrapper<ReconRuleConfig> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(ReconRuleConfig::getOrgId, orgId);
        }
        if (ruleType != null && !ruleType.isBlank()) {
            wrapper.eq(ReconRuleConfig::getRuleType, ruleType);
        }
        if (templateType != null && !templateType.isBlank()) {
            wrapper.eq(ReconRuleConfig::getTemplateType, templateType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ReconRuleConfig::getStatus, status);
        }
        wrapper.orderByDesc(ReconRuleConfig::getPriority);
        wrapper.orderByDesc(ReconRuleConfig::getUpdatedAt);

        return ruleConfigMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID获取规则详情，不存在则抛出异常
     */
    public ReconRuleConfig getById(Long id) {
        ReconRuleConfig rule = ruleConfigMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.RULE_CONFIG_ERROR, "规则不存在");
        }
        return rule;
    }

    // ==================== 规则CRUD ====================

    /**
     * 创建新规则 — 默认版本1、状态ACTIVE
     */
    @Transactional
    public ReconRuleConfig createRule(ReconRuleConfig rule) {
        if (rule.getVersion() == null) {
            rule.setVersion(1);
        }
        if (rule.getStatus() == null || rule.getStatus().isBlank()) {
            rule.setStatus("ACTIVE");
        }
        ruleConfigMapper.insert(rule);
        log.info("规则 {} (v{}) 已创建", rule.getRuleName(), rule.getVersion());
        return rule;
    }

    /**
     * 更新规则 — 版本号自动递增，仅更新非空字段
     */
    @Transactional
    public ReconRuleConfig updateRule(Long id, ReconRuleConfig update) {
        ReconRuleConfig existing = getById(id);
        existing.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 1);

        BeanUtil.copyProperties(update, existing, CopyOptions.create().ignoreNullValue());

        ruleConfigMapper.updateById(existing);
        log.info("规则 {} 已更新至 v{}", id, existing.getVersion());
        return getById(id);
    }

    /**
     * 逻辑删除规则
     */
    @Transactional
    public void deleteRule(Long id) {
        ReconRuleConfig rule = getById(id);
        ruleConfigMapper.deleteById(id);
        log.info("规则 {} ({}) 已删除", rule.getRuleName(), id);
    }

    // ==================== 规则状态管理 ====================

    /**
     * 启用规则
     */
    @Transactional
    public ReconRuleConfig enableRule(Long id) {
        ReconRuleConfig rule = getById(id);
        rule.setStatus("ACTIVE");
        ruleConfigMapper.updateById(rule);
        log.info("规则 {} 已启用", id);
        return rule;
    }

    /**
     * 禁用规则
     */
    @Transactional
    public ReconRuleConfig disableRule(Long id) {
        ReconRuleConfig rule = getById(id);
        rule.setStatus("INACTIVE");
        ruleConfigMapper.updateById(rule);
        log.info("规则 {} 已禁用", id);
        return rule;
    }

    /**
     * 获取组织下所有已启用的活跃规则
     */
    public List<ReconRuleConfig> getActiveRules(Long orgId) {
        return ruleConfigMapper.selectActiveRules(orgId);
    }

    // ==================== AI规则生成 ====================

    /**
     * 根据自然语言描述生成规则 — 委托给AI服务
     */
    public RuleGenerationResult generateRuleFromNL(String description, Long orgId) {
        log.info("开始NL规则生成, orgId={}, desc={}", orgId, description);
        RuleGenerationResult result = aiService.generateRuleFromNL(description);
        log.info("NL规则生成完成: ruleName={}, ruleCode={}",
                result.getRuleName(), result.getRuleCode());
        return result;
    }

    /**
     * 将AI生成的规则保存到数据库
     */
    @Transactional
    public ReconRuleConfig saveGeneratedRule(RuleGenerationResult generated, Long orgId, Long createdBy) {
        ReconRuleConfig rule = new ReconRuleConfig();
        rule.setOrgId(orgId);
        rule.setRuleName(generated.getRuleName());
        rule.setRuleCode(generated.getRuleCode());
        rule.setRuleType(generated.getRuleType());
        rule.setMatchConfig(generated.getMatchConfigJson());
        rule.setTolerance(generated.getToleranceJson());
        rule.setDescription(generated.getExplanation());
        rule.setStatus("ACTIVE");
        rule.setVersion(1);
        rule.setCreatedBy(createdBy);
        ruleConfigMapper.insert(rule);
        log.info("AI生成的规则 {} 已保存, id={}", rule.getRuleName(), rule.getId());
        return rule;
    }
}
