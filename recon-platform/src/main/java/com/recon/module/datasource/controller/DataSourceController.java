package com.recon.module.datasource.controller;

import com.recon.common.response.ApiResponse;
import com.recon.common.response.PageResult;
import com.recon.module.datasource.entity.DataSource;
import com.recon.module.datasource.entity.DataSourceSyncLog;
import com.recon.module.datasource.entity.RawRecord;
import com.recon.module.datasource.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/datasource")
@RequiredArgsConstructor
@Tag(name = "数据源管理")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @Operation(summary = "分页查询数据源")
    @GetMapping("/page")
    public ApiResponse<PageResult<DataSource>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String dsType,
            @RequestParam(required = false) String status) {
        log.info("分页查询数据源: page={}, size={}, orgId={}, dsType={}, status={}", page, size, orgId, dsType, status);
        return ApiResponse.success(PageResult.of(
                dataSourceService.pageDataSource(page, size, orgId, dsType, status)));
    }

    @Operation(summary = "根据ID查询数据源")
    @GetMapping("/{id}")
    public ApiResponse<DataSource> getById(@PathVariable Long id) {
        log.info("查询数据源: id={}", id);
        return ApiResponse.success(dataSourceService.getById(id));
    }

    @Operation(summary = "创建数据源")
    @PostMapping
    public ApiResponse<DataSource> create(@RequestBody @Valid DataSource dataSource) {
        log.info("创建数据源: dsName={}", dataSource.getDsName());
        return ApiResponse.success(dataSourceService.createDataSource(dataSource));
    }

    @Operation(summary = "更新数据源")
    @PutMapping("/{id}")
    public ApiResponse<DataSource> update(@PathVariable Long id, @RequestBody DataSource dataSource) {
        log.info("更新数据源: id={}", id);
        return ApiResponse.success(dataSourceService.updateDataSource(id, dataSource));
    }

    @Operation(summary = "删除数据源")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除数据源: id={}", id);
        dataSourceService.deleteDataSource(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取数据源字段映射建议")
    @GetMapping("/{id}/field-mapping")
    public ApiResponse<Map<String, Object>> getFieldMapping(@PathVariable Long id) {
        log.info("获取字段映射: sourceId={}", id);
        return ApiResponse.success(dataSourceService.getFieldMapping(id));
    }

    @Operation(summary = "测试数据源连接")
    @PostMapping("/{id}/test-connection")
    public ApiResponse<Boolean> testConnection(@PathVariable Long id) {
        log.info("测试数据源连接: id={}", id);
        return ApiResponse.success(dataSourceService.testConnection(id));
    }

    @Operation(summary = "同步数据源")
    @PostMapping("/{id}/sync")
    public ApiResponse<DataSourceSyncLog> sync(@PathVariable Long id) {
        log.info("同步数据源: id={}", id);
        return ApiResponse.success(dataSourceService.syncData(id));
    }

    @Operation(summary = "分页查询原始记录")
    @GetMapping("/{id}/records/page")
    public ApiResponse<PageResult<RawRecord>> recordsPage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        log.info("分页查询原始记录: sourceId={}, page={}, size={}, status={}", id, page, size, status);
        return ApiResponse.success(PageResult.of(
                dataSourceService.pageRawRecords(page, size, id, null, status)));
    }

    @Operation(summary = "批量导入原始记录")
    @PostMapping("/{id}/records/batch")
    public ApiResponse<Void> batchInsertRecords(@PathVariable Long id,
                                                 @RequestBody List<Map<String, Object>> records) {
        log.info("批量导入原始记录: sourceId={}, count={}", id, records.size());
        dataSourceService.batchImportRecords(id, records);
        return ApiResponse.success();
    }
}
