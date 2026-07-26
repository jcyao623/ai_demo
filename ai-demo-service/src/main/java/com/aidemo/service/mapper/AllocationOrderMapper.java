 package com.aidemo.service.mapper;
 import com.aidemo.service.entity.AllocationOrder;
 import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
@Mapper
 public interface AllocationOrderMapper extends BaseMapper<AllocationOrder> {
    @Update("UPDATE wms_allocation_order SET status = #{status}, allocated_qty = #{allocatedQty} WHERE order_no = #{orderNo}")
    int updateStatus(@Param("orderNo") String orderNo, @Param("status") String status, @Param("allocatedQty") Integer allocatedQty);
 }
