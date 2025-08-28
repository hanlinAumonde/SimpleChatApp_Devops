package com.devStudy.chatapp.gateway.service.Interface;

import com.devStudy.chatapp.gateway.dto.UserInfo;
import reactor.core.publisher.Mono;

public interface IUserService {
    public Mono<UserInfo> getUserByEmail(String email);
}
