package com.recon.module.engine.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.engine.entity.ReconMatch;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReconMatchMapper extends BaseMapper<ReconMatch> {

    @Select("SELECT * FROM recon_match WHERE task_id = #{taskId} AND status = 'AUTO_CONFIRMED'")
    List<ReconMatch> selectAutoConfirmedMatches(@Param("taskId") Long taskId);

    @Select("SELECT * FROM recon_match WHERE task_id = #{taskId} AND status = 'PENDING_REVIEW'")
    List<ReconMatch> selectPendingReviewMatches(@Param("taskId") Long taskId);
}
