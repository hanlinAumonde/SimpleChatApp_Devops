package com.devStudy.chatapp.message.service.Interface;

import java.util.Date;
import java.util.List;

import com.devStudy.chatapp.message.dto.ChatMsgDTO;
import com.devStudy.chatapp.message.dto.UserDTO;

public interface IChatMessageService {
    void saveMsgIntoCollection(long chatroomId, UserDTO sender, String content, Date timestamp);
    List<ChatMsgDTO> getChatMessagesByChatroomId(long chatroomId);
    List<ChatMsgDTO> getChatMessagesByChatroomIdByPage(long chatroomId, int page);
    List<ChatMsgDTO> getChatMessagesByChatroomIdByPage(long chatroomId, int page, long currentUserId);
}