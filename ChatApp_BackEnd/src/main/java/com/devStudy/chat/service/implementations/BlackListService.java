package com.devStudy.chat.service.implementations;

import com.devStudy.chat.service.interfaces.BlackListServiceInt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.devStudy.chat.service.utils.ConstantValues.BLACKLIST_PREFIX;

@Service
public class BlackListService implements BlackListServiceInt {

    //private static final Map<String, Long> blackListWithExpirationTime = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;

    public BlackListService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addTokenToBlackList(String token, Long expirationTime) {
        //blackListWithExpirationTime.put(token, expirationTime);
        long ttl = expirationTime - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blackListedToken", ttl, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isTokenInBlackList(String token) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        //return blackListWithExpirationTime.containsKey(token);
    }

}
