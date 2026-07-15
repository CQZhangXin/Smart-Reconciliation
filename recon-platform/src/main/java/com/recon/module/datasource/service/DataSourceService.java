package com.recon.module.datasource.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.common.utils.HashUtil;
import com.recon.module.datasource.entity.DataSource;
import com.recon.module.datasource.entity.DataSourceSyncLog;
import com.recon.module.datasource.entity.RawRecord;
import com.recon.module.datasource.repository.DataSourceMapper;
import com.recon.module.datasource.repository.DataSourceSyncLogMapper;
import com.recon.module.datasource.repository.RawRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 数据源服务
 *
 * <p>提供数据源的CRUD、连接测试、数据同步及原始记录管理功能。</p>
 *
 * @author zhangxin
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final RawRecordMapper rawRecordMapper;
    private final DataSourceSyncLogMapper dataSourceSyncLogMapper;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String HEALTH_UNKNOWN = "UNKNOWN";
    private static final String HEALTH_HEALTHY = "HEALTHY";
    private static final String SYNC_TYPE_MANUAL = "MANUAL";

    /**
     * 分页查询数据源列表
     *
     * @param page   页码
     * @param size   每页条数
     * @param orgId  组织ID（可选）
     * @param dsType 数据源类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    public IPage<DataSource> pageDataSource(int page, int size, Long orgId, String dsType, String status) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(DataSource::getOrgId, orgId);
        }
        if (StrUtil.isNotBlank(dsType)) {
            wrapper.eq(DataSource::getDsType, dsType);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(DataSource::getStatus, status);
        }
        wrapper.orderByDesc(DataSource::getCreatedAt);
        return dataSourceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据ID查询数据源
     *
     * @param id 数据源ID
     * @return 数据源实体
     * @throws BusinessException 数据源不存在时抛出
     */
    public DataSource getById(Long id) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            throw new BusinessException(ResultCode.DATA_SOURCE_ERROR, "数据源不存在");
        }
        return dataSource;
    }

    /**
     * 获取数据源的字段映射配置
     */
    public Map<String, Object> getFieldMapping(Long id) {
        DataSource ds = getById(id);
        String fieldMapping = ds.getFieldMapping();
        if (StrUtil.isBlank(fieldMapping)) {
            return Map.of();
        }
        try {
            return JSONUtil.parseObj(fieldMapping);
        } catch (Exception e) {
            log.warn("数据源 {} 字段映射解析失败: {}", id, e.getMessage());
            return Map.of();
        }
    }

    /**
     * 创建数据源
     *
     * @param dataSource 数据源实体
     * @return 保存后的数据源实体
     */
    @Transactional(rollbackFor = Exception.class)
    public DataSource createDataSource(DataSource dataSource) {
        if (StrUtil.isBlank(dataSource.getStatus())) {
            dataSource.setStatus(STATUS_ACTIVE);
        }
        if (StrUtil.isBlank(dataSource.getHealthStatus())) {
            dataSource.setHealthStatus(HEALTH_UNKNOWN);
        }
        dataSourceMapper.insert(dataSource);
        log.info("数据源创建成功: id={}, name={}", dataSource.getId(), dataSource.getDsName());
        return dataSource;
    }

    /**
     * 更新数据源（仅更新非空字段）
     *
     * @param id     数据源ID
     * @param update 更新的数据源信息
     * @return 更新后的数据源实体
     */
    @Transactional(rollbackFor = Exception.class)
    public DataSource updateDataSource(Long id, DataSource update) {
        DataSource existing = getById(id);
        BeanUtil.copyProperties(update, existing, CopyOptions.create().ignoreNullValue());
        dataSourceMapper.updateById(existing);
        log.info("数据源更新成功: id={}", id);
        return getById(id);
    }

    /**
     * 删除数据源（逻辑删除）
     *
     * @param id 数据源ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataSource(Long id) {
        getById(id);
        dataSourceMapper.deleteById(id);
        log.info("数据源已删除: id={}", id);
    }

    /**
     * 测试数据源连接
     *
     * <p>模拟连接测试，实际项目中需根据数据源类型（数据库、API、文件等）
     * 实现具体的连接验证逻辑。</p>
     *
     * @param id 数据源ID
     * @return true表示连接成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean testConnection(Long id) {
        DataSource dataSource = getById(id);
        log.info("测试数据源连接: id={}, name={}, type={}", id, dataSource.getDsName(), dataSource.getDsType());

        // TODO: 根据 dsType 实现真实的连接测试逻辑
        // 例如：数据库连接 -> JDBC ping，API连接 -> HTTP健康检查，文件源 -> 文件可读性检查

        dataSource.setHealthStatus(HEALTH_HEALTHY);
        dataSourceMapper.updateById(dataSource);
        log.info("数据源连接测试通过: id={}", id);
        return true;
    }

    /**
     * 同步数据
     *
     * <p>从数据源拉取数据并存入原始记录表。当前为模拟实现，
     * 生成50条模拟记录。生产环境需根据 syncStrategy 和 dsType
     * 实现真实的银行API、支付网关API或文件导入逻辑。</p>
     *
     * @param sourceId 数据源ID
     * @return 同步日志
     */
    @Transactional(rollbackFor = Exception.class)
    public DataSourceSyncLog syncData(Long sourceId) {
        DataSource dataSource = getById(sourceId);
        LocalDateTime startedAt = LocalDateTime.now();

        // 创建同步日志
        DataSourceSyncLog syncLog = new DataSourceSyncLog()
                .setSourceId(sourceId)
                .setOrgId(dataSource.getOrgId())
                .setSyncType(SYNC_TYPE_MANUAL)
                .setSyncStatus(STATUS_RUNNING)
                .setStartedAt(startedAt)
                .setCreatedAt(startedAt);
        dataSourceSyncLogMapper.insert(syncLog);

        int errorCount = 0;
        try {
            // 生成模拟数据
            List<RawRecord> mockRecords = generateMockRecords(sourceId, dataSource.getOrgId(), 50);
            for (RawRecord record : mockRecords) {
                try {
                    rawRecordMapper.insert(record);
                } catch (Exception e) {
                    log.error("插入模拟记录失败: record={}", JSONUtil.toJsonStr(record), e);
                    errorCount++;
                }
            }

            // 更新同步日志
            LocalDateTime completedAt = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
            syncLog.setTotalCount(mockRecords.size())
                    .setSuccessCount(mockRecords.size() - errorCount)
                    .setErrorCount(errorCount)
                    .setSyncStatus(STATUS_SUCCESS)
                    .setCompletedAt(completedAt)
                    .setDurationMs(durationMs);
            dataSourceSyncLogMapper.updateById(syncLog);

            // 更新数据源同步状态
            dataSource.setLastSyncAt(completedAt);
            dataSource.setLastSyncStatus(STATUS_SUCCESS);
            dataSourceMapper.updateById(dataSource);

            log.info("数据同步完成: sourceId={}, total={}, success={}, error={}, durationMs={}",
                    sourceId, mockRecords.size(), mockRecords.size() - errorCount, errorCount, durationMs);
        } catch (Exception e) {
            log.error("数据同步失败: sourceId={}", sourceId, e);
            LocalDateTime failedAt = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startedAt, failedAt).toMillis();
            syncLog.setSyncStatus(STATUS_FAILED)
                    .setErrorMsg("同步异常: " + e.getMessage())
                    .setCompletedAt(failedAt)
                    .setDurationMs(durationMs);
            dataSourceSyncLogMapper.updateById(syncLog);

            dataSource.setLastSyncAt(failedAt);
            dataSource.setLastSyncStatus(STATUS_FAILED);
            dataSourceMapper.updateById(dataSource);

            throw new BusinessException(ResultCode.DATA_SYNC_FAILED, "数据同步失败: " + e.getMessage());
        }

        return syncLog;
    }

    /**
     * 分页查询原始记录
     *
     * @param page     页码
     * @param size     每页条数
     * @param sourceId 数据源ID（可选）
     * @param orgId    组织ID（可选）
     * @param status   状态（可选）
     * @return 分页结果
     */
    public IPage<RawRecord> pageRawRecords(int page, int size, Long sourceId, Long orgId, String status) {
        LambdaQueryWrapper<RawRecord> wrapper = new LambdaQueryWrapper<>();
        if (sourceId != null) {
            wrapper.eq(RawRecord::getSourceId, sourceId);
        }
        if (orgId != null) {
            wrapper.eq(RawRecord::getOrgId, orgId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(RawRecord::getStatus, status);
        }
        wrapper.orderByDesc(RawRecord::getCreatedAt);
        return rawRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 批量导入原始记录
     *
     * <p>从外部系统（Excel、CSV、API等）批量导入原始交易记录。
     * 每条记录会计算SHA-256哈希值用于后续对账匹配。</p>
     *
     * @param sourceId    数据源ID
     * @param rawDataList 原始数据列表，每项为字段名到值的映射
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchImportRecords(Long sourceId, List<Map<String, Object>> rawDataList) {
        DataSource dataSource = getById(sourceId);
        String batchId = UUID.randomUUID().toString().replace("-", "");
        List<RawRecord> records = new ArrayList<>(rawDataList.size());

        for (Map<String, Object> rawData : rawDataList) {
            RawRecord record = new RawRecord();
            record.setSourceId(sourceId);
            record.setOrgId(dataSource.getOrgId());
            record.setBatchId(batchId);
            record.setTraceId((String) rawData.getOrDefault("traceId", UUID.randomUUID().toString()));

            // 存储原始JSON数据
            record.setRawData(JSONUtil.toJsonStr(rawData));

            // 提取金额
            Object amountObj = rawData.get("amount");
            if (amountObj != null) {
                if (amountObj instanceof BigDecimal) {
                    record.setAmount((BigDecimal) amountObj);
                } else if (amountObj instanceof Number) {
                    record.setAmount(BigDecimal.valueOf(((Number) amountObj).doubleValue()));
                } else {
                    try {
                        record.setAmount(new BigDecimal(amountObj.toString()));
                    } catch (NumberFormatException e) {
                        log.warn("无法解析金额字段: {}", amountObj);
                        record.setAmount(BigDecimal.ZERO);
                    }
                }
            } else {
                record.setAmount(BigDecimal.ZERO);
            }

            // 提取其他字段
            record.setCurrency((String) rawData.getOrDefault("currency", "CNY"));
            record.setTransactionRef((String) rawData.get("transactionRef"));
            record.setDescription((String) rawData.get("description"));
            record.setCounterParty((String) rawData.get("counterParty"));
            record.setCounterAcct((String) rawData.get("counterAcct"));
            record.setDirection((String) rawData.get("direction"));

            // 提取日期
            Object dateObj = rawData.get("transactionDate");
            if (dateObj instanceof LocalDate) {
                record.setTransactionDate((LocalDate) dateObj);
            } else if (dateObj instanceof String) {
                try {
                    record.setTransactionDate(LocalDate.parse((String) dateObj));
                } catch (Exception e) {
                    log.warn("无法解析交易日期: {}", dateObj);
                    record.setTransactionDate(LocalDate.now());
                }
            }

            // 计算哈希值用于后续对账
            String hashInput = JSONUtil.toJsonStr(rawData);
            record.setHashValue(HashUtil.sha256(hashInput));

            record.setStatus(STATUS_PENDING);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            records.add(record);
        }

        rawRecordMapper.insert(records);
        log.info("批量导入原始记录完成: sourceId={}, batchId={}, count={}", sourceId, batchId, records.size());
    }

    // ==================== 私有方法 ====================

    /**
     * 生成模拟原始记录
     */
    private List<RawRecord> generateMockRecords(Long sourceId, Long orgId, int count) {
        List<RawRecord> records = new ArrayList<>(count);
        Random random = new Random(System.currentTimeMillis());
        LocalDate today = LocalDate.now();
        String batchId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        String[] descriptions = {
                "商户收款-微信支付", "商户收款-支付宝", "退款-原路退回", "手续费扣除",
                "平台分账-技术服务费", "提现-对公账户", "充值-余额充值", "转账-跨行转账",
                "代付-供应商结算", "跨境支付-美元结汇", "红包发放", "优惠券核销",
                "预授权确认", "分期扣款-第{0}期", "保证金冻结", "利息收入",
                "税务代扣-增值税", "社保代缴", "工资代发", "报销款支付"
        };

        String[] counterParties = {
                "深圳市腾讯计算机系统有限公司", "支付宝（中国）网络技术有限公司",
                "北京银行股份有限公司", "招商银行股份有限公司", "上海浦东发展银行",
                "中国工商银行", "中国建设银行", "中国农业银行"
        };

        String[] directions = {"IN", "OUT"};
        BigDecimal minAmount = new BigDecimal("100.00");
        BigDecimal maxAmount = new BigDecimal("50000.00");
        BigDecimal range = maxAmount.subtract(minAmount);

        for (int i = 0; i < count; i++) {
            RawRecord record = new RawRecord();
            record.setSourceId(sourceId);
            record.setOrgId(orgId);
            record.setBatchId(batchId);
            record.setTraceId(UUID.randomUUID().toString());

            // 随机金额，保留两位小数
            BigDecimal randomAmount = minAmount.add(
                    range.multiply(BigDecimal.valueOf(random.nextDouble())))
                    .setScale(2, RoundingMode.HALF_UP);
            record.setAmount(randomAmount);
            record.setCurrency("CNY");

            // 随机日期（过去30天内）
            LocalDate txDate = today.minusDays(random.nextInt(30));
            record.setTransactionDate(txDate);
            record.setBookingDate(txDate);
            record.setValueDate(txDate.plusDays(1));

            // 随机描述
            String descTemplate = descriptions[random.nextInt(descriptions.length)];
            String description = descTemplate.contains("{0}")
                    ? descTemplate.replace("{0}", String.valueOf(random.nextInt(12) + 1))
                    : descTemplate;
            record.setDescription(description);

            record.setTransactionRef("TXN" + System.currentTimeMillis() + String.format("%04d", i));
            record.setCounterParty(counterParties[random.nextInt(counterParties.length)]);
            record.setCounterAcct(String.format("6222%012d", Math.abs(random.nextLong() % 1000000000000L)));
            record.setDirection(directions[random.nextInt(directions.length)]);

            // 余额和手续费
            record.setBalance(new BigDecimal("100000.00").add(randomAmount));
            if (random.nextDouble() < 0.3) {
                record.setFeeAmount(
                        randomAmount.multiply(new BigDecimal("0.003"))
                                .setScale(2, RoundingMode.HALF_UP));
            }

            // JSON序列化用于哈希计算
            String rawDataJson = JSONUtil.toJsonStr(record);
            record.setRawData(rawDataJson);
            record.setHashValue(HashUtil.sha256(rawDataJson));
            record.setStatus(STATUS_PENDING);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);

            records.add(record);
        }

        log.info("生成模拟记录完成: count={}, batchId={}", count, batchId);
        return records;
    }
}
