package com.devStudy.chatapp.websocket.interceptor;

import com.devStudy.chatapp.websocket.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

@Component
public class ChatHandShakeInterceptor implements HandshakeInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHandShakeInterceptor.class);

    @Value("${websocket.chat.endpoint}")
    private String CHAT_ENDPOINT;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                   ServerHttpResponse response, 
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            // 1. 解析WebSocket URL参数
            UriTemplate uriTemplate = new UriTemplate(CHAT_ENDPOINT);
            Map<String, String> uriTemplateVars = uriTemplate.match(request.getURI().toString());
            
            if (uriTemplateVars.isEmpty()) {
                LOGGER.warn("Failed to parse WebSocket URL parameters");
                return false;
            }

            String chatroomIdStr = uriTemplateVars.get("chatroomId");
            String userIdFromUrl = cleanUserId(uriTemplateVars.get("userId"));

            // 2. 从API网关传递的HTTP头获取用户信息
            String userId = request.getHeaders().getFirst("X-User-Id");
            String userEmail = request.getHeaders().getFirst("X-User-Email");
            String userFirstName = request.getHeaders().getFirst("X-User-FirstName");
            String userLastName = request.getHeaders().getFirst("X-User-LastName");

            if (userId == null) {
                LOGGER.warn("No user information from gateway - request may not be authenticated");
                return false;
            }

            // 3. 验证URL中的userId与网关传递的userId一致
            if (!userId.equals(userIdFromUrl)) {
                LOGGER.warn("User ID mismatch - URL: {}, Header: {}", userIdFromUrl, userId);
                return false;
            }

            // 4. 构建用户信息对象
            UserDTO userInfo = new UserDTO();
            userInfo.setId(Long.parseLong(userId));
            userInfo.setMail(userEmail);
            userInfo.setFirstName(userFirstName);
            userInfo.setLastName(userLastName);
            userInfo.setAdmin(false);
            userInfo.setActive(true);

            // 5. 存储信息到WebSocket会话属性
            attributes.put("chatroomId", Long.parseLong(chatroomIdStr));
            attributes.put("userId", Long.parseLong(userId));
            attributes.put("userInfo", userInfo);

            LOGGER.info("WebSocket handshake successful for user {} in chatroom {}", userId, chatroomIdStr);
            return true;

        } catch (Exception e) {
            LOGGER.error("WebSocket handshake failed", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, 
                              ServerHttpResponse response, 
                              WebSocketHandler wsHandler, 
                              Exception exception) {
        if (exception != null) {
            LOGGER.error("Error during WebSocket handshake", exception);
        }
    }

    /**
     * 清理用户ID中的查询参数
     */
    private String cleanUserId(String userId) {
        if (userId != null && userId.contains("?")) {
            return userId.substring(0, userId.indexOf("?"));
        }
        return userId;
    }
}