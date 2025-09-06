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
     * Obtiene mensajes históricos por ID de sala de chat y página
     * @param chatroomId chatroom ID
     * @param page page number (0-based)
     * @param userIdHeader user ID from header (optional)
     * @return list of chat messages
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
                // If parsing fails, we simply treat it as null
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
     * Sava un mensaje en una sala de chat específica
     * @param chatroomId chatroom ID
     * @param request request body containing message details
     * @return response entity with success status
     */
    @PostMapping("/chatrooms/{chatroomId}/save")
    public ResponseEntity<Boolean> saveMessage(
            @PathVariable long chatroomId,
            @RequestBody SaveMessageRequest request) {
        
        UserDTO sender = new UserDTO();
        sender.setId(request.getSenderId());
        sender.setFirstName(request.getSenderFirstName());
        sender.setLastName(request.getSenderLastName());
        sender.setMail(request.getSenderMail());
        
        boolean result = chatMessageService.saveMsgIntoCollection(
            chatroomId, 
            sender, 
            request.getContent(), 
            request.getTimestamp() != null ? request.getTimestamp() : new Date()
        );
        
        return ResponseEntity.ok(result);
    }
}