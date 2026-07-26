 package com.aidemo.service.entity;
 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 @Data @TableName("wms_warehouse")
 public class Warehouse {
     @TableId(type = IdType.AUTO) private Long id;
     private String code;
     private String name;
     private Integer priority;
     private String status;
 }
