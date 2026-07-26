 package com.aidemo.service.config;
 
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.SerializationFeature;
 import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.data.redis.connection.RedisConnectionFactory;
 import org.springframework.data.redis.core.RedisTemplate;
 import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
 import org.springframework.data.redis.serializer.StringRedisSerializer;
 
 @Configuration
 public class RedisConfig {
 
     @Bean
     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
         RedisTemplate<String, Object> template = new RedisTemplate<>();
         template.setConnectionFactory(redisConnectionFactory);
 
         ObjectMapper objectMapper = new ObjectMapper();
         objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
         objectMapper.registerModule(new JavaTimeModule());
 
         GenericJackson2JsonRedisSerializer serializer =
                 new GenericJackson2JsonRedisSerializer(objectMapper);
         StringRedisSerializer stringSerializer = new StringRedisSerializer();
 
         template.setKeySerializer(stringSerializer);
         template.setHashKeySerializer(stringSerializer);
         template.setValueSerializer(serializer);
         template.setHashValueSerializer(serializer);
         template.afterPropertiesSet();
         return template;
     }
 }
