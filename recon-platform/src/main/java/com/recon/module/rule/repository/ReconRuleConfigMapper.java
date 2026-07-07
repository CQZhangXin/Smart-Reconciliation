package com.recon.module.rule.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.rule.entity.ReconRuleConfig;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReconRuleConfigMapper extends BaseMapper<ReconRuleConfig> {

    @Select("SELECT * FROM recon_rule_config WHERE org_id = #{orgId} AND status = 'ACTIVE' AND deleted = 0 ORDER BY priority DESC")
    List<ReconRuleConfig> selectActiveRules(@Param("orgId") Long orgId);
}
