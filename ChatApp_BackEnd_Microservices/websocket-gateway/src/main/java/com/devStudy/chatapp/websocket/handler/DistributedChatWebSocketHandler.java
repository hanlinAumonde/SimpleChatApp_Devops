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
            // 1. Inscribe user connection in the distributed registry
            chatSessionRegistryService.registerUserConnection(chatroomId, userInfo);

            // 2. Add to local session map
            addLocalSession(chatroomId, userId, session);

            // 3. Subscribe to Redis channel if not already subscribed
            subscribeToChatroomChannel(chatroomId);

            // 4. Broadcast user online message (to all users including self)
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
            // 5. Send current online users to the newly connected user
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
            // First, close the session if not already closed
            removeSession(chatroomId, userId);
            
            // Obtain user info from the registry
            UserDTO userInfo = chatSessionRegistryService.getUser(chatroomId, userId);
            
            // Broadcast user offline message (to others only)
            if (userInfo != null) {
                Date now = new Date();
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
            // Obtain user info from the registry
            UserDTO userInfo = chatSessionRegistryService.getUser(chatroomId, userId);
            if (userInfo == null) {
                LOGGER.warn("User {} not found in chatroom {} session registry", userId, chatroomId);
                return;
            }

            String messageContent = message.getPayload();
            Date now = new Date();

            LOGGER.debug("Received message from user {} in chatroom {}: {}", userId, chatroomId, messageContent);

            // 1. Save message asynchronously
            messagePersistenceService.saveMessageAsync(chatroomId, userInfo, messageContent, now);

            // 2. Broadcast message to all users in the chatroom
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
        // Check if the error is due to client disconnection
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
     * Check if the exception indicates a client disconnection
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
     * Handle broadcasting member change messages (additions/removals)
     */
    public void broadcastMemberChangeMessage(long chatroomId, List<UserDTO> addedMembers, List<UserDTO> removedMembers) {
        Date now = new Date();
        
        // Broadcast member addition messages
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
        
        // Broadcast member removal messages
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
     * Handle broadcasting chatroom removal messages and cleanup
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
        
        // Clean up all local sessions
        Map<Long, WebSocketSession> sessions = localSessions.get(chatroomId);
        if (sessions != null) {
            sessions.values().forEach(this::closeSessionQuietly);
            localSessions.remove(chatroomId);
        }
        
        // Unsubscribe from Redis channel
        unsubscribeFromChatroomChannel(chatroomId);
    }

    /**
     * Broadcast a message to users in a chatroom, both locally and across instances if needed
     */
    private void broadcastMessage(int messageType, String message, long chatroomId, 
                                 String broadcastType, UserDTO sender, Date timestamp) {
        
        Set<UserDTO> onlineUsers = chatSessionRegistryService.getUserConnections(chatroomId);
        if (message.isEmpty() || onlineUsers.isEmpty()) {
            LOGGER.warn("Message is empty or no users in chatroom {}", chatroomId);
            return;
        }

        boolean allInLocalSessions = true;
        
        // 1. Local broadcasting
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
                // If no local session, ensure we are subscribed to the Redis channel
                if (localSessionsInChatroom == null || localSessionsInChatroom.isEmpty()) {
                    unsubscribeFromChatroomChannel(chatroomId);
                }
            }
        }

        // 2. Distributed broadcasting via Redis if not all users are local
        if (!allInLocalSessions && !WebSocketConstants.TO_SELF_IN_CHATROOM.equals(broadcastType)) {
            String timestampStr = new SimpleDateFormat("HH:mm").format(timestamp);
            ChatBroadcastMessage broadcastMessage = chatMessageBroker.createBroadcastMessage(
                messageType, broadcastType, message, sender, timestampStr
            );
            chatMessageBroker.sendToChatroom(chatroomId, broadcastMessage);
        }
    }

    /**
     * Send the list of currently online users to the newly connected user
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
     * Create a formatted JSON message string
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
        
        // If no local sessions, unsubscribe from channel
        if (localSessionsInChatroom == null || localSessionsInChatroom.isEmpty()) {
            unsubscribeFromChatroomChannel(chatroomId);
            return;
        }

        // Avoid processing messages originating from this instance
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