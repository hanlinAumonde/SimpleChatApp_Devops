package com.devStudy.chatapp.websocket.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        return getHealth(redisTemplate);
    }

    public static Health getHealth(RedisTemplate<String, Object> redisTemplate) {
        try(RedisConnection connection = redisTemplate.getConnectionFactory() != null ?
                redisTemplate.getConnectionFactory().getConnection() : null) {
            if (connection != null) {
                connection.ping();
                connection.close();
                return Health.up()
                            .withDetail("redis", "Connected")
                            .withDetail("status", "UP")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
        return Health.down().withDetail("redis", "Connection factory is null").build();
    }
}