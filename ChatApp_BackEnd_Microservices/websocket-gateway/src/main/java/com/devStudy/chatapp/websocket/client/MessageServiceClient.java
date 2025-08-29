package com.devStudy.chatapp.websocket.client;

import com.devStudy.chatapp.websocket.dto.SaveMessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 消息服务客户端
 * 用于异步保存聊天消息到MongoDB
 */
@FeignClient(name = "message-service")
public interface MessageServiceClient {
    
    /**
     * 保存聊天消息
     * @param chatroomId 聊天室ID
     * @param request 消息保存请求
     */
    @PostMapping("/api/messages/chatrooms/{chatroomId}/save")
    void saveMessage(@PathVariable("chatroomId") long chatroomId, 
                     @RequestBody SaveMessageRequest request);
}