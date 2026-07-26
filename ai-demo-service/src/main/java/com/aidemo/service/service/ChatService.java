 package com.aidemo.service.service;
 
 import com.aidemo.common.dto.Result;
 import org.springframework.ai.chat.client.ChatClient;
 import org.springframework.data.redis.core.RedisTemplate;
 import org.springframework.stereotype.Service;
 
 import java.util.concurrent.TimeUnit;
 
 @Service
 public class ChatService {
 
     private final ChatClient chatClient;
     private final RedisTemplate<String, Object> redisTemplate;
 
     public ChatService(ChatClient.Builder chatClientBuilder,
                         RedisTemplate<String, Object> redisTemplate) {
         this.chatClient = chatClientBuilder.build();
         this.redisTemplate = redisTemplate;
     }
 
     public Result<String> chat(String prompt) {
         if (prompt == null || prompt.isBlank()) {
             return Result.error(400, "prompt must not be empty");
         }
 
         String cacheKey = "ai:chat:" + prompt.hashCode();
         String cached = (String) redisTemplate.opsForValue().get(cacheKey);
         if (cached != null) {
             return Result.success(cached);
         }
 
         String response = chatClient.prompt()
                 .user(prompt)
                 .call()
                 .content();
 
         redisTemplate.opsForValue().set(cacheKey, response, 1, TimeUnit.HOURS);
         return Result.success(response);
     }
 }
