package com.recon.module.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recon.module.system.entity.SysAuditLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
