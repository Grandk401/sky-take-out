package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class RedisConfiguration {

    /**
     * 创建统一的Jackson序列化器（修复LocalDateTime兼容性问题）
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        // 核心：注册Java8时间模块，修复LocalDateTime报错
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("创建RedisTemplate对象");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setDefaultSerializer(createJsonSerializer());
        return redisTemplate;
    }

    /**
     * 配置Redis缓存管理器（用于Spring Cache注解方式操作Redis，如套餐模块的@Cacheable）
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("创建RedisCacheManager对象");

        // 使用与redisTemplate一致的序列化器
        Jackson2JsonRedisSerializer<Object> serializer = createJsonSerializer();

        // 基础配置：Key使用String序列化 + 允许空值缓存（与菜品手动缓存的防穿透策略保持一致）
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        // 分类缓存：24h TTL（分类数据几乎不变，适合长缓存）
        RedisCacheConfiguration categoryConfig = baseConfig.entryTtl(Duration.ofHours(24));

        // 套餐缓存：5h TTL（套餐数据变动稍频繁）
        RedisCacheConfiguration setmealConfig = baseConfig.entryTtl(Duration.ofHours(5));

        // 按 cacheName 注册差异化配置
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put("categoryCache", categoryConfig);
        configMap.put("setmealCache", setmealConfig);

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(configMap)
                .build();
    }
}
