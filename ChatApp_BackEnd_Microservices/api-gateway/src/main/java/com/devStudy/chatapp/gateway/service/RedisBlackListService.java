package com.devStudy.chatapp.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
public class RedisBlackListService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBlackListService.class);
    
    // 与认证服务保持一致的前缀
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    public RedisBlackListService(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * 检查Token是否在黑名单中
     */
    public Mono<Boolean> isTokenInBlackList(String token) {
        String key = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.hasKey(key)
                .doOnError(error -> LOGGER.error("Error checking blacklist for token: {}", error.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * 将Token加入黑名单
     */
    public Mono<Boolean> addTokenToBlackList(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        long currentTime = Instant.now().toEpochMilli();
        
        if (expirationTime <= currentTime) {
            LOGGER.debug("Token already expired, no need to blacklist");
            return Mono.just(true);
        }
        
        Duration ttl = Duration.ofMillis(expirationTime - currentTime);
        
        return reactiveRedisTemplate.opsForValue()
                .set(key, "blackListedToken", ttl)
                .doOnSuccess(result -> {
                    if (result) {
                        LOGGER.debug("Token added to blacklist successfully");
                    } else {
                        LOGGER.warn("Failed to add token to blacklist");
                    }
                })
                .doOnError(error -> LOGGER.error("Error adding token to blacklist: {}", error.getMessage()))
                .onErrorReturn(false);
    }
}