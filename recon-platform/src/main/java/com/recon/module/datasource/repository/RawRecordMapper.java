package com.recon.module.datasource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.datasource.entity.RawRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RawRecordMapper extends BaseMapper<RawRecord> {

    @Select("SELECT * FROM raw_record WHERE source_id = #{sourceId} AND status = 'PENDING' ORDER BY id LIMIT #{limit}")
    List<RawRecord> selectPendingRecords(@Param("sourceId") Long sourceId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM raw_record WHERE source_id = #{sourceId} AND status = 'PENDING'")
    int countPendingRecords(@Param("sourceId") Long sourceId);
}
