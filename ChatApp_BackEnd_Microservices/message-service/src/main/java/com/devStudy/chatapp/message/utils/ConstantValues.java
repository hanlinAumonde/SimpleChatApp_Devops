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

}