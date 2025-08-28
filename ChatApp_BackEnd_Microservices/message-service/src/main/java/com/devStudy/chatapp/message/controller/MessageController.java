package com.devStudy.chatapp.message.controller;

import java.util.Date;
import java.util.List;

import com.devStudy.chatapp.message.service.Implementation.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.devStudy.chatapp.message.dto.ChatMsgDTO;
import com.devStudy.chatapp.message.dto.SaveMessageRequest;
import com.devStudy.chatapp.message.dto.UserDTO;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageService chatMessageService;

    @Autowired
    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * 获取聊天室的消息历史（分页）
     * @param chatroomId 聊天室ID
     * @param page 页码，默认为0
     * @param userIdHeader 来自网关的用户ID
     * @return 消息列表
     */
    @GetMapping("/chatrooms/{chatroomId}/history")
    public ResponseEntity<List<ChatMsgDTO>> getHistoryMsgByChatroomIdAndPage(
            @PathVariable long chatroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        
        Long currentUserId = null;
        if (userIdHeader != null) {
            try {
                currentUserId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                // 如果解析失败，忽略用户ID
            }
        }
        
        List<ChatMsgDTO> messages;
        if (currentUserId != null) {
            messages = chatMessageService.getChatMessagesByChatroomIdByPage(chatroomId, page, currentUserId);
        } else {
            messages = chatMessageService.getChatMessagesByChatroomIdByPage(chatroomId, page);
        }
        
        return ResponseEntity.ok(messages);
    }

    /**
     * 保存消息（通常由WebSocket网关调用）
     * @param chatroomId 聊天室ID
     * @param request 消息保存请求
     * @return 成功响应
     */
    @PostMapping("/chatrooms/{chatroomId}/save")
    public ResponseEntity<Void> saveMessage(
            @PathVariable long chatroomId,
            @RequestBody SaveMessageRequest request) {
        
        UserDTO sender = new UserDTO();
        sender.setId(request.getSenderId());
        sender.setFirstName(request.getSenderFirstName());
        sender.setLastName(request.getSenderLastName());
        sender.setMail(request.getSenderMail());
        
        chatMessageService.saveMsgIntoCollection(
            chatroomId, 
            sender, 
            request.getContent(), 
            request.getTimestamp() != null ? request.getTimestamp() : new Date()
        );
        
        return ResponseEntity.ok().build();
    }
}