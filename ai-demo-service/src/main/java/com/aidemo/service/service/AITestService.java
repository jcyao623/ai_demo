 package com.aidemo.service.service;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.ai.chat.client.ChatClient;
 import org.springframework.cloud.client.ServiceInstance;
 import org.springframework.cloud.client.discovery.DiscoveryClient;
 import org.springframework.data.redis.core.RedisTemplate;
 import org.springframework.stereotype.Service;
 
 import java.time.Duration;
 import java.util.*;
 import java.util.concurrent.TimeUnit;
 
 @Service
 public class AITestService {
 
     private static final Logger log = LoggerFactory.getLogger(AITestService.class);
 
     private final ChatClient chatClient;
     private final RedisTemplate<String, Object> redisTemplate;
     private final DiscoveryClient discoveryClient;
 
     public AITestService(ChatClient.Builder chatClientBuilder,
                           RedisTemplate<String, Object> redisTemplate,
                           DiscoveryClient discoveryClient) {
         this.chatClient = chatClientBuilder.build();
         this.redisTemplate = redisTemplate;
         this.discoveryClient = discoveryClient;
     }
 
     /** Run full connectivity test suite */
     public Map<String, Object> runFullTest() {
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("timestamp", System.currentTimeMillis());
         result.put("service", "ai-demo-service");
 
         // Step 1: Redis
         result.put("step1_redis", testRedis());
 
         // Step 2: Nacos
         result.put("step2_nacos", testNacos());
 
         // Step 3: AI Model
         result.put("step3_ai_model", testAiModel());
 
         // Step 4: Full flow (Redis -> AI -> Cache)
         result.put("step4_full_flow", testFullFlow());
 
         // Overall
         boolean allPassed = true;
         for (Map.Entry<String, Object> entry : result.entrySet()) {
             if (entry.getValue() instanceof Map) {
                 Map<?, ?> m = (Map<?, ?>) entry.getValue();
                 Object status = m.get("status");
                 if ("FAIL".equals(status) || "ERROR".equals(status)) {
                     allPassed = false;
                     break;
                 }
             }
         }
         result.put("overall", allPassed ? "PASS" : "FAIL");
         return result;
     }
 
     /** Test 1: Redis connectivity */
     public Map<String, Object> testRedis() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "Redis 连通性测试");
         try {
             long start = System.currentTimeMillis();
             redisTemplate.opsForValue().set("__test:ping", "pong", 10, TimeUnit.SECONDS);
             String value = (String) redisTemplate.opsForValue().get("__test:ping");
             long elapsed = System.currentTimeMillis() - start;
 
             if ("pong".equals(value)) {
                 m.put("status", "PASS");
                 m.put("detail", "Redis 读写正常");
                 m.put("latency_ms", elapsed);
             } else {
                 m.put("status", "FAIL");
                 m.put("detail", "Redis 返回值异常: " + value);
             }
         } catch (Exception e) {
             m.put("status", "ERROR");
             m.put("detail", "Redis 连接失败: " + e.getMessage());
             log.warn("Redis test failed", e);
         }
         return m;
     }
 
     /** Test 2: Nacos service discovery */
     public Map<String, Object> testNacos() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "Nacos 注册中心测试");
         try {
             List<String> services = discoveryClient.getServices();
             m.put("status", "PASS");
             m.put("detail", "Nacos 服务发现正常");
             m.put("services", services);
 
             // Show instances for this service
             List<ServiceInstance> instances = discoveryClient.getInstances("ai-demo-service");
             List<Map<String, Object>> instanceList = new ArrayList<>();
             for (ServiceInstance inst : instances) {
                 Map<String, Object> im = new LinkedHashMap<>();
                 im.put("host", inst.getHost());
                 im.put("port", inst.getPort());
                 im.put("serviceId", inst.getServiceId());
                 im.put("uri", inst.getUri().toString());
                 instanceList.add(im);
             }
             m.put("instances", instanceList);
         } catch (Exception e) {
             m.put("status", "ERROR");
             m.put("detail", "Nacos 连接失败: " + e.getMessage());
             log.warn("Nacos test failed", e);
         }
         return m;
     }
 
     /** Test 3: AI model connectivity */
     public Map<String, Object> testAiModel() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "AI 模型（千问 Qwen）连通性测试");
         try {
             long start = System.currentTimeMillis();
             String response = chatClient.prompt()
                     .user("请用一句话回答：你好吗？请回复'我很好，AI服务连接正常！'")
                     .call()
                     .content();
             long elapsed = System.currentTimeMillis() - start;
 
             if (response != null && !response.isBlank()) {
                 m.put("status", "PASS");
                 m.put("detail", "AI 模型响应正常");
                 m.put("latency_ms", elapsed);
                 m.put("response_preview", response.substring(0, Math.min(response.length(), 100)));
             } else {
                 m.put("status", "FAIL");
                 m.put("detail", "AI 模型返回空响应");
             }
         } catch (Exception e) {
             m.put("status", "ERROR");
             m.put("detail", "AI 模型调用失败: " + e.getMessage());
             log.warn("AI model test failed", e);
         }
         return m;
     }
 
     /** Test 4: Full flow - write to Redis, call AI, cache result */
     public Map<String, Object> testFullFlow() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "全链路集成测试 (Redis → AI → Cache)");
         try {
             // Step A: Redis write cache key
             String testPrompt = "从1数到3";
             String cacheKey = "__test:fullflow:" + testPrompt.hashCode();
             long start = System.currentTimeMillis();
 
             // Step B: Call AI
             String aiResponse = chatClient.prompt()
                     .user(testPrompt)
                     .call()
                     .content();
             long aiElapsed = System.currentTimeMillis() - start;
 
             if (aiResponse == null || aiResponse.isBlank()) {
                 m.put("status", "FAIL");
                 m.put("detail", "AI 返回空，后续流程终止");
                 return m;
             }
 
             // Step C: Cache to Redis
             long cacheStart = System.currentTimeMillis();
             redisTemplate.opsForValue().set(cacheKey, aiResponse, 30, TimeUnit.SECONDS);
             String cachedResult = (String) redisTemplate.opsForValue().get(cacheKey);
             long cacheElapsed = System.currentTimeMillis() - cacheStart;
 
             if (aiResponse.equals(cachedResult)) {
                 m.put("status", "PASS");
                 m.put("detail", "全链路正常: AI 响应 → Redis 缓存");
                 m.put("ai_latency_ms", aiElapsed);
                 m.put("cache_latency_ms", cacheElapsed);
                 m.put("response_preview", aiResponse.substring(0, Math.min(aiResponse.length(), 100)));
             } else {
                 m.put("status", "FAIL");
                 m.put("detail", "缓存读回内容不一致");
             }
         } catch (Exception e) {
             m.put("status", "ERROR");
             m.put("detail", "全链路测试失败: " + e.getMessage());
             log.warn("Full flow test failed", e);
         }
         return m;
     }
 }
