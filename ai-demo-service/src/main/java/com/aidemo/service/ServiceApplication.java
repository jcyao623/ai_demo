 package com.aidemo.service;
 
 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
 
 @SpringBootApplication(scanBasePackages = "com.aidemo")
 @EnableDiscoveryClient
 public class ServiceApplication {
     public static void main(String[] args) {
         SpringApplication.run(ServiceApplication.class, args);
     }
 }
