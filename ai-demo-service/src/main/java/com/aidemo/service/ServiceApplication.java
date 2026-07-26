 package com.aidemo.service;
 
 import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;
 
@SpringBootApplication(scanBasePackages = "com.aidemo")
@MapperScan("com.aidemo.service.mapper")
 @EnableDiscoveryClient
 public class ServiceApplication {
     public static void main(String[] args) {
         SpringApplication.run(ServiceApplication.class, args);
     }
 }
