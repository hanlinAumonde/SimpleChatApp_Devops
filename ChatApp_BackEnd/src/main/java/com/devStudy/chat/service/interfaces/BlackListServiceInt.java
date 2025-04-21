package com.devStudy.chat.service.interfaces;

public interface BlackListServiceInt {
    void addTokenToBlackList(String token, Long expirationTime);
    boolean isTokenInBlackList(String token);
}
