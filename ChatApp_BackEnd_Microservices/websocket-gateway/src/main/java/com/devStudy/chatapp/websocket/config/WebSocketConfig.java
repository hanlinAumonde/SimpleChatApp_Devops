package com.devStudy.chatapp.websocket.config;

import com.devStudy.chatapp.websocket.handler.DistributedChatWebSocketHandler;
import com.devStudy.chatapp.websocket.interceptor.ChatHandShakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${websocket.chat.allowed-origins}")
    private String allowedOrigins;

    @Value("${websocket.chat.endpoint}")
    private String CHAT_ENDPOINT;

    private final DistributedChatWebSocketHandler distributedChatWebSocketHandler;

    private final ChatHandShakeInterceptor chatHandShakeInterceptor;

    @Autowired
    public WebSocketConfig(DistributedChatWebSocketHandler distributedChatWebSocketHandler,
                           ChatHandShakeInterceptor chatHandShakeInterceptor) {
        this.distributedChatWebSocketHandler = distributedChatWebSocketHandler;
        this.chatHandShakeInterceptor = chatHandShakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(distributedChatWebSocketHandler, CHAT_ENDPOINT)
                .addInterceptors(chatHandShakeInterceptor)
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}