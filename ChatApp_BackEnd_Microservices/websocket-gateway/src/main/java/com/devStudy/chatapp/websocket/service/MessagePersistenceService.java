package com.devStudy.chatapp.websocket.service;

import com.devStudy.chatapp.websocket.client.MessageServiceClient;
import com.devStudy.chatapp.websocket.dto.SaveMessageRequest;
import com.devStudy.chatapp.websocket.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MessagePersistenceService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePersistenceService.class);

    private final MessageServiceClient messageServiceClient;

    @Autowired
    public MessagePersistenceService(MessageServiceClient messageServiceClient) {
        this.messageServiceClient = messageServiceClient;
    }

    /**
     * 异步保存消息到消息服务
     * @param chatroomId 聊天室ID
     * @param userInfo 发送者信息
     * @param content 消息内容
     * @param timestamp 时间戳
     */
    @Async
    public void saveMessageAsync(long chatroomId, UserDTO userInfo, String content, Date timestamp) {
        try {
            SaveMessageRequest messageRequest = createMessageRequest(userInfo, content, timestamp);
            messageServiceClient.saveMessage(chatroomId, messageRequest);
            
            LOGGER.debug("Message saved successfully for user {} in chatroom {}", 
                        userInfo.getId(), chatroomId);
                        
        } catch (Exception e) {
            LOGGER.error("Failed to save message asynchronously for user {} in chatroom {}: {}", 
                        userInfo.getId(), chatroomId, e.getMessage());
            // 暂时只记录错误日志，不影响WebSocket实时通信
            // TODO: 后续可考虑引入重试机制或消息队列
        }
    }

    /**
     * 构建消息保存请求
     */
    private SaveMessageRequest createMessageRequest(UserDTO userInfo, String content, Date timestamp) {
        SaveMessageRequest request = new SaveMessageRequest();
        request.setSenderId(userInfo.getId());
        request.setSenderFirstName(userInfo.getFirstName());
        request.setSenderLastName(userInfo.getLastName());
        request.setSenderMail(userInfo.getMail());
        request.setContent(content);
        request.setTimestamp(timestamp);
        return request;
    }

//    /**
//     * 同步保存消息（用于关键消息或fallback场景）
//     * @param chatroomId 聊天室ID
//     * @param userInfo 发送者信息
//     * @param content 消息内容
//     * @param timestamp 时间戳
//     * @return 是否保存成功
//     */
//    public boolean saveMessageSync(long chatroomId, UserDTO userInfo, String content, Date timestamp) {
//        try {
//            SaveMessageRequest messageRequest = createMessageRequest(userInfo, content, timestamp);
//            messageServiceClient.saveMessage(chatroomId, messageRequest);
//
//            LOGGER.debug("Message saved synchronously for user {} in chatroom {}",
//                        userInfo.getId(), chatroomId);
//            return true;
//
//        } catch (Exception e) {
//            LOGGER.error("Failed to save message synchronously for user {} in chatroom {}: {}",
//                        userInfo.getId(), chatroomId, e.getMessage());
//            return false;
//        }
//    }
}