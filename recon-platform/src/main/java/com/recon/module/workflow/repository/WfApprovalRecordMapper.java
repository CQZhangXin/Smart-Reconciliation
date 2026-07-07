package com.recon.module.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.workflow.entity.WfApprovalRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WfApprovalRecordMapper extends BaseMapper<WfApprovalRecord> {
}
