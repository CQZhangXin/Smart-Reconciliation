package com.recon.module.custom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.custom.entity.CustomReconDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自定义对账方案 Mapper
 */
@Mapper
public interface CustomReconDefinitionMapper extends BaseMapper<CustomReconDefinition> {
}
