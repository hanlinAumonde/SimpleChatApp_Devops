package com.devStudy.chatapp.gateway.filter;

import com.devStudy.chatapp.gateway.service.JwtTokenService;
import com.devStudy.chatapp.gateway.service.RedisBlackListService;
import com.devStudy.chatapp.gateway.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AuthenticationGatewayFilter extends AbstractGatewayFilterFactory<AuthenticationGatewayFilter.Config> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationGatewayFilter.class);

    private final JwtTokenService jwtTokenService;
    private final RedisBlackListService blackListService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthenticationGatewayFilter(JwtTokenService jwtTokenService,
                                     RedisBlackListService blackListService,
                                     UserService userService,
                                     ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtTokenService = jwtTokenService;
        this.blackListService = blackListService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = jwtTokenService.getTokenFromRequest(exchange);
            
            if (token == null) {
                return handleUnauthorized(exchange, "JWT token not found");
            }

            // 验证token是否在黑名单中
            return blackListService.isTokenInBlackList(token)
                    .flatMap(isBlacklisted -> {
                        if (isBlacklisted) {
                            return handleUnauthorized(exchange, "JWT token is blacklisted");
                        }
                        
                        // 验证token并获取邮箱
                        String email = jwtTokenService.validateTokenAndGetEmail(token);
                        if (email == null) {
                            // Token无效，加入黑名单
                            return blackListService.addTokenToBlackList(token, 
                                    jwtTokenService.getExpirationDate(token) != null ? 
                                    jwtTokenService.getExpirationDate(token).getTime() : 
                                    System.currentTimeMillis())
                                    .then(handleUnauthorized(exchange, "Invalid JWT token"));
                        }

                        // 获取用户信息并添加到请求头
                        return userService.getUserByEmail(email)
                                .flatMap(userInfo -> {
                                    if (userInfo.getId() == null) {
                                        // 用户不存在，将token加入黑名单
                                        return blackListService.addTokenToBlackList(token,
                                                jwtTokenService.getExpirationDate(token).getTime())
                                                .then(handleUnauthorized(exchange, "User not found"));
                                    }

                                    // 添加用户信息到请求头
                                    ServerHttpRequest modifiedRequest = exchange.getRequest()
                                            .mutate()
                                            .header("X-User-Id", userInfo.getId().toString())
                                            .header("X-User-Email", userInfo.getEmail())
                                            .header("X-User-Name", userInfo.getUsername())
                                            .header("X-User-Role", userInfo.getRole())
                                            .build();

                                    ServerWebExchange modifiedExchange = exchange.mutate()
                                            .request(modifiedRequest)
                                            .build();

                                    LOGGER.debug("Authentication successful for user: {}", email);
                                    return chain.filter(modifiedExchange);
                                });
                    });
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "Unauthorized",
                "detail", message,
                "isAuthenticated", false
        );

        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error creating JSON response", e);
            return response.setComplete();
        }
    }

    public static class Config {
        // 配置类，暂时为空
    }
}