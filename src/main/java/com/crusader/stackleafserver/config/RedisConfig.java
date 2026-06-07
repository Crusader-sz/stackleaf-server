package com.crusader.stackleafserver.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    // Key 统一使用字符串序列化器，保证可读性和兼容性
    private final StringRedisSerializer stringSerializer = new StringRedisSerializer();
    // Value 使用 Jackson JSON 序列化器，支持对象类型和泛型
    private final Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();

    /**
     * 创建并配置 Jackson2JsonRedisSerializer
     * - 注册 Java 8 时间模块，支持 LocalDateTime 等类型
     * - 自动检测所有属性的 getter/setter
     * - 启用默认类型信息，解决反序列化时的类型擦除问题（使用安全验证器）
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 支持 Java 8 日期时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 自动检测所有属性（字段、getter、setter）的可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 在 JSON 中存储类型信息（@class），用于反序列化时还原具体类型
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }

    /**
     * 配置 RedisTemplate
     * - Key / HashKey 使用 String 序列化器，便于 Redis 命令行查看和调试
     * - Value / HashValue 使用 Jackson JSON 序列化器，支持存储复杂对象
     */
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // 设置 Key 和 HashKey 的序列化器
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        // 设置 Value 和 HashValue 的序列化器
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }

    /**
     * 配置 RedisCacheManager（用于 Spring Cache 抽象）
     * - 缓存默认过期时间：6 小时
     * - Key 序列化：StringRedisSerializer
     * - Value 序列化：Jackson2JsonRedisSerializer
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))// 设置全局缓存过期时间
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
