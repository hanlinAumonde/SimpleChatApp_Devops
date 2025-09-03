package com.devStudy.chatapp.websocket.service;

import com.devStudy.chatapp.websocket.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ChatSessionRegistryService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatSessionRegistryService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${websocket.chat.session-expiry:86400}")
    private long chatSessionExpiry;

    @Value("${websocket.chat.redis.chatroom-users-key}")
    private String CHATROOM_USERS_KEY;

    @Autowired
    public ChatSessionRegistryService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Build the Redis key for a user's connection in a chatroom.
     * Format: chatroom:{chatroomId}:user:{userId}
     */
    private String buildUserKey(long chatroomId, long userId) {
        return String.format(CHATROOM_USERS_KEY, chatroomId, userId);
    }

    /**
     * Inscribe a user connection to a chatroom with an expiry time.
     * @param chatroomId chatroom ID
     * @param userInfo user information
     */
    public void registerUserConnection(long chatroomId, UserDTO userInfo) {
        try {
            String key = buildUserKey(chatroomId, userInfo.getId());
            redisTemplate.opsForValue().set(key, userInfo, chatSessionExpiry, TimeUnit.SECONDS);
            LOGGER.debug("Registered user {} in chatroom {}", userInfo.getId(), chatroomId);
        } catch (Exception e) {
            LOGGER.error("Failed to register user connection for user {} in chatroom {}", 
                        userInfo.getId(), chatroomId, e);
            throw new RuntimeException("Failed to register user connection", e);
        }
    }

    /**
     * Remove a user connection from a chatroom.
     * @param chatroomId chatroom ID
     * @param userId user ID
     * @return number of remaining users in the chatroom after removal
     */
    public long removeUserConnection(long chatroomId, long userId) {
        try {
            String key = buildUserKey(chatroomId, userId);
            redisTemplate.delete(key);
            long remainingUsers = getUserCount(chatroomId);
            LOGGER.debug("Removed user {} from chatroom {}, remaining users: {}", 
                        userId, chatroomId, remainingUsers);
            return remainingUsers;
        } catch (Exception e) {
            LOGGER.error("Failed to remove user connection for user {} in chatroom {}", 
                        userId, chatroomId, e);
            return getUserCount(chatroomId); // return current count even on failure
        }
    }

    /**
     * Obtain the number of users currently in a chatroom.
     * @param chatroomId chatroom ID
     * @return user count
     */
    public long getUserCount(long chatroomId) {
        try {
            String pattern = String.format("chatroom:%d:user:*", chatroomId);
            Set<String> keys = redisTemplate.keys(pattern);
            return keys.size();
        } catch (Exception e) {
            LOGGER.error("Failed to get user count for chatroom {}", chatroomId, e);
            return 0;
        }
    }

    /**
     * Obtain all user connections in a chatroom.
     * @param chatroomId chatroom ID
     * @return collection of user information
     */
    public Set<UserDTO> getUserConnections(long chatroomId) {
        try {
            String pattern = String.format("chatroom:%d:user:*", chatroomId);
            Set<String> keys = redisTemplate.keys(pattern);
            
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
                        LOGGER.warn("Unexpected object type in Redis: {}", val != null ? val.getClass() : "null");
                    }
                }
            }
            
            LOGGER.debug("Retrieved {} users from chatroom {}", users.size(), chatroomId);
            return users;
            
        } catch (Exception e) {
            LOGGER.error("Failed to get user connections for chatroom {}", chatroomId, e);
            return Set.of(); // return empty set on failure
        }
    }

    /**
     * Obtain a specific user's information in a chatroom.
     * @param chatroomId chatroom ID
     * @param userId user ID
     * @return user information or null if not found
     */
    public UserDTO getUser(long chatroomId, long userId) {
        try {
            String key = buildUserKey(chatroomId, userId);
            Object userObj = redisTemplate.opsForValue().get(key);
            
            if (userObj instanceof UserDTO user) {
                return user;
            } else if (userObj != null) {
                LOGGER.warn("Unexpected object type for user {} in chatroom {}: {}", 
                           userId, chatroomId, userObj.getClass());
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.error("Failed to get user {} from chatroom {}", userId, chatroomId, e);
            return null;
        }
    }
}