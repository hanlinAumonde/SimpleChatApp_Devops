package com.devStudy.chatapp.gateway.service.Implementation;

import com.devStudy.chatapp.gateway.dto.UserInfo;
import com.devStudy.chatapp.gateway.service.Interface.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserService implements IUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    
    private final WebClient webClient;

    @Autowired
    public UserService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://auth-service")
                .build();
    }

    /**
     * 通过邮箱获取用户信息
     */
    public Mono<UserInfo> getUserByEmail(String email) {
        return webClient.get()
                .uri("/api/auth/user-info?email={email}", email)
                .retrieve()
                .bodyToMono(UserInfo.class)
                .doOnError(error -> LOGGER.error("Error fetching user info for email {}: {}", 
                    email, error.getMessage()))
                .onErrorReturn(new UserInfo()); // 返回空用户信息作为默认值
    }
}