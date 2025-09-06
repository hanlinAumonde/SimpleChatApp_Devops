package com.devStudy.chatapp.websocket.events;

import com.devStudy.chatapp.websocket.config.RabbitMQConfig;
import com.devStudy.chatapp.websocket.dto.UserDTO;
import com.devStudy.chatapp.websocket.handler.DistributedChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RabbitMQEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQEventListener.class);

    private final DistributedChatWebSocketHandler webSocketHandler;

    @Autowired
    public RabbitMQEventListener(DistributedChatWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Listener for chatroom member change events
     */
    @RabbitListener(queues = RabbitMQConfig.WEBSOCKET_CHATROOM_MEMBER_CHANGE_QUEUE)
    public void handleChatroomMemberChange(Map<String, Object> eventData) {
        try {
            LOGGER.info("Received chatroom member change event: {}", eventData);
            
            Long chatroomId = getLong(eventData, "chatroomId");
            if (chatroomId == null) {
                LOGGER.warn("Missing chatroomId in member change event");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> addedMembersData = 
                (List<Map<String, Object>>) eventData.get("addedMembers");
            
            @SuppressWarnings("unchecked") 
            List<Map<String, Object>> removedMembersData = 
                (List<Map<String, Object>>) eventData.get("removedMembers");

            List<UserDTO> addedMembers = convertToUserDTOs(addedMembersData);
            List<UserDTO> removedMembers = convertToUserDTOs(removedMembersData);

            // Broadcast member change message via WebSocket handler
            webSocketHandler.broadcastMemberChangeMessage(chatroomId, addedMembers, removedMembers);
            
            LOGGER.debug("Successfully processed member change event for chatroom {}: +{} -{}", 
                        chatroomId, addedMembers.size(), removedMembers.size());

        } catch (Exception e) {
            LOGGER.error("Error processing chatroom member change event", e);
        }
    }

    /**
     * Listener for chatroom removal events
     */
    @RabbitListener(queues = RabbitMQConfig.WEBSOCKET_CHATROOM_REMOVE_QUEUE)
    public void handleChatroomRemoval(Map<String, Object> eventData) {
        try {
            LOGGER.info("Received chatroom removal event: {}", eventData);
            
            Long chatroomId = getLong(eventData, "chatroomId");
            if (chatroomId == null) {
                LOGGER.warn("Missing chatroomId in chatroom removal event");
                return;
            }

            // Broadcast chatroom removal message via WebSocket handler
            webSocketHandler.broadcastChatroomRemovalMessage(chatroomId);
            
            LOGGER.info("Successfully processed chatroom removal event for chatroom {}", chatroomId);

        } catch (Exception e) {
            LOGGER.error("Error processing chatroom removal event", e);
        }
    }

    /**
     * Transform a list of Map data to a list of UserDTOs
     */
    private List<UserDTO> convertToUserDTOs(List<Map<String, Object>> usersData) {
        if (usersData == null) {
            return List.of();
        }
        
        return usersData.stream()
                .map(this::mapToUserDTO)
                .toList();
    }

    /**
     * Transform a Map to a UserDTO
     */
    private UserDTO mapToUserDTO(Map<String, Object> userData) {
        UserDTO user = new UserDTO();
        user.setId(getLong(userData, "id"));
        user.setFirstName((String) userData.get("firstName"));
        user.setLastName((String) userData.get("lastName"));
        user.setMail((String) userData.get("mail"));
        user.setAdmin(getBoolean(userData, "admin"));
        user.setActive(getBoolean(userData, "active"));
        return user;
    }

    /**
     * Obtain Long value from Map safely
     */
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                LOGGER.warn("Failed to parse Long value for key {}: {}", key, value);
            }
        }
        return null;
    }

    /**
     * Obtain Boolean value from Map safely
     */
    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return false;
    }
}