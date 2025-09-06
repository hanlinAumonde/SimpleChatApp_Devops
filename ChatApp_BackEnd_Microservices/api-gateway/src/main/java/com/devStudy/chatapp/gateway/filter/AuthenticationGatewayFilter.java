package com.devStudy.chatapp.gateway.filter;

import com.devStudy.chatapp.gateway.service.Implementation.JwtTokenService;
import com.devStudy.chatapp.gateway.service.Implementation.RedisBlackListService;
import com.devStudy.chatapp.gateway.service.Implementation.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${chatroomApp.request.header.UserID}")
    private String UserIDHeader;

    @Value("${chatroomApp.request.header.UserEmail}")
    private String UserEmailHeader;

    @Value("${chatroomApp.request.header.UserFirstName}")
    private String UserFirstNameHeader;

    @Value("${chatroomApp.request.header.UserLastName}")
    private String UserLastNameHeader;

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

            // Check if token is blacklisted
            return blackListService.isTokenInBlackList(token)
                    .flatMap(isBlacklisted -> {
                        if (isBlacklisted) {
                            return handleUnauthorized(exchange, "JWT token is blacklisted");
                        }
                        
                        // Check token validity and extract email
                        String email = jwtTokenService.validateTokenAndGetEmail(token);
                        if (email == null) {
                            // Invalid token, add to blacklist
                            return blackListService.addTokenToBlackList(token, 
                                    jwtTokenService.getExpirationDate(token) != null ? 
                                    jwtTokenService.getExpirationDate(token).getTime() : 
                                    System.currentTimeMillis())
                                    .then(handleUnauthorized(exchange, "Invalid JWT token"));
                        }

                        // Obtain user info from user service
                        return userService.getUserByEmail(email)
                                .flatMap(userInfo -> {
                                    if (userInfo.getId() == null) {
                                        // User not found, add token to blacklist
                                        return blackListService.addTokenToBlackList(token,
                                                jwtTokenService.getExpirationDate(token).getTime())
                                                .then(handleUnauthorized(exchange, "User not found"));
                                    }

                                    // Add user info to headers
                                    ServerHttpRequest modifiedRequest = exchange.getRequest()
                                            .mutate()
                                            .header(UserIDHeader, userInfo.getId().toString())
                                            .header(UserEmailHeader, userInfo.getMail())
                                            .header(UserFirstNameHeader, userInfo.getFirstName())
                                            .header(UserLastNameHeader, userInfo.getLastName())
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

    public static class Config {}
}