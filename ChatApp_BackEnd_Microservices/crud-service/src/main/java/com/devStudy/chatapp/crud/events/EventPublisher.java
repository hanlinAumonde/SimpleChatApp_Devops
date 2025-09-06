package com.devStudy.chatapp.crud.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    
    private final RabbitTemplate rabbitTemplate;

    @Value("${chatroomApp.rabbitmq.RABBITMQ_EXCHANGE_NAME}")
    private String EXCHANGE_NAME;

    @Value("${chatroomApp.rabbitmq.CHATROOM_MEMBER_CHANGE_ROUTING_KEY}")
    private String CHATROOM_MEMBER_CHANGE_ROUTING_KEY;

    @Value("${chatroomApp.rabbitmq.CHATROOM_REMOVE_ROUTING_KEY}")
    private String CHATROOM_REMOVE_ROUTING_KEY;

    @Autowired
    EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatroomMemberChangeEvent(ChangeChatroomMemberEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("chatroomId", event.getChatroomId());
            message.put("addedMembers", event.getAddedMembers());
            message.put("removedMembers", event.getRemovedMembers());
            message.put("eventType", "CHATROOM_MEMBER_CHANGE");
            
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, CHATROOM_MEMBER_CHANGE_ROUTING_KEY, message);
            
            logger.info("Published chatroom member change event for chatroom {}: added={}, removed={}", 
                       event.getChatroomId(), 
                       event.getAddedMembers().size(), 
                       event.getRemovedMembers().size());
        } catch (Exception e) {
            logger.error("Failed to publish chatroom member change event: {}", e.getMessage());
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRemoveChatroomEvent(RemoveChatroomEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("chatroomId", event.getEventMsg());
            message.put("eventType", "CHATROOM_REMOVE");
            
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, CHATROOM_REMOVE_ROUTING_KEY, message);
            
            logger.info("Published chatroom remove event for chatroom {}", event.getEventMsg());
        } catch (Exception e) {
            logger.error("Failed to publish chatroom remove event: {}", e.getMessage());
        }
    }
}