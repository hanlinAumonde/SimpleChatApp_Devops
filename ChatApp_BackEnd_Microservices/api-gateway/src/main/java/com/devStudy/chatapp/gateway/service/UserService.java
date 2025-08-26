package com.devStudy.chatapp.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserService {
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

    /**
     * 用户信息DTO
     */
    public static class UserInfo {
        private Long id;
        private String email;
        private String username;
        private String role;

        public UserInfo() {}

        public UserInfo(Long id, String email, String username, String role) {
            this.id = id;
            this.email = email;
            this.username = username;
            this.role = role;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}