 package com.aidemo.service.service;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.ai.chat.client.ChatClient;
 import org.springframework.cloud.client.ServiceInstance;
 import org.springframework.cloud.client.discovery.DiscoveryClient;
 import org.springframework.data.redis.core.RedisTemplate;
 import org.springframework.jdbc.core.JdbcTemplate;
 import org.springframework.stereotype.Service;
 
 import java.time.Duration;
 import java.util.*;
 import java.util.concurrent.TimeUnit;
 
 @Service
 public class AITestService2 {
 
     private static final Logger log = LoggerFactory.getLogger(AITestService2.class);
 
     private final ChatClient chatClient;
     private final RedisTemplate<String, Object> redisTemplate;
     private final DiscoveryClient discoveryClient;
     private final JdbcTemplate jdbcTemplate;
 
     public AITestService2(ChatClient.Builder chatClientBuilder,
                            RedisTemplate<String, Object> redisTemplate,
                            DiscoveryClient discoveryClient,
                            JdbcTemplate jdbcTemplate) {
         this.chatClient = chatClientBuilder.build();
         this.redisTemplate = redisTemplate;
         this.discoveryClient = discoveryClient;
         this.jdbcTemplate = jdbcTemplate;
     }
 
     public Map<String, Object> runFullTest() {
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("timestamp", System.currentTimeMillis());
         result.put("service", "ai-demo-service");
         result.put("step1_redis", testRedis());
         result.put("step2_nacos", testNacos());
         result.put("step3_ai_model", testAiModel());
         result.put("step4_full_flow", testFullFlow());
         result.put("step5_mysql", testMySQL());
 
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
 
     public Map<String, Object> testRedis() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "Redis");
         try {
             long start = System.currentTimeMillis();
             redisTemplate.opsForValue().set("__test:ping", "pong", 10, TimeUnit.SECONDS);
             String value = (String) redisTemplate.opsForValue().get("__test:ping");
             long elapsed = System.currentTimeMillis() - start;
             if ("pong".equals(value)) {
                 m.put("status", "PASS"); m.put("latency_ms", elapsed);
             } else {
                 m.put("status", "FAIL");
             }
         } catch (Exception e) {
             m.put("status", "ERROR"); m.put("detail", e.getMessage());
         }
         return m;
     }
 
     public Map<String, Object> testNacos() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "Nacos");
         try {
             List<String> services = discoveryClient.getServices();
             m.put("status", "PASS");
             List<ServiceInstance> instances = discoveryClient.getInstances("ai-demo-service");
             List<Map<String, Object>> instList = new ArrayList<>();
             for (ServiceInstance inst : instances) {
                 Map<String, Object> im = new LinkedHashMap<>();
                 im.put("host", inst.getHost()); im.put("port", inst.getPort());
                 im.put("serviceId", inst.getServiceId()); im.put("uri", inst.getUri().toString());
                 instList.add(im);
             }
             m.put("instances", instList);
         } catch (Exception e) {
             m.put("status", "ERROR"); m.put("detail", e.getMessage());
         }
         return m;
     }
 
     public Map<String, Object> testAiModel() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "AI Model");
         try {
             long start = System.currentTimeMillis();
             String response = chatClient.prompt()
                     .user("Say: I am working. Reply in English.")
                     .call()
                     .content();
             long elapsed = System.currentTimeMillis() - start;
             if (response != null && !response.isBlank()) {
                 m.put("status", "PASS"); m.put("latency_ms", elapsed);
                 m.put("preview", response.substring(0, Math.min(response.length(), 100)));
             } else {
                 m.put("status", "FAIL");
             }
         } catch (Exception e) {
             m.put("status", "ERROR"); m.put("detail", e.getMessage());
         }
         return m;
     }
 
     public Map<String, Object> testFullFlow() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "Full Flow");
         try {
             String testPrompt = "Count from 1 to 3";
             String cacheKey = "__test:fullflow:" + testPrompt.hashCode();
             long start = System.currentTimeMillis();
             String aiResponse = chatClient.prompt().user(testPrompt).call().content();
             long aiElapsed = System.currentTimeMillis() - start;
             if (aiResponse == null || aiResponse.isBlank()) {
                 m.put("status", "FAIL"); return m;
             }
             long cacheStart = System.currentTimeMillis();
             redisTemplate.opsForValue().set(cacheKey, aiResponse, 30, TimeUnit.SECONDS);
             String cachedResult = (String) redisTemplate.opsForValue().get(cacheKey);
             long cacheElapsed = System.currentTimeMillis() - cacheStart;
             if (aiResponse.equals(cachedResult)) {
                 m.put("status", "PASS"); m.put("ai_ms", aiElapsed); m.put("cache_ms", cacheElapsed);
             } else {
                 m.put("status", "FAIL");
             }
         } catch (Exception e) {
             m.put("status", "ERROR"); m.put("detail", e.getMessage());
         }
         return m;
     }
 
     public Map<String, Object> testMySQL() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", "MySQL");
         try {
             long start = System.currentTimeMillis();
             Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
             long elapsed = System.currentTimeMillis() - start;
             if (result != null && result == 1) {
                 m.put("status", "PASS"); m.put("latency_ms", elapsed);
                 String db = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
                 m.put("database", db);
                 String ver = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
                 m.put("version", ver);
             } else {
                 m.put("status", "FAIL");
             }
         } catch (Exception e) {
             m.put("status", "ERROR"); m.put("detail", e.getMessage());
         }
         return m;
     }
 }
