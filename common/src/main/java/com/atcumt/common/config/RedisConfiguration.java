package com.atcumt.common.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnClass({RedisConnectionFactory.class})
public class RedisConfiguration {
    @Value("${sa-token.alone-redis.database:2}")
    private int saRedisDatabase;
    @Value("${sa-token.alone-redis.host:redis}")
    private String saRedisHost;
    @Value("${sa-token.alone-redis.port:6379}")
    private int saRedisPort;
    @Value("${sa-token.alone-redis.password}")
    private String saRedisPassword;
    @Value("${sa-token.alone-redis.timeout:2000}")
    private int saRedisTimeout;

    @Bean
    @Primary
    public <K, V> RedisTemplate<K, V> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return getKvRedisTemplate(redisConnectionFactory);
    }

    @Bean
    public RedisTemplate<String, byte[]> byteRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean(name = "saRedisTemplate")
    public <K, V> RedisTemplate<K, V> saRedisTemplate() {
        // 创建 RedisStandaloneConfiguration，配置单机模式
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(saRedisHost);
        redisConfig.setPort(saRedisPort);
        redisConfig.setDatabase(saRedisDatabase);
        if (saRedisPassword != null && !saRedisPassword.isEmpty()) {
            redisConfig.setPassword(saRedisPassword);
        }

        // 创建 LettuceConnectionFactory
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfig);
        lettuceConnectionFactory.afterPropertiesSet(); // 初始化工厂

        // 配置 RedisTemplate
        return getKvRedisTemplate(lettuceConnectionFactory);
    }

    @NotNull
    private <K, V> RedisTemplate<K, V> getKvRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<K, V> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}