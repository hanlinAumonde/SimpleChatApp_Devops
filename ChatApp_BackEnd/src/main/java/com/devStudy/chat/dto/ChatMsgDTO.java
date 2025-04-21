package com.devStudy.chat.dto;

public class ChatMsgDTO {
	private int index;
	private long userId;
	private String username;
	private String message;
	private String timestamp;
	private boolean sentByUser;
	private String messageType;
	
	public ChatMsgDTO() {
	}
	
	// Getters and Setters
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public boolean isSentByUser() {
		return sentByUser;
	}
	
	public void setSentByUser(boolean isSentByUser) {
		this.sentByUser = isSentByUser;
	}
	
	public String getMessageType() {
		return messageType;
	}
	
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
}
