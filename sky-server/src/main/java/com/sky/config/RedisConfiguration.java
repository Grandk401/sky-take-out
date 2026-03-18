package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("创建RedisTemplate对象");
        // 创建RedisTemplate对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置RedisConnectionFactory
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置默认序列化器
        redisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return redisTemplate;
    }
}
