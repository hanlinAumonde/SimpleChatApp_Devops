package com.devStudy.chatapp.websocket.dto;

public class WebSocketConstants {

    // WebSocket消息类型
    public static final int MESSAGE_TEXT = 0;
    public static final int MESSAGE_CONNECT = 1; 
    public static final int MESSAGE_DISCONNECT = 2;
    public static final int MESSAGE_REMOVE_CHATROOM = 3;
    public static final int MESSAGE_ADD_CHATROOM_MEMBER = 4;
    public static final int MESSAGE_REMOVE_CHATROOM_MEMBER = 5;

    // 消息广播类型
    public static final String TO_ALL_IN_CHATROOM = "ALL";
    public static final String TO_OTHERS_IN_CHATROOM = "OTHERS"; 
    public static final String TO_SELF_IN_CHATROOM = "SELF";

    private WebSocketConstants() {}
}