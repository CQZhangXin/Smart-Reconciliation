package com.recon.module.custom.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recon.ai.dto.ReconDefinitionNLResult;
import com.recon.ai.service.AIService;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.custom.dto.CustomReconRunRequest;
import com.recon.module.custom.dto.CustomReconRunResult;
import com.recon.module.custom.dto.CustomReconValidateResult;
import com.recon.module.custom.dto.NLParseResult;
import com.recon.module.custom.entity.CustomReconDefinition;
import com.recon.module.custom.repository.CustomReconDefinitionMapper;
import com.recon.module.datasource.entity.DataSource;
import com.recon.module.datasource.entity.RawRecord;
import com.recon.module.datasource.repository.DataSourceMapper;
import com.recon.module.datasource.repository.RawRecordMapper;
import com.recon.module.engine.entity.ReconTask;
import com.recon.module.engine.service.ReconTaskService;
import com.recon.module.rule.entity.ReconRuleConfig;
import com.recon.module.rule.repository.ReconRuleConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义对账服务
 *
 * <p>负责自定义对账方案的 CRUD、预检与执行。执行时复用现有对账任务与匹配引擎，
 * 支持多 B 方数据源（为每个 B 源创建一对任务）。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomReconService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String TASK_TYPE_CUSTOM = "CUSTOM";
    private static final int PENDING_PREVIEW_LIMIT = 5000;

    private final CustomReconDefinitionMapper definitionMapper;
    private final DataSourceMapper dataSourceMapper;
    private final RawRecordMapper rawRecordMapper;
    private final ReconRuleConfigMapper ruleConfigMapper;
    private final ReconTaskService reconTaskService;
    private final AIService aiService;

    /**
     * 分页查询自定义对账方案
     *
     * @param page    页码
     * @param size    每页条数
     * @param orgId   组织ID
     * @param status  状态
     * @param keyword 名称关键字
     * @return 分页结果
     */
    public IPage<CustomReconDefinition> pageDefinitions(int page, int size, Long orgId,
                                                        String status, String keyword) {
        LambdaQueryWrapper<CustomReconDefinition> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(CustomReconDefinition::getOrgId, orgId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(CustomReconDefinition::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(CustomReconDefinition::getDefName, keyword)
                    .or()
                    .like(CustomReconDefinition::getDefCode, keyword));
        }
        wrapper.orderByDesc(CustomReconDefinition::getUpdatedAt);
        return definitionMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID查询方案
     *
     * @param id 方案ID
     * @return 方案实体
     */
    public CustomReconDefinition getById(Long id) {
        CustomReconDefinition definition = definitionMapper.selectById(id);
        if (definition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "自定义对账方案不存在");
        }
        return definition;
    }

    /**
     * 创建自定义对账方案
     *
     * @param definition 方案实体
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomReconDefinition createDefinition(CustomReconDefinition definition) {
        validateDefinitionPayload(definition);
        if (StrUtil.isBlank(definition.getStatus())) {
            definition.setStatus(STATUS_DRAFT);
        }
        if (StrUtil.isBlank(definition.getPeriodType())) {
            definition.setPeriodType("MONTHLY");
        }
        if (definition.getMatchLayers() == null || definition.getMatchLayers().isEmpty()) {
            Map<String, Boolean> defaultLayers = new HashMap<>();
            defaultLayers.put("exact", true);
            defaultLayers.put("rule", true);
            defaultLayers.put("ai", true);
            defaultLayers.put("split", true);
            definition.setMatchLayers(defaultLayers);
        }
        ensureDefCodeUnique(definition.getOrgId(), definition.getDefCode(), null);
        definitionMapper.insert(definition);
        log.info("自定义对账方案创建成功: id={}, name={}", definition.getId(), definition.getDefName());
        return definition;
    }

    /**
     * 更新自定义对账方案
     *
     * @param id     方案ID
     * @param update 更新内容
     * @return 更新后实体
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomReconDefinition updateDefinition(Long id, CustomReconDefinition update) {
        CustomReconDefinition existing = getById(id);
        if (StrUtil.isNotBlank(update.getDefCode())
                && !update.getDefCode().equals(existing.getDefCode())) {
            ensureDefCodeUnique(existing.getOrgId(), update.getDefCode(), id);
        }
        BeanUtil.copyProperties(update, existing, CopyOptions.create().ignoreNullValue());
        validateDefinitionPayload(existing);
        definitionMapper.updateById(existing);
        log.info("自定义对账方案更新成功: id={}", id);
        return getById(id);
    }

    /**
     * 删除自定义对账方案
     *
     * @param id 方案ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(Long id) {
        getById(id);
        definitionMapper.deleteById(id);
        log.info("自定义对账方案已删除: id={}", id);
    }

    /**
     * 启用方案
     *
     * @param id 方案ID
     * @return 更新后实体
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomReconDefinition enable(Long id) {
        CustomReconDefinition definition = getById(id);
        CustomReconValidateResult validateResult = validate(id);
        if (!validateResult.isValid()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "方案校验未通过: " + String.join("; ", validateResult.getErrors()));
        }
        definition.setStatus(STATUS_ACTIVE);
        definitionMapper.updateById(definition);
        return definition;
    }

    /**
     * 停用方案
     *
     * @param id 方案ID
     * @return 更新后实体
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomReconDefinition disable(Long id) {
        CustomReconDefinition definition = getById(id);
        definition.setStatus(STATUS_INACTIVE);
        definitionMapper.updateById(definition);
        return definition;
    }

    /**
     * 预检方案配置与数据就绪情况
     *
     * @param id 方案ID
     * @return 预检结果
     */
    public CustomReconValidateResult validate(Long id) {
        CustomReconDefinition definition = getById(id);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        DataSource sourceA = dataSourceMapper.selectById(definition.getSourceAId());
        if (sourceA == null) {
            errors.add("主数据源A不存在");
        } else if (!STATUS_ACTIVE.equals(sourceA.getStatus())) {
            errors.add("主数据源A未启用: " + sourceA.getDsName());
        }

        List<Long> sourceBIdList = definition.getSourceBIds();
        if (CollUtil.isEmpty(sourceBIdList)) {
            errors.add("至少配置一个对账方数据源B");
        }

        List<Map<String, Object>> sourceBInfoList = new ArrayList<>();
        int pendingB = 0;
        if (CollUtil.isNotEmpty(sourceBIdList)) {
            for (Long sourceBId : sourceBIdList) {
                if (definition.getSourceAId() != null && definition.getSourceAId().equals(sourceBId)) {
                    errors.add("数据源B不能与主数据源A相同: " + sourceBId);
                    continue;
                }
                DataSource sourceB = dataSourceMapper.selectById(sourceBId);
                if (sourceB == null) {
                    errors.add("对账方数据源不存在: " + sourceBId);
                    continue;
                }
                if (!STATUS_ACTIVE.equals(sourceB.getStatus())) {
                    errors.add("对账方数据源未启用: " + sourceB.getDsName());
                }
                List<RawRecord> pendingRecords = rawRecordMapper.selectPendingRecords(
                        sourceBId, PENDING_PREVIEW_LIMIT);
                pendingB += pendingRecords.size();
                sourceBInfoList.add(buildSourceInfo(sourceB, pendingRecords.size()));
                if (pendingRecords.isEmpty()) {
                    warnings.add("数据源「" + sourceB.getDsName() + "」暂无待对账记录，请先同步");
                }
            }
        }

        int pendingA = 0;
        Map<String, Object> sourceAInfo = null;
        if (sourceA != null) {
            List<RawRecord> pendingRecordsA = rawRecordMapper.selectPendingRecords(
                    sourceA.getId(), PENDING_PREVIEW_LIMIT);
            pendingA = pendingRecordsA.size();
            sourceAInfo = buildSourceInfo(sourceA, pendingA);
            if (pendingA == 0) {
                warnings.add("主数据源「" + sourceA.getDsName() + "」暂无待对账记录，请先同步");
            }
        }

        List<Long> ruleIdList = definition.getRuleIds();
        int activeRuleCount = 0;
        if (CollUtil.isNotEmpty(ruleIdList)) {
            for (Long ruleId : ruleIdList) {
                ReconRuleConfig rule = ruleConfigMapper.selectById(ruleId);
                if (rule == null) {
                    errors.add("规则不存在: " + ruleId);
                } else if (!STATUS_ACTIVE.equals(rule.getStatus())) {
                    warnings.add("规则「" + rule.getRuleName() + "」未启用，执行时将被跳过");
                } else {
                    activeRuleCount++;
                }
            }
        } else {
            List<ReconRuleConfig> orgRules = ruleConfigMapper.selectActiveRules(definition.getOrgId());
            activeRuleCount = orgRules == null ? 0 : orgRules.size();
            if (activeRuleCount == 0) {
                warnings.add("未指定规则且组织下无启用规则，将仅执行精确匹配等默认层");
            }
        }

        boolean valid = errors.isEmpty();
        return CustomReconValidateResult.builder()
                .valid(valid)
                .errors(errors)
                .warnings(warnings)
                .sourceAInfo(sourceAInfo)
                .sourceBInfoList(sourceBInfoList)
                .activeRuleCount(activeRuleCount)
                .pendingRecordA(pendingA)
                .pendingRecordB(pendingB)
                .build();
    }

    /**
     * 执行自定义对账方案
     *
     * <p>为每个 B 方数据源创建 CUSTOM 类型任务并执行。多源时返回全部任务ID，
     * primaryTaskId 取第一个任务。</p>
     *
     * @param id      方案ID
     * @param request 执行参数
     * @return 执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomReconRunResult run(Long id, CustomReconRunRequest request) {
        CustomReconDefinition definition = getById(id);
        if (STATUS_INACTIVE.equals(definition.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "方案已停用，无法执行");
        }

        CustomReconValidateResult validateResult = validate(id);
        if (!validateResult.isValid()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "方案校验未通过: " + String.join("; ", validateResult.getErrors()));
        }

        List<Long> sourceBIdList = CollUtil.isNotEmpty(request.getSourceBIds())
                ? request.getSourceBIds()
                : definition.getSourceBIds();
        List<Long> ruleIdList = CollUtil.isNotEmpty(request.getRuleIds())
                ? request.getRuleIds()
                : definition.getRuleIds();

        String reconPeriod = StrUtil.blankToDefault(request.getReconPeriod(), definition.getDefaultPeriod());
        if (StrUtil.isBlank(reconPeriod)) {
            reconPeriod = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        boolean async = request.getAsync() == null || Boolean.TRUE.equals(request.getAsync());
        String ruleIdsJson = CollUtil.isEmpty(ruleIdList) ? null : JSONUtil.toJsonStr(ruleIdList);

        List<Long> taskIds = new ArrayList<>();
        for (int i = 0; i < sourceBIdList.size(); i++) {
            Long sourceBId = sourceBIdList.get(i);
            String taskName = sourceBIdList.size() == 1
                    ? definition.getDefName()
                    : definition.getDefName() + "-源" + (i + 1);

            ReconTask task = new ReconTask()
                    .setTaskName(taskName)
                    .setTaskType(TASK_TYPE_CUSTOM)
                    .setSourceAId(definition.getSourceAId())
                    .setSourceBId(sourceBId)
                    .setRuleIds(ruleIdsJson)
                    .setReconPeriod(reconPeriod)
                    .setPriority("NORMAL")
                    .setCreatedBy(definition.getCreatedBy());
            task.setOrgId(definition.getOrgId());
            task.setLedgerId(definition.getLedgerId());

            if (StrUtil.isNotBlank(request.getPeriodStart())) {
                task.setPeriodStart(LocalDate.parse(request.getPeriodStart()));
            }
            if (StrUtil.isNotBlank(request.getPeriodEnd())) {
                task.setPeriodEnd(LocalDate.parse(request.getPeriodEnd()));
            }

            ReconTask created = reconTaskService.createTask(task);
            taskIds.add(created.getId());

            if (async) {
                reconTaskService.executeTaskAsync(created.getId());
            } else {
                reconTaskService.executeTask(created.getId());
            }
        }

        Long primaryTaskId = taskIds.get(0);
        definition.setLastRunTaskId(primaryTaskId);
        definition.setLastRunAt(LocalDateTime.now());
        if (STATUS_DRAFT.equals(definition.getStatus())) {
            definition.setStatus(STATUS_ACTIVE);
        }
        definitionMapper.updateById(definition);

        log.info("自定义对账方案已触发执行: defId={}, taskIds={}, async={}", id, taskIds, async);
        return CustomReconRunResult.builder()
                .definitionId(id)
                .defName(definition.getDefName())
                .primaryTaskId(primaryTaskId)
                .taskIds(taskIds)
                .async(async)
                .message(async
                        ? "已异步创建 " + taskIds.size() + " 个对账任务"
                        : "已同步完成 " + taskIds.size() + " 个对账任务")
                .build();
    }

    private void validateDefinitionPayload(CustomReconDefinition definition) {
        if (StrUtil.isBlank(definition.getDefName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "方案名称不能为空");
        }
        if (StrUtil.isBlank(definition.getDefCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "方案编码不能为空");
        }
        if (definition.getSourceAId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "主数据源A不能为空");
        }
        if (CollUtil.isEmpty(definition.getSourceBIds())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少配置一个对账方数据源B");
        }
    }

    private void ensureDefCodeUnique(Long orgId, String defCode, Long excludeId) {
        LambdaQueryWrapper<CustomReconDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomReconDefinition::getOrgId, orgId)
                .eq(CustomReconDefinition::getDefCode, defCode);
        if (excludeId != null) {
            wrapper.ne(CustomReconDefinition::getId, excludeId);
        }
        Long count = definitionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "方案编码已存在: " + defCode);
        }
    }

    private Map<String, Object> buildSourceInfo(DataSource source, int pendingCount) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", source.getId());
        info.put("dsName", source.getDsName());
        info.put("dsType", source.getDsType());
        info.put("dsCategory", source.getDsCategory());
        info.put("status", source.getStatus());
        info.put("healthStatus", source.getHealthStatus());
        info.put("pendingCount", pendingCount);
        return info;
    }

    // ========== 自然语言解析 ==========

    /**
     * 从自然语言解析对账方案定义
     *
     * @param description 用户自然语言描述
     * @param orgId       组织ID
     * @return NLParseResult 含已解析到 ID 的定义及未匹配项
     */
    public NLParseResult parseFromNL(String description, Long orgId) {
        // 1. 获取可用数据源名称列表
        LambdaQueryWrapper<DataSource> dsWrapper = new LambdaQueryWrapper<>();
        dsWrapper.eq(DataSource::getOrgId, orgId)
                .eq(DataSource::getStatus, "ACTIVE");
        List<DataSource> dataSources = dataSourceMapper.selectList(dsWrapper);
        List<String> sourceNames = dataSources.stream()
                .map(DataSource::getDsName)
                .collect(Collectors.toList());

        // 2. 获取可用规则名称列表
        List<ReconRuleConfig> rules = ruleConfigMapper.selectActiveRules(orgId);
        List<String> ruleNames = rules != null
                ? rules.stream().map(ReconRuleConfig::getRuleName).collect(Collectors.toList())
                : List.of();

        // 3. 调用AI解析
        ReconDefinitionNLResult nlResult = aiService.generateReconDefinitionFromNL(
                description, sourceNames, ruleNames);

        // 4. 创建方案定义并填充基本信息
        CustomReconDefinition definition = new CustomReconDefinition();
        definition.setDefName(nlResult.getDefName());
        definition.setDefCode(findUniqueCode(orgId, nlResult.getDefCode()));
        definition.setDescription(
                StrUtil.isNotBlank(nlResult.getDescription()) ? nlResult.getDescription() : description);
        definition.setPeriodType(
                StrUtil.blankToDefault(nlResult.getPeriodType(), "MONTHLY"));
        definition.setDefaultPeriod(nlResult.getDefaultPeriod());
        definition.setMatchLayers(nlResult.getMatchLayers());
        definition.setOrgId(orgId);
        definition.setStatus(STATUS_DRAFT);

        List<String> unresolvedSources = new ArrayList<>();
        List<String> unresolvedRules = new ArrayList<>();

        // 5. 解析数据源A名称 -> ID
        if (StrUtil.isNotBlank(nlResult.getSourceAName())) {
            DataSource sourceA = fuzzyMatchDataSource(dataSources, nlResult.getSourceAName());
            if (sourceA != null) {
                definition.setSourceAId(sourceA.getId());
            } else {
                unresolvedSources.add(nlResult.getSourceAName());
            }
        }

        // 6. 解析数据源B名称 -> ID
        List<Long> sourceBIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(nlResult.getSourceBNames())) {
            for (String bName : nlResult.getSourceBNames()) {
                DataSource sourceB = fuzzyMatchDataSource(dataSources, bName);
                if (sourceB != null) {
                    sourceBIds.add(sourceB.getId());
                } else {
                    unresolvedSources.add(bName);
                }
            }
        }
        definition.setSourceBIds(sourceBIds);

        // 7. 解析规则名称 -> ID
        List<Long> ruleIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(nlResult.getRuleNames()) && rules != null) {
            for (String rName : nlResult.getRuleNames()) {
                ReconRuleConfig rule = fuzzyMatchRule(rules, rName);
                if (rule != null) {
                    ruleIds.add(rule.getId());
                } else {
                    unresolvedRules.add(rName);
                }
            }
        }
        definition.setRuleIds(ruleIds);

        return NLParseResult.builder()
                .definition(definition)
                .unresolvedSources(unresolvedSources)
                .unresolvedRules(unresolvedRules)
                .aiExplanation(nlResult.getAiExplanation())
                .build();
    }

    /**
     * 模糊匹配数据源名称 -> DataSource 实体
     * 3步 fallback: 精确匹配 → 包含匹配 → 中文关键词匹配
     */
    private DataSource fuzzyMatchDataSource(List<DataSource> sources, String name) {
        if (sources == null || StrUtil.isBlank(name)) return null;
        String normalized = name.trim().toLowerCase();

        // Step 1: 精确匹配（忽略大小写和空格）
        for (DataSource ds : sources) {
            if (ds.getDsName() != null && ds.getDsName().trim().toLowerCase().equals(normalized)) {
                return ds;
            }
        }
        // Step 2: 包含匹配
        for (DataSource ds : sources) {
            if (ds.getDsName() != null
                    && (ds.getDsName().toLowerCase().contains(normalized)
                    || normalized.contains(ds.getDsName().toLowerCase()))) {
                return ds;
            }
        }
        // Step 3: 中文关键词匹配（至少2个关键词命中）
        String[] keywords = name.split("[\\s、,，]+");
        for (DataSource ds : sources) {
            if (ds.getDsName() == null) continue;
            int matchCount = 0;
            for (String kw : keywords) {
                if (kw.length() >= 2 && ds.getDsName().contains(kw)) {
                    matchCount++;
                }
            }
            if (matchCount >= 2) return ds;
        }
        return null;
    }

    /**
     * 模糊匹配规则名称 -> ReconRuleConfig 实体
     */
    private ReconRuleConfig fuzzyMatchRule(List<ReconRuleConfig> rules, String name) {
        if (rules == null || StrUtil.isBlank(name)) return null;
        String normalized = name.trim().toLowerCase();
        for (ReconRuleConfig rule : rules) {
            if (rule.getRuleName() != null && rule.getRuleName().trim().toLowerCase().equals(normalized)) {
                return rule;
            }
        }
        for (ReconRuleConfig rule : rules) {
            if (rule.getRuleName() != null
                    && (rule.getRuleName().toLowerCase().contains(normalized)
                    || normalized.contains(rule.getRuleName().toLowerCase()))) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 生成唯一编码，避免与已有编码冲突
     */
    private String findUniqueCode(Long orgId, String defCode) {
        String code = StrUtil.blankToDefault(defCode, "NL_GEN_" + System.currentTimeMillis());
        LambdaQueryWrapper<CustomReconDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomReconDefinition::getOrgId, orgId)
                .eq(CustomReconDefinition::getDefCode, code);
        Long count = definitionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return code + "_" + System.currentTimeMillis() % 100000;
        }
        return code;
    }
}
