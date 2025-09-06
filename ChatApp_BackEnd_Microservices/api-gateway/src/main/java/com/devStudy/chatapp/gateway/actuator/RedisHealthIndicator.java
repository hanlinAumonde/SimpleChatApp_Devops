package com.devStudy.chatapp.gateway.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    public RedisHealthIndicator(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Health> health() {
        return getHealth(reactiveRedisTemplate);
    }

    private static Mono<Health> getHealth(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        return reactiveRedisTemplate.createMono(ReactiveRedisConnection::ping)
                .map(ping -> {
                    if ("PONG".equals(ping)) {
                        return Health.up()
                                .withDetail("redis", "Connected")
                                .withDetail("status", "UP")
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("redis", "Unexpected ping response: " + ping)
                                .build();
                    }
                })
                .onErrorResume(e ->
                        Mono.just(
                            Health.down()
                            .withDetail("redis", "Connection failed")
                            .withDetail("error", e.getMessage())
                            .build()
                        )
                );
    }
}