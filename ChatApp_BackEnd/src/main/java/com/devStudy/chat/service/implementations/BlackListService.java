package com.devStudy.chat.service.implementations;

import com.devStudy.chat.service.interfaces.BlackListServiceInt;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlackListService implements BlackListServiceInt {

    //TODO : Use Redis to store blacklisted tokens
    private static final Map<String, Long> blackListWithExpirationTime = new ConcurrentHashMap<>();

    @Override
    public void addTokenToBlackList(String token, Long expirationTime) {
        blackListWithExpirationTime.put(token, expirationTime);
    }

    @Override
    public boolean isTokenInBlackList(String token) {
        return blackListWithExpirationTime.containsKey(token);
    }

    @Scheduled(fixedDelayString = "${chatroomApp.blackListService.clearExpiredTokensInterval}")
    private void clearExpiredTokens(){
        blackListWithExpirationTime.entrySet()
                .removeIf(entry -> entry.getValue() <= System.currentTimeMillis());
    }
}
