package com.devStudy.chat.websocket.distributed;

import com.devStudy.chat.dto.DTOMapper;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.model.User;
import com.devStudy.chat.service.implementations.ChatMessageService;
import com.devStudy.chat.service.implementations.UserService;
import com.devStudy.chat.service.utils.Events.ChangeChatroomMemberEvent;
import com.devStudy.chat.service.utils.Events.RemoveChatroomEvent;
import com.devStudy.chat.service.utils.Exceptions.WebSocketException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.devStudy.chat.service.utils.ConstantValues.*;
import static com.devStudy.chat.service.utils.ConstantValues.TO_OTHERS_IN_CHATROOM;

@Component
public class DistributedChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedChatWebSocketHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<Long, Map<Long, WebSocketSession>> localSessions = new ConcurrentHashMap<>();
    private static final Map<Long, MessageListener> chatroomListeners = new ConcurrentHashMap<>();

    private final UserService userService;
    private final ChatSessionRegistryService chatSessionRegistryService;
    private final ChatMessageBroker chatMessageBroker;
    private final ChatMessageService chatMessageService;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    public DistributedChatWebSocketHandler(
            UserService userService,
            ChatSessionRegistryService chatSessionRegistryService,
            ChatMessageBroker chatMessageBroker,
            ChatMessageService chatMessageService,
            RedisMessageListenerContainer redisMessageListenerContainer) {
        this.userService = userService;
        this.chatSessionRegistryService = chatSessionRegistryService;
        this.chatMessageBroker = chatMessageBroker;
        this.chatMessageService = chatMessageService;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    private UserDTO getUserInfo(long userId){
        try {
            Optional<User> user = userService.findUserById(userId);
            return user.map(DTOMapper::toUserDTO).orElseThrow(() -> new WebSocketException("User not found"));
        } catch (Exception e) {
            throw new WebSocketException("Failed to get user info", e);
        }
    }

    private String setMessage(int messageType, String message, UserDTO userInfo, Date now) {
        //format : {user: {id: 1, username: "user1 user1"}, messageType: 0, message: "hello" , timestamp : "18:00"}
        try {
            //Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            ObjectNode finalNode = MAPPER.createObjectNode();

            ObjectNode userNode = MAPPER.createObjectNode();
            userNode.put("id", userInfo.getId());
            userNode.put("username", userInfo.getLastName() + " " + userInfo.getFirstName());

            finalNode.set("user", userNode);
            finalNode.put("messageType", messageType);
            finalNode.put("message", message);
            finalNode.put("timestamp", sdf.format(now));

            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(finalNode);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while creating message : ", e);
            throw new WebSocketException("Error while creating message", e);
        }
    }

    private void sendMessageToSession(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                TextMessage textMessage = new TextMessage(message);
                session.sendMessage(textMessage);
            }
        } catch (IOException e) {
            throw new WebSocketException("Error while sending message to session", e);
        }
    }

    private void broadcastMessage(int messageType, String message, long chatroomId, String broadcastType, UserDTO sender) {
        Set<UserDTO> userSet = chatSessionRegistryService.getUserConnections(chatroomId);
        if (message.isEmpty() || userSet.isEmpty()) {
            LOGGER.warn("Message is empty or no users in chatroom");
            return;
        }
        boolean allInLocalSessions = true;
        for (UserDTO user : userSet) {
            Map<Long , WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
            if(localSessionsInChatroom.containsKey(user.getId())) {
                WebSocketSession session = localSessionsInChatroom.get(user.getId());
                switch (broadcastType) {
                    case TO_ALL_IN_CHATROOM:
                        sendMessageToSession(session, message);
                        break;
                    case TO_SELF_IN_CHATROOM:
                        if (user.getId() == sender.getId()) {
                            sendMessageToSession(session, message);
                        }
                        break;
                    case TO_OTHERS_IN_CHATROOM:
                        if (user.getId() != sender.getId()) {
                            sendMessageToSession(session, message);
                        }
                        break;
                    default:
                        break;
                }
            }else{
                allInLocalSessions = false;
                if(localSessionsInChatroom.isEmpty())
                    unsubscribeFromChatroomChannel(chatroomId);
            }
        }
        if(!allInLocalSessions && !Objects.equals(broadcastType, TO_SELF_IN_CHATROOM)) {
            chatMessageBroker.sendToChatroom(chatroomId, new ChatBroadcastMessage(
                    messageType,
                    broadcastType,
                    message,
                    sender,
                    new SimpleDateFormat("HH:mm").format(new Date()),
                    ChatMessageBroker.getInstanceId()
            ));
        }
    }

    private void closeSessionQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error while closing session", e);
        }
    }

    private void removeSession(long chatroomId, long userId) {
        Map<Long, WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
        WebSocketSession session = localSessionsInChatroom.remove(userId);
        if (session != null) {
            closeSessionQuietly(session);
        }
        if(localSessionsInChatroom.isEmpty()) {
            localSessions.remove(chatroomId);
        }
        long userCount = chatSessionRegistryService.removeUserConnection(chatroomId, userId);
        if (userCount == 0) {
            unsubscribeFromChatroomChannel(chatroomId);
        }
    }

    private void unsubscribeFromChatroomChannel(long chatroomId) {
        MessageListener listener = chatroomListeners.remove(chatroomId);
        if (listener != null) {
            String channelName = String.format(CHATROOM_CHANNEL, chatroomId);
            redisMessageListenerContainer.removeMessageListener(listener, new ChannelTopic(channelName));
            LOGGER.info("Unsubscribed from channel: {}", channelName);
        }
    }

    private void addLocalSession(long chatroomId, long userId, WebSocketSession session) {
        localSessions.computeIfAbsent(chatroomId, k -> new ConcurrentHashMap<>()).put(userId, session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");

        // Register the user connection
        UserDTO userInfo = getUserInfo(userId);
        chatSessionRegistryService.registerUserConnection(chatroomId, userInfo);

        addLocalSession(chatroomId, userId, session);
        subscribeToChatroomChannel(chatroomId);

        broadcastMessage(
                MESSAGE_CONNECT,
                setMessage(MESSAGE_CONNECT, TO_ALL_IN_CHATROOM, userInfo, new Date()),
                chatroomId,
                TO_ALL_IN_CHATROOM,
                userInfo
        );

        //send to self for acknowledgment of all connected users in chatroom
        chatSessionRegistryService.getUserConnections(chatroomId).forEach(user -> {
            if(user.getId() != userId)
                broadcastMessage(
                        MESSAGE_CONNECT,
                        setMessage(MESSAGE_CONNECT, TO_SELF_IN_CHATROOM, user, new Date()),
                        chatroomId,
                        TO_SELF_IN_CHATROOM,
                        userInfo
                );
        });
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
                    LOGGER.error("Error processing Redis message", e);
                }
            };

            redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channelName));
            chatroomListeners.put(chatroomId, listener);
            LOGGER.info("Subscribed to channel: {}", channelName);
        }
    }

    private void broadcastMessageLocally(
            String message,
            long chatroomId,
            String broadcastType,
            UserDTO sender
    ) {
        Map<Long, WebSocketSession> localSessionsInChatroom = localSessions.get(chatroomId);
        if (localSessionsInChatroom != null) {
            localSessionsInChatroom.entrySet().stream()
                    .filter(entry -> Objects.equals(broadcastType, TO_ALL_IN_CHATROOM) ||
                            (Objects.equals(broadcastType, TO_OTHERS_IN_CHATROOM) && entry.getKey() != sender.getId()))
                    .map(Map.Entry::getValue)
                    .forEach(session -> sendMessageToSession(session, message));
        }
    }

    private void handleRedisMessage(ChatBroadcastMessage chatMessage, long chatroomId) {
        if(localSessions.get(chatroomId).isEmpty()) {
            unsubscribeFromChatroomChannel(chatroomId);
            return;
        }
        if(!Objects.equals(chatMessage.instanceId, ChatMessageBroker.getInstanceId())) {
            broadcastMessageLocally(
                    chatMessage.message,
                    chatroomId,
                    chatMessage.broadcastType,
                    chatMessage.sender
            );
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        UserDTO userInfo = chatSessionRegistryService.getUser(chatroomId, userId);

        // on envoie un message de déconnexion à tous les utilisateurs connect
        broadcastMessage(
                MESSAGE_DISCONNECT,
                setMessage(MESSAGE_DISCONNECT, TO_OTHERS_IN_CHATROOM, userInfo, new Date()),
                chatroomId,
                TO_OTHERS_IN_CHATROOM,
                userInfo
        );

        removeSession(chatroomId, userId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        //UserDTO userInfo = getUserInfo(userId);
        UserDTO userInfo = chatSessionRegistryService.getUser(chatroomId, userId);

        String msg = message.getPayload();

        Date date = new Date();
        chatMessageService.saveMsgIntoCollection(chatroomId, userInfo, msg, date);
        //on envoie le message à tous les utilisateurs connectés
        broadcastMessage(
                MESSAGE_TEXT,
                setMessage(MESSAGE_TEXT, msg, userInfo, date),
                chatroomId,
                TO_ALL_IN_CHATROOM,
                userInfo
        );
    }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        long chatroomId = (long) session.getAttributes().get("chatroomId");
        long userId = (long) session.getAttributes().get("userId");
        LOGGER.error("Error occurred in session: ", exception);
        closeSessionQuietly(session);
        removeSession(chatroomId, userId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void removeEventListener(RemoveChatroomEvent event) {
        long chatroomId = event.getEventMsg();
        broadcastMessage(
                MESSAGE_REMOVE_CHATROOM,
                setMessage(MESSAGE_REMOVE_CHATROOM,"This chatroom has been removed!",new UserDTO(),new Date()),
                chatroomId,
                TO_ALL_IN_CHATROOM,
                null
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void changeChatroomMemberEventListener(ChangeChatroomMemberEvent event) {
        long chatroomId = event.getChatroomId();
        for(UserDTO user : event.getAddedMembers()) {
            broadcastMessage(
                    MESSAGE_ADD_CHATROOM_MEMBER,
                    setMessage(MESSAGE_ADD_CHATROOM_MEMBER,"A new user has joined the chatroom!", user, new Date()),
                    chatroomId,
                    TO_ALL_IN_CHATROOM,
                    null
            );
        }
        for(UserDTO user : event.getRemovedMembers()) {
            broadcastMessage(
                    MESSAGE_REMOVE_CHATROOM_MEMBER,
                    setMessage(MESSAGE_REMOVE_CHATROOM_MEMBER, "A user has left the chatroom!", user, new Date()),
                    chatroomId,
                    TO_ALL_IN_CHATROOM,
                    null
            );
        }
    }

    /**
     * 聊天消息类
     */
    public record ChatBroadcastMessage (
            int messageType,        // 消息类型: MESSAGE_TEXT, MESSAGE_CONNECT, MESSAGE_DISCONNECT
            String broadcastType,        // 目标类型: TO_ALL_IN_CHATROOM, TO_OTHERS_IN_CHATROOM, TO_SELF_IN_CHATROOM
            String message,           // 消息内容
            UserDTO sender,             // 发送者信息
            String timestamp,         // 时间戳
            String instanceId // 实例ID
    ) implements Serializable {}
}
