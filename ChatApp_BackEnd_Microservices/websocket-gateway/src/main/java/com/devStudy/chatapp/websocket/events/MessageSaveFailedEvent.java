package com.devStudy.chatapp.websocket.events;

import com.devStudy.chatapp.websocket.dto.SaveMessageRequest;

import java.util.Date;

/**
 * Event indicating a failure to save a chat message.
 * This event can be published to notify other components of the failure.
 */
public class MessageSaveFailedEvent {
    
    private final long chatroomId;
    private final SaveMessageRequest messageRequest;
    private final Date originalTimestamp;
    private final String failureReason;
    private final int retryCount;
    
    public MessageSaveFailedEvent(long chatroomId, SaveMessageRequest messageRequest, 
                                Date originalTimestamp, String failureReason, int retryCount) {
        this.chatroomId = chatroomId;
        this.messageRequest = messageRequest;
        this.originalTimestamp = originalTimestamp;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
    }
    
    // Getters
    public long getChatroomId() { return chatroomId; }
    public SaveMessageRequest getMessageRequest() { return messageRequest; }
    public Date getOriginalTimestamp() { return originalTimestamp; }
    public String getFailureReason() { return failureReason; }
    public int getRetryCount() { return retryCount; }
    
    @Override
    public String toString() {
        return String.format("MessageSaveFailedEvent{chatroomId=%d, senderId=%d, retryCount=%d, reason='%s'}", 
                           chatroomId, messageRequest.getSenderId(), retryCount, failureReason);
    }
}