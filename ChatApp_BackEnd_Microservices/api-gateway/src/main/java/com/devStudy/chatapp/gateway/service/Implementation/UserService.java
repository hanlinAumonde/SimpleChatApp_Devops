package com.devStudy.chatapp.gateway.service.Implementation;

import com.devStudy.chatapp.gateway.dto.UserInfo;
import com.devStudy.chatapp.gateway.service.Interface.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserService implements IUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    
    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    @Autowired
    public UserService(WebClient.Builder webClientBuilder,
                      ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder
                .baseUrl("http://auth-service")
                .build();
        this.circuitBreaker = circuitBreakerFactory.create("auth-service");
    }

    /**
     * Obtain user information by email from the authentication service
     * Uses a circuit breaker to handle failures gracefully
     */
    public Mono<UserInfo> getUserByEmail(String email) {
        return circuitBreaker.run(
            webClient.get()
                .uri("/api/auth/user-info?email={email}", email)
                .retrieve()
                .bodyToMono(UserInfo.class)
                .doOnError(error -> LOGGER.error("Error fetching user info for email {}: {}", 
                    email, error.getMessage())),
            throwable -> {
                LOGGER.warn("Fallback triggered for email {}: {}", email, throwable.getMessage());
                return Mono.just(createFallbackUserInfo(email));
            }
        );
    }

    private UserInfo createFallbackUserInfo(String email) {
        UserInfo fallbackUser = new UserInfo();
        fallbackUser.setMail(email);
        return fallbackUser;
    }
}