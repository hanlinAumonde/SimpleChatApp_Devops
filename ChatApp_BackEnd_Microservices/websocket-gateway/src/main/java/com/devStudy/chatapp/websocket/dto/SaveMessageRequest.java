package com.devStudy.chatapp.websocket.dto;

import java.util.Date;

/**
 * 消息保存请求DTO
 * 与message-service中的SaveMessageRequest保持一致
 */
public class SaveMessageRequest {
    private long senderId;
    private String senderFirstName;
    private String senderLastName;
    private String senderMail;
    private String content;
    private Date timestamp;

    public SaveMessageRequest() {
    }

    // Getters and Setters
    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getSenderFirstName() {
        return senderFirstName;
    }

    public void setSenderFirstName(String senderFirstName) {
        this.senderFirstName = senderFirstName;
    }

    public String getSenderLastName() {
        return senderLastName;
    }

    public void setSenderLastName(String senderLastName) {
        this.senderLastName = senderLastName;
    }

    public String getSenderMail() {
        return senderMail;
    }

    public void setSenderMail(String senderMail) {
        this.senderMail = senderMail;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SaveMessageRequest{" +
                "senderId=" + senderId +
                ", senderFirstName='" + senderFirstName + '\'' +
                ", senderLastName='" + senderLastName + '\'' +
                ", senderMail='" + senderMail + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}