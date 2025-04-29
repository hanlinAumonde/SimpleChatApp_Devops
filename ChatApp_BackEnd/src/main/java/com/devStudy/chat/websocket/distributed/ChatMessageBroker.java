package com.devStudy.chat.websocket.distributed;

import com.devStudy.chat.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.UUID;

import static com.devStudy.chat.service.utils.ConstantValues.CHATROOM_CHANNEL;

@Service
public class ChatMessageBroker {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageBroker.class);

    private static final String instanceId = UUID.randomUUID().toString();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ChatMessageBroker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送消息到聊天室频道
     */
    public void sendToChatroom(long chatroomId, DistributedChatWebSocketHandler.ChatBroadcastMessage message) {
        try {
            String channel = String.format(CHATROOM_CHANNEL, chatroomId);
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, messageJson);
            logger.debug("Message sent to channel {}: {}", channel, messageJson);
        } catch (Exception e) {
            logger.error("Failed to send message to chatroom", e);
            throw new RuntimeException("Failed to send message to chatroom", e);
        }
    }

    public static String getInstanceId() {
        return instanceId;
    }
}
