package com.devStudy.chat.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.devStudy.chat.dto.UserDTO;

@Document(collection = "Chat_Messages")
public class ChatMessage {
	
	@Id
	private ObjectId id;
	
	@Field("chatroomId")
	@Indexed
	private long chatroomId;
	
	@Field("sender")
	private UserDTO sender;
	
	@Field("content")
	private String content;
	
	@Field("timestamp")
	@Indexed(direction = IndexDirection.DESCENDING)
	private Date timestamp;
	
	public ChatMessage() {
	}
	
	// Getters and Setters
	public ObjectId getId() {
		return id;
	}
	
	public void setId(ObjectId id) {
		this.id = id;
	}
	
	public long getChatroomId() {
		return chatroomId;
	}
	
	public void setChatroomId(long chatroomId) {
		this.chatroomId = chatroomId;
	}
	
	public UserDTO getUser() {
		return sender;
	}
	
	public void setUser(UserDTO user) {
		this.sender = user;
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
}
