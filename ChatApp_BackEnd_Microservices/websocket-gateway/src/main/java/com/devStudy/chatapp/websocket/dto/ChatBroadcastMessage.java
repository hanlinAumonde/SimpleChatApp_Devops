package com.devStudy.chatapp.websocket.dto;

import java.io.Serializable;

public record ChatBroadcastMessage(
        int messageType,        // 消息类型: MESSAGE_TEXT, MESSAGE_CONNECT, MESSAGE_DISCONNECT
        String broadcastType,   // 广播类型: TO_ALL_IN_CHATROOM, TO_OTHERS_IN_CHATROOM, TO_SELF_IN_CHATROOM  
        String message,         // 消息内容
        UserDTO sender,         // 发送者信息
        String timestamp,       // 时间戳
        String instanceId       // 实例ID，用于避免重复处理
) implements Serializable {
}