package com.devStudy.chatapp.websocket.client;

import com.devStudy.chatapp.websocket.dto.SaveMessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Message Service Client
 * Save message into mongoDB using message-service microservice
 * Use spring cloud integration of resilience4j for fallback
 */
@FeignClient(name = "message-service", fallback = MessageServiceClientFallBack.class)
public interface MessageServiceClient {
    
    /**
     * Save message
     * @param chatroomId chatroomId
     * @param request message save request
     * @return success
     */
    @PostMapping("/api/messages/chatrooms/{chatroomId}/save")
    boolean saveMessage(@PathVariable("chatroomId") long chatroomId,
                     @RequestBody SaveMessageRequest request);
}