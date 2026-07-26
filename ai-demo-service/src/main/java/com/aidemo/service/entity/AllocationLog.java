 package com.aidemo.service.entity;
 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.time.LocalDateTime;
 @Data @TableName("wms_allocation_log")
 public class AllocationLog {
     @TableId(type = IdType.AUTO) private Long id;
     private String orderNo;
     private String stepName;
     private String status;
     private String detail;
     private LocalDateTime startAt;
     private LocalDateTime endAt;
 }
