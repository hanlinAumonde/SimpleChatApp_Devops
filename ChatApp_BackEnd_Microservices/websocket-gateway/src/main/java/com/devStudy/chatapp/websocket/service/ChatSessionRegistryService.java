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
    private long chatSessionExpiry; // 24小时

    @Value("${websocket.chat.redis.chatroom-users-key}")
    private String CHATROOM_USERS_KEY;

    @Autowired
    public ChatSessionRegistryService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 构建用户在聊天室中的Redis key
     * 格式: chatroom:{chatroomId}:user:{userId}
     */
    private String buildUserKey(long chatroomId, long userId) {
        return String.format(CHATROOM_USERS_KEY, chatroomId, userId);
    }

    /**
     * 注册用户连接到聊天室
     * @param chatroomId 聊天室ID
     * @param userInfo 用户信息
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
     * 移除用户连接并返回剩余用户数量
     * @param chatroomId 聊天室ID
     * @param userId 用户ID
     * @return 移除后聊天室中的用户数量
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
            return getUserCount(chatroomId); // 返回当前用户数，即使删除失败
        }
    }

    /**
     * 获取聊天室中的用户数量
     * @param chatroomId 聊天室ID
     * @return 用户数量
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
     * 获取聊天室中的所有用户连接
     * @param chatroomId 聊天室ID
     * @return 用户信息集合
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
            return Set.of(); // 返回空集合而不是抛出异常
        }
    }

    /**
     * 获取特定用户在聊天室中的信息
     * @param chatroomId 聊天室ID
     * @param userId 用户ID
     * @return 用户信息，如果不存在则返回null
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