package com.devStudy.chatapp.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(stringRedisSerializer)
                .value(stringRedisSerializer)
                .hashKey(stringRedisSerializer)  // 添加hashKey序列化器
                .hashValue(stringRedisSerializer)  // 添加hashValue序列化器
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}