package com.devStudy.chatapp.auth.service.Implementation;

import com.devStudy.chatapp.auth.service.Interface.IBlackListService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.devStudy.chatapp.auth.utils.ConstantValues.BLACKLIST_PREFIX;

@Service
public class BlackListService implements IBlackListService {

    private final RedisTemplate<String, Object> redisTemplate;

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