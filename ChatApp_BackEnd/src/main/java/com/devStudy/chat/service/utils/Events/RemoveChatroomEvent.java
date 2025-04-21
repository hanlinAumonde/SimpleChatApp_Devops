package com.devStudy.chat.service.utils.Events;

import org.springframework.context.ApplicationEvent;

import java.io.Serial;

public class RemoveChatroomEvent extends ApplicationEvent {
	
	@Serial
	private static final long serialVersionUID = -4118480440900563692L;
	private final long chatroomId;
	
	public RemoveChatroomEvent(long chatroomId) {
		super(chatroomId);
		this.chatroomId = chatroomId;
	}
	
	public long getEventMsg() {
		return this.chatroomId;
	}
	
}
