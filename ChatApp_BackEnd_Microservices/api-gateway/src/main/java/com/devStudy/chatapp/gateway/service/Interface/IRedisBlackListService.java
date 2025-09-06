package com.devStudy.chatapp.gateway.service.Interface;

import reactor.core.publisher.Mono;

public interface IRedisBlackListService {
    public Mono<Boolean> isTokenInBlackList(String token);

    public Mono<Boolean> addTokenToBlackList(String token, long expirationTime);
}
