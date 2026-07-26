 package com.aidemo.service.controller;
 
 import com.aidemo.common.dto.Result;
 import com.aidemo.service.service.ChatService;
 import org.springframework.data.redis.core.RedisTemplate;
 import org.springframework.web.bind.annotation.*;
 
 import java.util.Set;
 
 @RestController
 @RequestMapping("/api/ai")
 public class ChatController {
 
     private final ChatService chatService;
     private final RedisTemplate<String, Object> redisTemplate;
 
     public ChatController(ChatService chatService,
                            RedisTemplate<String, Object> redisTemplate) {
         this.chatService = chatService;
         this.redisTemplate = redisTemplate;
     }
 
     @PostMapping("/chat")
     public Result<String> chat(@RequestBody String prompt) {
         return chatService.chat(prompt);
     }
 
     @GetMapping("/health")
     public Result<String> health() {
         return Result.success("AI Service is running");
     }
 
     @GetMapping("/redis/ping")
     public Result<String> redisPing() {
         try {
             redisTemplate.opsForValue().set("demo:ping", "pong");
             String pong = (String) redisTemplate.opsForValue().get("demo:ping");
             return Result.success(pong);
         } catch (Exception e) {
             return Result.error(500, "Redis connection failed: " + e.getMessage());
         }
     }
 
     @GetMapping("/redis/keys")
     public Result<Set<String>> redisKeys(@RequestParam(defaultValue = "*") String pattern) {
         Set<String> keys = redisTemplate.keys(pattern);
         return Result.success(keys);
     }
 }
