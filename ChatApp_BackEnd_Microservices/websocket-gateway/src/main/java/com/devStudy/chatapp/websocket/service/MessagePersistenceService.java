package com.devStudy.chatapp.websocket.service;

import com.devStudy.chatapp.websocket.client.MessageServiceClient;
import com.devStudy.chatapp.websocket.dto.SaveMessageRequest;
import com.devStudy.chatapp.websocket.dto.UserDTO;
import com.devStudy.chatapp.websocket.events.MessageSaveFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MessagePersistenceService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePersistenceService.class);

    private final MessageServiceClient messageServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MessagePersistenceService(MessageServiceClient messageServiceClient,
                                   ApplicationEventPublisher eventPublisher) {
        this.messageServiceClient = messageServiceClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Save message asynchronously.
     * Use @Async to avoid blocking the main thread.
     * @param chatroomId chatroom ID
     * @param userInfo sender information
     * @param content message content
     * @param timestamp timestamp
     */
    @Async
    public void saveMessageAsync(long chatroomId, UserDTO userInfo, String content, Date timestamp) {
        SaveMessageRequest messageRequest = createMessageRequest(userInfo, content, timestamp);
        boolean success = messageServiceClient.saveMessage(chatroomId, messageRequest);

        if (success) {
            LOGGER.debug("Message saved successfully for user {} in chatroom {}",
                        userInfo.getId(), chatroomId);
        } else {
            // two possible scenarios of failure:
            // 1. Failure in normal call, service returned false
            // 2. Feign circuit breaker activated, fallback returned false
            // In both cases, we treat it as a failure to save the message
            // and trigger the retry mechanism
            LOGGER.warn("Message service returned false for user {} in chatroom {} - may be normal failure or circuit breaker fallback",
                       userInfo.getId(), chatroomId);

            // Publish failure event to trigger event-driven retry mechanism
            publishFailureEvent(chatroomId, messageRequest, timestamp);
        }
    }
    
    /**
     * Publish a message save failure event for retry handling.
     */
    private void publishFailureEvent(long chatroomId, SaveMessageRequest messageRequest, 
                                   Date timestamp) {
        MessageSaveFailedEvent event = new MessageSaveFailedEvent(
            chatroomId, messageRequest, timestamp, "Service returned false (normal call or circuit breaker)", 0);
        
        eventPublisher.publishEvent(event);
        
        LOGGER.info("Published MessageSaveFailedEvent: {}", event);
    }

    /**
     * Build the SaveMessageRequest object.
     */
    private SaveMessageRequest createMessageRequest(UserDTO userInfo, String content, Date timestamp) {
        SaveMessageRequest request = new SaveMessageRequest();
        request.setSenderId(userInfo.getId());
        request.setSenderFirstName(userInfo.getFirstName());
        request.setSenderLastName(userInfo.getLastName());
        request.setSenderMail(userInfo.getMail());
        request.setContent(content);
        request.setTimestamp(timestamp);
        return request;
    }


//    public boolean saveMessageSync(long chatroomId, UserDTO userInfo, String content, Date timestamp) {
//        try {
//            SaveMessageRequest messageRequest = createMessageRequest(userInfo, content, timestamp);
//            messageServiceClient.saveMessage(chatroomId, messageRequest);
//
//            LOGGER.debug("Message saved synchronously for user {} in chatroom {}",
//                        userInfo.getId(), chatroomId);
//            return true;
//
//        } catch (Exception e) {
//            LOGGER.error("Failed to save message synchronously for user {} in chatroom {}: {}",
//                        userInfo.getId(), chatroomId, e.getMessage());
//            return false;
//        }
//    }
}