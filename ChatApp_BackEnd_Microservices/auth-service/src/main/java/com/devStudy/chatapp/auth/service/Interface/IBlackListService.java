package com.devStudy.chatapp.auth.service.Interface;

public interface IBlackListService {
    void addTokenToBlackList(String token, Long expirationTime);

    boolean isTokenInBlackList(String token);
}
