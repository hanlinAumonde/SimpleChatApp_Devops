package com.devStudy.chatapp.message.utils;

import java.text.SimpleDateFormat;

public final class ConstantValues {
    
    private ConstantValues() {}
    
    // Date format for chat messages
    public static final SimpleDateFormat DateSignFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat ContentTimeStampFormat = new SimpleDateFormat("HH:mm");
    
    // Used for identifying the message type (sent with rest api)
    public static final String MSG_DATE_SIGN = "dateSign";
    public static final String MSG_CONTENT = "content";
    public static final String MSG_LATEST_DATE_SIGN = "latestDateSign";

    // WebSocket broadcast types
    public static final String TO_ALL_IN_CHATROOM = "toAll";
    public static final String TO_SELF_IN_CHATROOM = "toSelf";
    public static final String TO_OTHERS_IN_CHATROOM = "toOthers";
    
    // Message types for websocket
    public static final int MESSAGE_TEXT = 0;
    public static final int MESSAGE_CONNECT = 1;
    public static final int MESSAGE_DISCONNECT = 2;
    public static final int MESSAGE_REMOVE_CHATROOM = 3;
    public static final int MESSAGE_ADD_CHATROOM_MEMBER = 4;
    public static final int MESSAGE_REMOVE_CHATROOM_MEMBER = 5;

}