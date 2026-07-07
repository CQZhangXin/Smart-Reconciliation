package com.recon.module.datasource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.datasource.entity.DataSource;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceMapper extends BaseMapper<DataSource> {
}
