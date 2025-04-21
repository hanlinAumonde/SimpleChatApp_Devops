package com.devStudy.chat.service.interfaces;

import java.util.Date;
import java.util.List;

import com.devStudy.chat.dto.ChatMsgDTO;
import com.devStudy.chat.dto.UserDTO;

public interface ChatMessageServiceInt {
	
	public void saveMsgIntoCollection(long chatroomId, UserDTO sender, String content, Date timestamp);
	
	public List<ChatMsgDTO> getChatMessagesByChatroomId(long chatroomId);
	
	public List<ChatMsgDTO> getChatMessagesByChatroomIdByPage(long chatroomId, int page);
}
