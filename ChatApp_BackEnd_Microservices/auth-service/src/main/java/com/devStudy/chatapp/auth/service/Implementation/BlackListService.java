package com.devStudy.chatapp.auth.service.Implementation;

import com.devStudy.chatapp.auth.service.Interface.IBlackListService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BlackListService implements IBlackListService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${chatroomApp.redis.BLACKLIST_PREFIX}")
    private String BLACKLIST_PREFIX;

    public BlackListService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addTokenToBlackList(String token, Long expirationTime) {
        long ttl = expirationTime - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blackListedToken", ttl, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isTokenInBlackList(String token) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
    }
}