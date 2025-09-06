package com.devStudy.chatapp.websocket.events;

import com.devStudy.chatapp.websocket.client.MessageServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handler for retrying message save operations upon failure.
 * Listens for MessageSaveFailedEvent and attempts to retry saving the message
 */
@Component
public class MessageSaveRetryHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSaveRetryHandler.class);

    @Value("${websocket.message.MAX_RETRY_ATTEMPTS}")
    private int MAX_RETRY_ATTEMPTS;

    @Value("${websocket.message.INITIAL_RETRY_DELAY_MS}")
    private long INITIAL_RETRY_DELAY_MS;

    @Value("${websocket.message.RETRY_BACKOFF_MULTIPLIER}")
    private double RETRY_BACKOFF_MULTIPLIER;
    
    private final MessageServiceClient messageServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    
    @Autowired
    public MessageSaveRetryHandler(MessageServiceClient messageServiceClient,
                                 ApplicationEventPublisher eventPublisher) {
        this.messageServiceClient = messageServiceClient;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Handle message save failure events.
     * Use @Async to process retries asynchronously.
     */
    @Async
    @EventListener
    public void handleMessageSaveFailure(MessageSaveFailedEvent event) {
        if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            LOGGER.error("Message save failed after max retries - Chatroom ID: {}, Sender ID: {}, Reason: {}",
                        event.getChatroomId(), event.getMessageRequest().getSenderId(), event.getFailureReason());
            
            handleFinalFailure(event);
            return;
        }
        
        // calculate delay with exponential backoff
        long delayMs = calculateRetryDelay(event.getRetryCount());
        
        LOGGER.info("Retrying message save in {} ms - Chatroom ID: {}, Retry Count: {}",
                   delayMs, event.getChatroomId(), event.getRetryCount() + 1);
        
        // Schedule the retry after the calculated delay
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                        .execute(() -> retryMessageSave(event));
    }
    
    /**
     * Execute the retry logic
     */
    private void retryMessageSave(MessageSaveFailedEvent event) {
        boolean success = messageServiceClient.saveMessage(event.getChatroomId(), event.getMessageRequest());

        if (success) {
            LOGGER.info("Message save retry succeeded - Chatroom ID: {}, Retry Count: {}",
                       event.getChatroomId(), event.getRetryCount() + 1);
        } else {
            LOGGER.warn("Message save retry failed - Chatroom ID: {}, Retry Count: {}",
                       event.getChatroomId(), event.getRetryCount() + 1);

            // Publish a new failure event to trigger another retry
            publishRetryEvent(event);
        }
    }
    
    /**
     * Event publishing for retry
     */
    private void publishRetryEvent(MessageSaveFailedEvent originalEvent) {
        MessageSaveFailedEvent retryEvent = new MessageSaveFailedEvent(
            originalEvent.getChatroomId(),
            originalEvent.getMessageRequest(),
            originalEvent.getOriginalTimestamp(),
                "Service returned false",
            originalEvent.getRetryCount() + 1
        );
        
        eventPublisher.publishEvent(retryEvent);
    }
    
    /**
     * Calculate delay using exponential backoff
     */
    private long calculateRetryDelay(int retryCount) {
        return (long) (INITIAL_RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, retryCount));
    }
    
    /**
     * Handle the case when all retries have been exhausted
     * TODO: consider saving to a dead-letter queue or alerting
     */
    private void handleFinalFailure(MessageSaveFailedEvent event) {
        LOGGER.error("Message save failed after all retries - Chatroom ID: {}, Sender ID: {}, Reason: {}",
                    event.getChatroomId(), event.getMessageRequest().getSenderId(), event.getFailureReason());
    }
}