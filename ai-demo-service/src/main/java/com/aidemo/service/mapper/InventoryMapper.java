 package com.aidemo.service.mapper;
 import com.aidemo.service.entity.Inventory;
 import com.baomidou.mybatisplus.core.mapper.BaseMapper;
 import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import java.util.List;
 import java.util.Map;
 @Mapper
 public interface InventoryMapper extends BaseMapper<Inventory> {
    @Select("SELECT w.code, w.name, i.total_qty, i.allocated_qty, (i.total_qty - i.allocated_qty) AS avail " +
            "FROM wms_inventory i JOIN wms_warehouse w ON i.warehouse_id = w.id WHERE i.sku = #{sku} ORDER BY w.priority")
    List<Map<String, Object>> selectStockBySku(String sku);
 
    @Update("UPDATE wms_inventory SET allocated_qty = allocated_qty + #{qty} WHERE sku = #{sku} AND warehouse_id = (SELECT id FROM wms_warehouse WHERE code = #{warehouseCode})")
    int updateAllocatedQty(@Param("sku") String sku, @Param("warehouseCode") String warehouseCode, @Param("qty") int qty);
 }
