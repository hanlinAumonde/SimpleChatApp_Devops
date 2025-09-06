package com.devStudy.chatapp.websocket.client;

import com.devStudy.chatapp.websocket.dto.SaveMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Message Service Client Fallback
 * When message-service is unavailable, this fallback will be triggered
 */
@Component
public class MessageServiceClientFallBack implements MessageServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceClientFallBack.class);
    
    @Override
    public boolean saveMessage(long chatroomId, SaveMessageRequest request) {
        logger.warn("MessageService is unavailable. Fallback triggered for chatroomId: {}", chatroomId);
        
        // return false to indicate failure and trigger retry mechanism
        return false;
    }
}
