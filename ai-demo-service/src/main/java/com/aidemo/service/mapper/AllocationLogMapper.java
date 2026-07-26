 package com.aidemo.service.mapper;
import com.aidemo.service.entity.AllocationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
@Mapper
 public interface AllocationLogMapper {
    @Insert("INSERT INTO wms_allocation_log (order_no, step_name, status, detail, start_at, end_at) VALUES (#{orderNo}, #{stepName}, #{status}, #{detail}, #{startAt}, #{endAt})")
    int insertLog(AllocationLog log);
 }
