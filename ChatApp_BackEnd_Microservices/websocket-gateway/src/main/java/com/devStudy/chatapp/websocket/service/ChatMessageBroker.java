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
        // 生成唯一的实例ID，用于避免消息重复处理
        this.instanceId = generateInstanceId();
        LOGGER.info("ChatMessageBroker initialized with instance ID: {}", instanceId);
    }

    /**
     * 生成实例ID
     */
    private String generateInstanceId() {
        // 优先使用环境变量中的HOSTNAME或配置的instance-id
        String hostname = System.getenv("HOSTNAME");
        StringBuilder instanceId = new StringBuilder("websocket-");
        if (hostname != null && !hostname.isEmpty()) {
            instanceId.append(hostname);
        }
        return instanceId.append(UUID.randomUUID()).toString();
    }

    /**
     * 发送消息到聊天室频道
     * @param chatroomId 聊天室ID
     * @param message 广播消息
     */
    public void sendToChatroom(long chatroomId, ChatBroadcastMessage message) {
        try {
            String channel = String.format(CHATROOM_CHANNEL, chatroomId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            // 通过Redis pub/sub发布消息
            redisTemplate.convertAndSend(channel, messageJson);
            
            LOGGER.debug("Message sent to channel {}: messageType={}, broadcastType={}, instanceId={}", 
                        channel, message.messageType(), message.broadcastType(), message.instanceId());
                        
        } catch (Exception e) {
            LOGGER.error("Failed to send message to chatroom {} channel", chatroomId, e);
            throw new RuntimeException("Failed to send message to chatroom", e);
        }
    }

    /**
     * 创建带有当前实例ID的广播消息
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
     * 检查消息是否来自当前实例
     * @param messageInstanceId 消息中的实例ID
     * @return 是否为当前实例发送的消息
     */
    public boolean isFromCurrentInstance(String messageInstanceId) {
        return instanceId.equals(messageInstanceId);
    }

    //public String getInstanceId() {return instanceId;}
}