 package com.aidemo.service.controller;
 
import com.aidemo.common.dto.Result;
import com.aidemo.service.service.AITestService2;
import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.web.bind.annotation.*;
 
 import java.util.Map;
 
 @RestController
 @RequestMapping("/api/ai/test")
 public class TestController {
 
    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final AITestService2 aiTestService;
 
    public TestController(AITestService2 aiTestService) {
         this.aiTestService = aiTestService;
     }
 
     /** 全流程连通性测试 */
     @PostMapping
    public Result<Map<String, Object>> runFullTest() {
        log.info("Running full connectivity test...");
        Map<String, Object> result = aiTestService.runFullTest();
        log.info("Full test completed: {}", result.get("overall"));
        return Result.success(result);
    }
 
     /** 仅测试 Redis */
     @PostMapping("/redis")
     public Result<Map<String, Object>> testRedis() {
         return Result.success(aiTestService.testRedis());
     }
 
     /** 仅测试 Nacos */
     @PostMapping("/nacos")
     public Result<Map<String, Object>> testNacos() {
         return Result.success(aiTestService.testNacos());
     }
 
     /** 仅测试 AI 模型 */
     @PostMapping("/ai")
     public Result<Map<String, Object>> testAi() {
         return Result.success(aiTestService.testAiModel());
     }
 
     /** 仅测试全链路 */
    @PostMapping("/fullflow")
    public Result<Map<String, Object>> testFullFlow() {
        return Result.success(aiTestService.testFullFlow());
    }
 
    @PostMapping("/mysql")
    public Result<Map<String, Object>> testMySQL() {
        return Result.success(aiTestService.testMySQL());
    }
 }
