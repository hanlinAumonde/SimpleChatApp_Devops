package com.devStudy.chatapp.websocket.service;

import com.devStudy.chatapp.websocket.dto.ChatBroadcastMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatMessageBroker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageBroker.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    @Value("${websocket.chat.redis.chatroom-channel}")
    private String CHATROOM_CHANNEL;

    @Autowired
    public ChatMessageBroker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.instanceId = generateInstanceId();
        LOGGER.info("ChatMessageBroker initialized with instance ID: {}", instanceId);
    }

    /**
     * Generate a unique instance ID for this broker instance
     */
    private String generateInstanceId() {
        String hostname = System.getenv("HOSTNAME");
        StringBuilder instanceId = new StringBuilder("websocket-");
        if (hostname != null && !hostname.isEmpty()) {
            instanceId.append(hostname);
        }
        return instanceId.append(UUID.randomUUID()).toString();
    }

    /**
     * Send a broadcast message to all subscribers of the specified chatroom channel.
     * @param chatroomId chatroom ID
     * @param message broadcast message
     */
    public void sendToChatroom(long chatroomId, ChatBroadcastMessage message) {
        try {
            String channel = String.format(CHATROOM_CHANNEL, chatroomId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            // Publish the message to the Redis channel
            redisTemplate.convertAndSend(channel, messageJson);
            
            LOGGER.debug("Message sent to channel {}: messageType={}, broadcastType={}, instanceId={}", 
                        channel, message.messageType(), message.broadcastType(), message.instanceId());
                        
        } catch (Exception e) {
            LOGGER.error("Failed to send message to chatroom {} channel", chatroomId, e);
            throw new RuntimeException("Failed to send message to chatroom", e);
        }
    }

    /**
     * Create a broadcast message object
     */
    public ChatBroadcastMessage createBroadcastMessage(int messageType, 
                                                      String broadcastType,
                                                      String message, 
                                                      com.devStudy.chatapp.websocket.dto.UserDTO sender, 
                                                      String timestamp) {
        return new ChatBroadcastMessage(
            messageType,
            broadcastType, 
            message,
            sender,
            timestamp,
            instanceId
        );
    }

    /**
     * Check if the message was sent by the current instance
     * @param messageInstanceId Instance ID from the message
     * @return true if the message is from the current instance, false otherwise
     */
    public boolean isFromCurrentInstance(String messageInstanceId) {
        return instanceId.equals(messageInstanceId);
    }

    //public String getInstanceId() {return instanceId;}
}