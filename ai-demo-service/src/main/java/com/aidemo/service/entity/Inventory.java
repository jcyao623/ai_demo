 package com.aidemo.service.entity;
 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 @Data @TableName("wms_inventory")
 public class Inventory {
     @TableId(type = IdType.AUTO) private Long id;
     private Long warehouseId;
     private String sku;
     private String productName;
     private Integer totalQty;
     private Integer allocatedQty;
 }
