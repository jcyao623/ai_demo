 package com.aidemo.service.entity;
 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.time.LocalDateTime;
 @Data @TableName("wms_allocation_order")
 public class AllocationOrder {
     @TableId(type = IdType.AUTO) private Long id;
     private String orderNo;
     private String sku;
     private String productName;
     private Integer requiredQty;
     private Integer allocatedQty;
     private String status;
     private String flowLog;
     private LocalDateTime createdAt;
     private LocalDateTime updatedAt;
 }
