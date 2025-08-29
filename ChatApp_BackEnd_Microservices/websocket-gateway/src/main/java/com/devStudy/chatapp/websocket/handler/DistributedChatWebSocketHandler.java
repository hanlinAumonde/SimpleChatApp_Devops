package com.devStudy.chatapp.websocket.handler;

import com.devStudy.chatapp.websocket.dto.ChatBroadcastMessage;
import com.devStudy.chatapp.websocket.dto.UserDTO;
import com.devStudy.chatapp.websocket.dto.WebSocketConstants;
import com.devStudy.chatapp.websocket.service.ChatMessageBroker;
import com.devStudy.chatapp.websocket.service.ChatSessionRegistryService;
import com.devStudy.chatapp.websocket.service.MessagePersistenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DistributedChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedChatWebSocketHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 本地会话管理: chatroomId -> userId -> WebSocketSession
    private static final Map<Long, Map<Long, WebSocketSession>> localSessions = new ConcurrentHashMap<>();
    // Redis频道监听器管理: chatroomId -> MessageListener
    private static final Map<Long, MessageListener> chatroomListeners = new ConcurrentHashMap<>();

    private final ChatSessionRegistryService chatSessionRegistryService;

    private final ChatMessageBroker chatMessageBroker;

    private final MessagePersistenceService messagePersistenceService;

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    @Value("${websocket.chat.redis.chatroom-channel}")
    private String CHATROOM_CHANNEL;

    @Autowired
    public DistributedChatWebSocketHandler(ChatSessionRegistryService chatSessionRegistryService,
                                           ChatMessageBroker chatMessageBroker,
                                           MessagePersistenceService messagePersistenceService,
                                           RedisMessageListenerContainer redisMessageListenerContainer) {
        this.chatSessionRegistryService = chatSessionRegistryService;
        this.chatMessageBroker = chatMessageBroker;
        this.messagePersistenceService = messagePersistenceService;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        UserDTO userInfo = (UserDTO) session.getAttributes().get("userInfo");

        LOGGER.info("WebSocket connection established: user {} in chatroom {}", userId, chatroomId);

        try {
            // 1. 注册用户到Redis分布式会话表
            chatSessionRegistryService.registerUserConnection(chatroomId, userInfo);

            // 2. 添加到本地会话管理
            addLocalSession(chatroomId, userId, session);

            // 3. 订阅聊天室Redis频道（如果尚未订阅）
            subscribeToChatroomChannel(chatroomId);

            // 4. 广播用户上线消息给所有用户
            Date now = new Date();
            broadcastMessage(
                WebSocketConstants.MESSAGE_CONNECT,
                createFormattedMessage(WebSocketConstants.MESSAGE_CONNECT, 
                                     WebSocketConstants.TO_ALL_IN_CHATROOM, userInfo, now),
                chatroomId,
                WebSocketConstants.TO_ALL_IN_CHATROOM,
                userInfo,
                now
            );
            // 5. 向新连接用户发送当前在线用户列表
            sendOnlineUsersToNewUser(chatroomId, userId, userInfo, now);
        } catch (Exception e) {
            LOGGER.error("Error establishing WebSocket connection for user {} in chatroom {}", 
                        userId, chatroomId, e);
            closeSessionQuietly(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        
        LOGGER.info("WebSocket connection closed: user {} in chatroom {}, status: {}", 
                   userId, chatroomId, status);

        try {
            // 先清理本地会话，避免向已关闭的会话发送消息
            removeSession(chatroomId, userId);
            
            // 获取用户信息
            UserDTO userInfo = chatSessionRegistryService.getUser(chatroomId, userId);
            
            // 广播用户下线消息（只发给其他用户）
            if (userInfo != null) {
                Date now = new Date();
                // 使用异步方式广播，避免阻塞连接关闭过程
                try {
                    broadcastMessage(
                        WebSocketConstants.MESSAGE_DISCONNECT,
                        createFormattedMessage(WebSocketConstants.MESSAGE_DISCONNECT, 
                                             WebSocketConstants.TO_OTHERS_IN_CHATROOM, userInfo, now),
                        chatroomId,
                        WebSocketConstants.TO_OTHERS_IN_CHATROOM,
                        userInfo,
                        now
                    );
                } catch (Exception broadcastException) {
                    LOGGER.debug("Failed to broadcast disconnect message for user {} in chatroom {}: {}", 
                               userId, chatroomId, broadcastException.getMessage());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error during WebSocket connection cleanup for user {} in chatroom {}: {}", 
                        userId, chatroomId, e.getMessage());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        
        try {
            // 从Redis获取用户信息（比从session属性获取更可靠）
            UserDTO userInfo = chatSessionRegistryService.getUser(chatroomId, userId);
            if (userInfo == null) {
                LOGGER.warn("User {} not found in chatroom {} session registry", userId, chatroomId);
                return;
            }

            String messageContent = message.getPayload();
            Date now = new Date();

            LOGGER.debug("Received message from user {} in chatroom {}: {}", userId, chatroomId, messageContent);

            // 1. 异步保存消息到消息服务
            messagePersistenceService.saveMessageAsync(chatroomId, userInfo, messageContent, now);

            // 2. 广播消息给所有用户
            broadcastMessage(
                WebSocketConstants.MESSAGE_TEXT,
                createFormattedMessage(WebSocketConstants.MESSAGE_TEXT, messageContent, userInfo, now),
                chatroomId,
                WebSocketConstants.TO_ALL_IN_CHATROOM,
                userInfo,
                now
            );

        } catch (Exception e) {
            LOGGER.error("Error handling text message from user {} in chatroom {}", userId, chatroomId, e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        LOGGER.info("WebSocket transport error for user {} in chatroom {}: {}",
                   userId, chatroomId, exception.getStackTrace());
        // 判断是否为常见的客户端断开错误
        if (isClientDisconnectionError(exception)) {
            LOGGER.debug("WebSocket client disconnected for user {} in chatroom {}: {}", 
                        userId, chatroomId, exception.getMessage());
        } else {
            LOGGER.error("WebSocket transport error for user {} in chatroom {}", userId, chatroomId, exception);
        }
        
        closeSessionQuietly(session);
        removeSession(chatroomId, userId);
    }
    
    /**
     * 判断是否为客户端断开导致的常见错误
     */
    private boolean isClientDisconnectionError(Throwable exception) {
        if (exception == null) return false;
        
        String message = exception.getMessage();
        return message != null && (
            message.contains("Connection reset by peer") ||
            message.contains("Broken pipe") ||
            message.contains("Connection closed") ||
            exception.getCause() != null && isClientDisconnectionError(exception.getCause())
        );
    }

    /**
     * 处理聊天室成员变更事件广播
     */
    public void broadcastMemberChangeMessage(long chatroomId, List<UserDTO> addedMembers, List<UserDTO> removedMembers) {
        Date now = new Date();
        
        // 广播新成员加入消息
        for (UserDTO user : addedMembers) {
            broadcastMessage(
                WebSocketConstants.MESSAGE_ADD_CHATROOM_MEMBER,
                createFormattedMessage(WebSocketConstants.MESSAGE_ADD_CHATROOM_MEMBER, 
                                     "A new user has joined the chatroom!", user, now),
                chatroomId,
                WebSocketConstants.TO_ALL_IN_CHATROOM,
                null,
                now
            );
        }
        
        // 广播成员离开消息
        for (UserDTO user : removedMembers) {
            broadcastMessage(
                WebSocketConstants.MESSAGE_REMOVE_CHATROOM_MEMBER,
                createFormattedMessage(WebSocketConstants.MESSAGE_REMOVE_CHATROOM_MEMBER, 
                                     "A user has left the chatroom!", user, now),
                chatroomId,
                WebSocketConstants.TO_ALL_IN_CHATROOM,
                null,
                now
            );
        }
    }

    /**
     * 处理聊天室删除事件广播
     */
    public void broadcastChatroomRemovalMessage(long chatroomId) {
        Date now = new Date();
        broadcastMessage(
            WebSocketConstants.MESSAGE_REMOVE_CHATROOM,
            createFormattedMessage(WebSocketConstants.MESSAGE_REMOVE_CHATROOM, 
                                 "This chatroom has been removed!", new UserDTO(), now),
            chatroomId,
            WebSocketConstants.TO_ALL_IN_CHATROOM,
            null,
            now
        );
        
        // 关闭所有相关连接
        Map<Long, WebSocketSession> sessions = localSessions.get(chatroomId);
        if (sessions != null) {
            sessions.values().forEach(this::closeSessionQuietly);
            localSessions.remove(chatroomId);
        }
        
        // 取消订阅频道
        unsubscribeFromChatroomChannel(chatroomId);
    }

    /**
     * 广播消息的核心方法
     */
    private void broadcastMessage(int messageType, String message, long chatroomId, 
                                 String broadcastType, UserDTO sender, Date timestamp) {
        
        Set<UserDTO> onlineUsers = chatSessionRegistryService.getUserConnections(chatroomId);
        if (message.isEmpty() || onlineUsers.isEmpty()) {
            LOGGER.warn("Message is empty or no users in chatroom {}", chatroomId);
            return;
        }

        boolean allInLocalSessions = true;
        
        // 1. 本地广播
        for (UserDTO user : onlineUsers) {
            Map<Long, WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
            if (localSessionsInChatroom != null && localSessionsInChatroom.containsKey(user.getId())) {
                WebSocketSession session = localSessionsInChatroom.get(user.getId());
                
                boolean shouldSend = switch (broadcastType) {
                    case WebSocketConstants.TO_ALL_IN_CHATROOM -> true;
                    case WebSocketConstants.TO_SELF_IN_CHATROOM -> 
                        sender != null && user.getId().equals(sender.getId());
                    case WebSocketConstants.TO_OTHERS_IN_CHATROOM -> 
                        sender == null || !user.getId().equals(sender.getId());
                    default -> false;
                };
                
                if (shouldSend) {
                    sendMessageToSession(session, message);
                }
            } else {
                allInLocalSessions = false;
                // 如果当前实例在聊天室中没有任何本地连接，取消订阅
                if (localSessionsInChatroom == null || localSessionsInChatroom.isEmpty()) {
                    unsubscribeFromChatroomChannel(chatroomId);
                }
            }
        }

        // 2. 跨实例广播（如果有用户在其他实例且不是仅发给自己）
        if (!allInLocalSessions && !WebSocketConstants.TO_SELF_IN_CHATROOM.equals(broadcastType)) {
            String timestampStr = new SimpleDateFormat("HH:mm").format(timestamp);
            ChatBroadcastMessage broadcastMessage = chatMessageBroker.createBroadcastMessage(
                messageType, broadcastType, message, sender, timestampStr
            );
            chatMessageBroker.sendToChatroom(chatroomId, broadcastMessage);
        }
    }

    /**
     * 向新连接的用户发送当前在线用户列表
     */
    private void sendOnlineUsersToNewUser(long chatroomId, long newUserId, UserDTO newUser, Date now) {
        Set<UserDTO> onlineUsers = chatSessionRegistryService.getUserConnections(chatroomId);
        
        for (UserDTO onlineUser : onlineUsers) {
            if (!onlineUser.getId().equals(newUserId)) {
                broadcastMessage(
                    WebSocketConstants.MESSAGE_CONNECT,
                    createFormattedMessage(WebSocketConstants.MESSAGE_CONNECT, 
                                         WebSocketConstants.TO_SELF_IN_CHATROOM, onlineUser, now),
                    chatroomId,
                    WebSocketConstants.TO_SELF_IN_CHATROOM,
                    newUser,
                    now
                );
            }
        }
    }

    /**
     * 创建格式化的消息
     */
    private String createFormattedMessage(int messageType, String messageContent, UserDTO userInfo, Date timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            ObjectNode finalNode = MAPPER.createObjectNode();
            
            ObjectNode userNode = MAPPER.createObjectNode();
            userNode.put("id", userInfo.getId() != null ? userInfo.getId() : 0L);
            userNode.put("username", formatUsername(userInfo));
            
            finalNode.set("user", userNode);
            finalNode.put("messageType", messageType);
            finalNode.put("message", messageContent);
            finalNode.put("timestamp", sdf.format(timestamp));
            
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(finalNode);
            
        } catch (JsonProcessingException e) {
            LOGGER.error("Error creating formatted message", e);
            throw new RuntimeException("Error creating formatted message", e);
        }
    }

    private String formatUsername(UserDTO userInfo) {
        if (userInfo.getLastName() != null && userInfo.getFirstName() != null) {
            return userInfo.getLastName() + " " + userInfo.getFirstName();
        } else if (userInfo.getLastName() != null) {
            return userInfo.getLastName();
        } else {
            return "Unknown User";
        }
    }

    private void sendMessageToSession(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    if (session.isOpen()) {
                        TextMessage textMessage = new TextMessage(message);
                        session.sendMessage(textMessage);
                    }
                }
            }
        } catch (IOException | IllegalStateException e) {
            LOGGER.debug("WebSocket session is closed, skip sending message: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error sending message to session", e);
        }
    }

    private void closeSessionQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error closing session", e);
        }
    }

    private void addLocalSession(long chatroomId, long userId, WebSocketSession session) {
        localSessions.computeIfAbsent(chatroomId, k -> new ConcurrentHashMap<>()).put(userId, session);
    }

    private void removeSession(long chatroomId, long userId) {
        Map<Long, WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
        if (localSessionsInChatroom != null) {
            WebSocketSession session = localSessionsInChatroom.remove(userId);
            if (session != null) {
                closeSessionQuietly(session);
            }
            
            if (localSessionsInChatroom.isEmpty()) {
                localSessions.remove(chatroomId);
            }
        }
        
        long userCount = chatSessionRegistryService.removeUserConnection(chatroomId, userId);
        if (userCount == 0) {
            unsubscribeFromChatroomChannel(chatroomId);
        }
    }

    private void subscribeToChatroomChannel(long chatroomId) {
        if (!chatroomListeners.containsKey(chatroomId)) {
            String channelName = String.format(CHATROOM_CHANNEL, chatroomId);

            MessageListener listener = (message, pattern) -> {
                try {
                    ChatBroadcastMessage chatMessage = 
                        MAPPER.readValue(message.getBody(), ChatBroadcastMessage.class);
                    handleRedisMessage(chatMessage, chatroomId);
                } catch (Exception e) {
                    LOGGER.error("Error processing Redis message for chatroom {}", chatroomId, e);
                }
            };

            redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channelName));
            chatroomListeners.put(chatroomId, listener);
            LOGGER.info("Subscribed to Redis channel: {}", channelName);
        }
    }

    private void unsubscribeFromChatroomChannel(long chatroomId) {
        MessageListener listener = chatroomListeners.remove(chatroomId);
        if (listener != null) {
            String channelName = String.format(CHATROOM_CHANNEL, chatroomId);
            redisMessageListenerContainer.removeMessageListener(listener, new ChannelTopic(channelName));
            LOGGER.info("Unsubscribed from Redis channel: {}", channelName);
        }
    }

    private void handleRedisMessage(ChatBroadcastMessage chatMessage, long chatroomId) {
        Map<Long, WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
        
        // 如果当前实例没有该聊天室的连接，取消订阅
        if (localSessionsInChatroom == null || localSessionsInChatroom.isEmpty()) {
            unsubscribeFromChatroomChannel(chatroomId);
            return;
        }

        // 避免处理自己发送的消息
        if (!chatMessageBroker.isFromCurrentInstance(chatMessage.instanceId())) {
            broadcastMessageLocally(
                chatMessage.message(),
                chatroomId,
                chatMessage.broadcastType(),
                chatMessage.sender()
            );
        }
    }

    private void broadcastMessageLocally(String message, long chatroomId, 
                                       String broadcastType, UserDTO sender) {
        Map<Long, WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
        if (localSessionsInChatroom != null) {
            localSessionsInChatroom.entrySet().stream()
                .filter(entry -> switch (broadcastType) {
                    case WebSocketConstants.TO_ALL_IN_CHATROOM -> true;
                    case WebSocketConstants.TO_OTHERS_IN_CHATROOM ->
                        sender == null || !entry.getKey().equals(sender.getId());
                    case WebSocketConstants.TO_SELF_IN_CHATROOM ->
                        sender != null && entry.getKey().equals(sender.getId());
                    default -> false;
                })
                .map(Map.Entry::getValue)
                .forEach(session -> sendMessageToSession(session, message));
        }
    }
}