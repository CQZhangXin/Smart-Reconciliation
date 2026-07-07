package com.recon.module.discrepancy.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.discrepancy.entity.ReconDiscrepancy;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReconDiscrepancyMapper extends BaseMapper<ReconDiscrepancy> {
}
