package com.recon.module.engine.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.datasource.entity.RawRecord;
import com.recon.module.discrepancy.entity.ReconDiscrepancy;
import com.recon.module.discrepancy.repository.ReconDiscrepancyMapper;
import com.recon.module.engine.entity.ReconMatch;
import com.recon.module.engine.entity.ReconTask;
import com.recon.module.engine.repository.ReconMatchMapper;
import com.recon.module.engine.repository.ReconTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 对账任务服务
 *
 * <p>负责对账任务的创建、执行、匹配结果审核及任务摘要查询。
 * 对账执行过程会调用匹配引擎进行自动匹配，未匹配的记录将生成差异记录，
 * 后续由AI服务进行差异分类和根因分析。</p>
 *
 * @author zhangxin
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReconTaskService {

    private final ReconTaskMapper reconTaskMapper;
    private final ReconMatchMapper reconMatchMapper;
    private final ReconDiscrepancyMapper reconDiscrepancyMapper;
    private final MatchingEngineService matchingEngineService;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_MANUAL_CONFIRMED = "MANUAL_CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String SIDE_A = "A";
    private static final String SIDE_B = "B";

    /**
     * 分页查询对账任务列表
     *
     * @param page     页码
     * @param size     每页条数
     * @param orgId    组织ID（可选）
     * @param status   任务状态（可选）
     * @param taskType 任务类型（可选）
     * @return 分页结果
     */
    public IPage<ReconTask> pageTasks(int page, int size, Long orgId, String status, String taskType) {
        LambdaQueryWrapper<ReconTask> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(ReconTask::getOrgId, orgId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(ReconTask::getStatus, status);
        }
        if (StrUtil.isNotBlank(taskType)) {
            wrapper.eq(ReconTask::getTaskType, taskType);
        }
        wrapper.orderByDesc(ReconTask::getCreatedAt);
        return reconTaskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID查询对账任务
     *
     * @param id 任务ID
     * @return 对账任务实体
     * @throws BusinessException 任务不存在时抛出
     */
    public ReconTask getById(Long id) {
        ReconTask task = reconTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.RECON_TASK_FAILED, "对账任务不存在");
        }
        return task;
    }

    /**
     * 创建对账任务
     *
     * @param task 对账任务实体
     * @return 保存后的对账任务实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconTask createTask(ReconTask task) {
        task.setStatus(STATUS_PENDING);
        reconTaskMapper.insert(task);
        log.info("对账任务创建成功: id={}, name={}, type={}", task.getId(), task.getTaskName(), task.getTaskType());
        return task;
    }

    /**
     * 更新对账任务（仅更新非空字段）
     *
     * @param id     任务ID
     * @param update 更新的任务信息
     * @return 更新后的任务实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconTask updateTask(Long id, ReconTask update) {
        ReconTask existing = getById(id);
        BeanUtil.copyProperties(update, existing, CopyOptions.create().ignoreNullValue());
        reconTaskMapper.updateById(existing);
        log.info("对账任务更新成功: id={}", id);
        return getById(id);
    }

    /**
     * 删除对账任务（逻辑删除）
     *
     * <p>仅允许删除状态为 PENDING、COMPLETED 或 FAILED 的任务，
     * 正在运行中的任务不允许删除。</p>
     *
     * @param id 任务ID
     * @throws BusinessException 任务正在运行中时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        ReconTask task = getById(id);
        if (STATUS_RUNNING.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.RECON_TASK_RUNNING, "任务正在运行中，无法删除");
        }
        reconTaskMapper.deleteById(id);
        log.info("对账任务已删除: id={}, status={}", id, task.getStatus());
    }

    /**
     * 执行对账任务
     *
     * <p>完整的对账执行流程：
     * <ol>
     *   <li>校验任务状态（必须是 PENDING）</li>
     *   <li>将任务状态更新为 RUNNING，记录开始时间</li>
     *   <li>调用匹配引擎执行自动匹配</li>
     *   <li>将未匹配的记录生成差异记录</li>
     *   <li>统计匹配数量、差异数量，计算匹配率</li>
     *   <li>更新任务状态为 COMPLETED</li>
     * </ol>
     * </p>
     *
     * @param taskId 任务ID
     * @return 执行完成后的任务实体
     * @throws BusinessException 任务状态不正确或执行失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconTask executeTask(Long taskId) {
        // 1. 获取任务并校验状态
        ReconTask task = getById(taskId);
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.RECON_TASK_FAILED,
                    "任务状态不正确，当前状态: " + task.getStatus() + "，仅PENDING状态的任务可以执行");
        }

        // 2. 更新为运行中
        LocalDateTime startedAt = LocalDateTime.now();
        task.setStatus(STATUS_RUNNING);
        task.setStartedAt(startedAt);
        reconTaskMapper.updateById(task);
        log.info("对账任务开始执行: taskId={}, name={}", taskId, task.getTaskName());

        try {
            // 3. 调用匹配引擎执行对账
            List<RawRecord> unmatchedRecords = matchingEngineService.executeReconciliation(task);
            log.info("匹配引擎执行完成: taskId={}, unmatchedCount={}", taskId, unmatchedRecords.size());

            // 4. 生成差异记录
            int discrepancyCount = 0;
            List<ReconDiscrepancy> discrepancies = new ArrayList<>();

            for (RawRecord unmatched : unmatchedRecords) {
                // 根据数据源确定记录所属方
                String side = determineSide(task, unmatched.getSourceId());

                ReconDiscrepancy discrepancy = new ReconDiscrepancy()
                        .setTaskId(taskId)
                        .setOrgId(task.getOrgId())
                        .setRecordId(unmatched.getId())
                        .setSide(side)
                        .setStatus(STATUS_PENDING)
                        .setAmount(unmatched.getAmount())
                        .setAmountDiff(unmatched.getAmount())
                        .setCreatedAt(LocalDateTime.now())
                        .setUpdatedAt(LocalDateTime.now());
                discrepancies.add(discrepancy);
                discrepancyCount++;
            }

            // 批量保存差异记录
            if (!discrepancies.isEmpty()) {
                for (ReconDiscrepancy discrepancy : discrepancies) {
                    reconDiscrepancyMapper.insert(discrepancy);
                }
                log.info("差异记录生成完成: taskId={}, count={}", taskId, discrepancyCount);
            }

            // 5. 统计匹配结果
            int totalCount = (task.getTotalACount() != null ? task.getTotalACount() : 0)
                    + (task.getTotalBCount() != null ? task.getTotalBCount() : 0);
            int unmatchedCount = unmatchedRecords.size();
            int matchedCount = Math.max(0, totalCount - unmatchedCount);

            // 重新从数据库统计确认的匹配记录数
            LambdaQueryWrapper<ReconMatch> matchWrapper = new LambdaQueryWrapper<>();
            matchWrapper.eq(ReconMatch::getTaskId, taskId);
            Long actualMatchedCount = reconMatchMapper.selectCount(matchWrapper);

            // 6. 更新任务结果
            LocalDateTime completedAt = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();

            BigDecimal matchRate = BigDecimal.ZERO;
            if (totalCount > 0) {
                matchRate = BigDecimal.valueOf(actualMatchedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
            }

            task.setMatchedCount(actualMatchedCount.intValue());
            task.setUnmatchedCount(unmatchedCount);
            task.setDiscrepancyCount(discrepancyCount);
            task.setMatchRate(matchRate);
            task.setCompletedAt(completedAt);
            task.setDurationMs(durationMs);
            task.setStatus(STATUS_COMPLETED);
            reconTaskMapper.updateById(task);

            log.info("对账任务执行完成: taskId={}, matched={}, unmatched={}, discrepancy={}, matchRate={}%, durationMs={}",
                    taskId, actualMatchedCount, unmatchedCount, discrepancyCount, matchRate, durationMs);

        } catch (Exception e) {
            log.error("对账任务执行失败: taskId={}", taskId, e);
            LocalDateTime failedAt = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startedAt, failedAt).toMillis();

            task.setStatus(STATUS_FAILED);
            task.setErrorMsg("执行异常: " + e.getMessage());
            task.setCompletedAt(failedAt);
            task.setDurationMs(durationMs);
            reconTaskMapper.updateById(task);

            throw new BusinessException(ResultCode.RECON_TASK_FAILED, "对账任务执行失败: " + e.getMessage());
        }

        return getById(taskId);
    }

    /**
     * 异步执行对账任务
     *
     * <p>将任务提交到Spring异步线程池执行，立即返回CompletableFuture。
     * 调用方可以通过future.get()等待结果或使用future.thenAccept()处理回调。</p>
     *
     * @param taskId 任务ID
     * @return CompletableFuture包装的任务结果
     */
    @Async
    public CompletableFuture<ReconTask> executeTaskAsync(Long taskId) {
        log.info("异步对账任务提交: taskId={}", taskId);
        ReconTask result = executeTask(taskId);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 分页查询匹配结果
     *
     * @param page      页码
     * @param size      每页条数
     * @param taskId    任务ID（可选）
     * @param matchType 匹配类型（可选）：EXACT、FUZZY、AI_SUGGESTED
     * @param status    状态（可选）
     * @return 分页结果
     */
    public IPage<ReconMatch> pageMatches(int page, int size, Long taskId, String matchType, String status) {
        LambdaQueryWrapper<ReconMatch> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(ReconMatch::getTaskId, taskId);
        }
        if (StrUtil.isNotBlank(matchType)) {
            wrapper.eq(ReconMatch::getMatchType, matchType);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(ReconMatch::getStatus, status);
        }
        wrapper.orderByDesc(ReconMatch::getCreatedAt);
        return reconMatchMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 确认匹配结果
     *
     * <p>人工审核后将匹配状态设置为手动确认。</p>
     *
     * @param matchId    匹配记录ID
     * @param reviewedBy 审核人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmMatch(Long matchId, Long reviewedBy) {
        ReconMatch match = getMatchById(matchId);
        match.setStatus(STATUS_MANUAL_CONFIRMED);
        match.setReviewedBy(reviewedBy);
        match.setReviewedAt(LocalDateTime.now());
        reconMatchMapper.updateById(match);
        log.info("匹配结果已确认: matchId={}, reviewedBy={}", matchId, reviewedBy);
    }

    /**
     * 驳回匹配结果
     *
     * <p>人工审核后将匹配结果标记为驳回，需填写驳回原因。</p>
     *
     * @param matchId    匹配记录ID
     * @param reviewedBy 审核人ID
     * @param comment    驳回原因/审核意见
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectMatch(Long matchId, Long reviewedBy, String comment) {
        ReconMatch match = getMatchById(matchId);
        match.setStatus(STATUS_REJECTED);
        match.setReviewedBy(reviewedBy);
        match.setReviewedAt(LocalDateTime.now());
        match.setReviewComment(comment);
        reconMatchMapper.updateById(match);
        log.info("匹配结果已驳回: matchId={}, reviewedBy={}, comment={}", matchId, reviewedBy, comment);
    }

    /**
     * 获取待人工审核的匹配列表
     *
     * @param taskId 任务ID
     * @return 待审核的匹配记录列表
     */
    public List<ReconMatch> getPendingReviewMatches(Long taskId) {
        List<ReconMatch> pendingMatches = reconMatchMapper.selectPendingReviewMatches(taskId);
        log.debug("查询待审核匹配: taskId={}, count={}", taskId, pendingMatches.size());
        return pendingMatches;
    }

    /**
     * 获取任务执行摘要
     *
     * <p>在任务已有数据基础上计算额外的摘要指标，包括匹配率等。</p>
     *
     * @param taskId 任务ID
     * @return 包含摘要信息的任务实体
     */
    public ReconTask getTaskSummary(Long taskId) {
        ReconTask task = getById(taskId);

        // 计算匹配率
        if (task.getMatchedCount() != null && task.getUnmatchedCount() != null) {
            int total = task.getMatchedCount() + task.getUnmatchedCount();
            if (total > 0) {
                BigDecimal matchRate = BigDecimal.valueOf(task.getMatchedCount())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                task.setMatchRate(matchRate);
            } else {
                task.setMatchRate(BigDecimal.ZERO);
            }
        }

        // 查询差异记录统计
        LambdaQueryWrapper<ReconDiscrepancy> discrepancyWrapper = new LambdaQueryWrapper<>();
        discrepancyWrapper.eq(ReconDiscrepancy::getTaskId, taskId);
        long actualDiscrepancyCount = reconDiscrepancyMapper.selectCount(discrepancyWrapper);
        task.setDiscrepancyCount((int) actualDiscrepancyCount);

        log.debug("任务摘要查询: taskId={}, matchRate={}%, discrepancyCount={}",
                taskId, task.getMatchRate(), actualDiscrepancyCount);

        return task;
    }

    // ==================== 私有方法 ====================

    /**
     * 根据匹配ID获取匹配记录，不存在时抛出异常
     */
    private ReconMatch getMatchById(Long matchId) {
        ReconMatch match = reconMatchMapper.selectById(matchId);
        if (match == null) {
            throw new BusinessException(ResultCode.MATCH_ENGINE_ERROR, "匹配记录不存在");
        }
        return match;
    }

    /**
     * 根据数据源ID确定记录所属方（A或B）
     *
     * @param task     对账任务
     * @param sourceId 数据源ID
     * @return "A" 或 "B"
     */
    private String determineSide(ReconTask task, Long sourceId) {
        if (sourceId != null && sourceId.equals(task.getSourceAId())) {
            return SIDE_A;
        }
        if (sourceId != null && sourceId.equals(task.getSourceBId())) {
            return SIDE_B;
        }
        // 如果sourceId不匹配任一数据源，默认标记为A方
        log.warn("无法确定记录所属方: sourceId={}, sourceAId={}, sourceBId={}",
                sourceId, task.getSourceAId(), task.getSourceBId());
        return SIDE_A;
    }
}
