package com.recon.module.datasource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.datasource.entity.DataSourceSyncLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceSyncLogMapper extends BaseMapper<DataSourceSyncLog> {
}
