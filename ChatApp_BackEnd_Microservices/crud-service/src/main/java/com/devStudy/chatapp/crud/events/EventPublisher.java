package com.devStudy.chatapp.crud.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.devStudy.chatapp.crud.config.RabbitMQConfig;
import com.devStudy.chatapp.crud.dto.UserDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @EventListener
    public void handleChatroomMemberChangeEvent(ChangeChatroomMemberEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("chatroomId", event.getChatroomId());
            message.put("addedMembers", event.getAddedMembers());
            message.put("removedMembers", event.getRemovedMembers());
            message.put("eventType", "CHATROOM_MEMBER_CHANGE");
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.CHATROOM_MEMBER_CHANGE_ROUTING_KEY, message);
            
            logger.info("Published chatroom member change event for chatroom {}: added={}, removed={}", 
                       event.getChatroomId(), 
                       event.getAddedMembers().size(), 
                       event.getRemovedMembers().size());
        } catch (Exception e) {
            logger.error("Failed to publish chatroom member change event: {}", e.getMessage());
        }
    }
    
    @EventListener
    public void handleRemoveChatroomEvent(RemoveChatroomEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("chatroomId", event.getEventMsg());
            message.put("eventType", "CHATROOM_REMOVE");
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.CHATROOM_REMOVE_ROUTING_KEY, message);
            
            logger.info("Published chatroom remove event for chatroom {}", event.getEventMsg());
        } catch (Exception e) {
            logger.error("Failed to publish chatroom remove event: {}", e.getMessage());
        }
    }
}