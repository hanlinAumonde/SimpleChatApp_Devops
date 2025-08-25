package com.devStudy.chatapp.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.devStudy.chatapp.auth.utils.ConstantValues.BLACKLIST_PREFIX;

@Service
public class BlackListService {

    private final RedisTemplate<String, Object> redisTemplate;

    public BlackListService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addTokenToBlackList(String token, Long expirationTime) {
        long ttl = expirationTime - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blackListedToken", ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isTokenInBlackList(String token) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
    }
}