package com.recon.module.engine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.engine.entity.ReconTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReconTaskMapper extends BaseMapper<ReconTask> {
}
