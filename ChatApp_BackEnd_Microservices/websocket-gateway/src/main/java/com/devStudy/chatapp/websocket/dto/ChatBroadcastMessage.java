package com.devStudy.chatapp.websocket.dto;

import java.io.Serializable;

public record ChatBroadcastMessage(
        int messageType,        // Message Type: MESSAGE_TEXT, MESSAGE_CONNECT, MESSAGE_DISCONNECT
        String broadcastType,   // Broadcast Type: TO_ALL_IN_CHATROOM, TO_OTHERS_IN_CHATROOM, TO_SELF_IN_CHATROOM
        String message,         // Message content
        UserDTO sender,         // Sender information
        String timestamp,       // Timestamp of the message
        String instanceId       // Instance ID of the WebSocket server
) implements Serializable {
}