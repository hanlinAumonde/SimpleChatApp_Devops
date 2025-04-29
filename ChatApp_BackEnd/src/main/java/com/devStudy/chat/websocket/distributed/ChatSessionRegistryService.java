package com.devStudy.chat.websocket.distributed;

import com.devStudy.chat.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.devStudy.chat.service.utils.ConstantValues.CHATROOM_USERS_KEY;

@Service
public class ChatSessionRegistryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatSessionRegistryService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${chatroomApp.CHAT_SESSION_EXPIRY}")
    private long chatSessionExpiry;

    @Autowired
    public ChatSessionRegistryService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildUserKey(long chatroomId, long userId) {
        return String.format(CHATROOM_USERS_KEY + "%d", chatroomId, userId);
    }

    public void registerUserConnection(long chatroomId, UserDTO userInfo) {
        try {
            String key = buildUserKey(chatroomId, userInfo.getId());
            redisTemplate.opsForValue().set(key, userInfo, chatSessionExpiry, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("Failed to register user connection", e);
            throw new RuntimeException("Failed to register user connection", e);
        }
    }

    public long removeUserConnection(long chatroomId, long userId){
        //remove user from chatroom, and return the number of users in the chatroom after removing
        String key = buildUserKey(chatroomId, userId);
        redisTemplate.delete(key);
        return getUserCount(chatroomId);
    }

    public long getUserCount(long chatroomId) {
        Set<String> keys = redisTemplate.keys(String.format(CHATROOM_USERS_KEY + "*", chatroomId));
        return keys.size();
    }

    public Set<UserDTO> getUserConnections(long chatroomId) {
        try {
            Set<String> keys = redisTemplate.keys(String.format(CHATROOM_USERS_KEY + "*", chatroomId));
            if (keys.isEmpty()) {
                return Set.of();
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            Set<UserDTO> users = new HashSet<>();
            if (values != null) {
                for (Object val : values) {
                    if (val instanceof UserDTO user) {
                        users.add(user);
                    } else {
                        LOGGER.warn("Unexpected object type in Redis: {}", val);
                    }
                }
            }
            return users;
        } catch (Exception e) {
            LOGGER.error("Failed to get user connections", e);
            throw new RuntimeException("Failed to get user connections", e);
        }
    }

    public UserDTO getUser(long chatroomId, long userId) {
        try {
            String key = buildUserKey(chatroomId, userId);
            return (UserDTO) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to get user connection", e);
            throw new RuntimeException("Failed to get user connection", e);
        }
    }
}
