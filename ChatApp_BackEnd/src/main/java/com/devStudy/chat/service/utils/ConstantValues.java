package com.devStudy.chat.service.utils;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public final class ConstantValues {
	
	private ConstantValues() {}
	
	// Msg for creation of user compte
	public static final String CreationSuccess = "create compte";
	public static final String CompteExist = "compte already exists";

    // Date format for chatroom start date
	public static final DateTimeFormatter ISO_LOCAL_DATETIME_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

	// Max failed attempts for login
    //public static final int MAX_FAILED_ATTEMPTS = 5;
    
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

    // Jwt token flag
    public static final String TOKEN_FLAG_RESET_PASSWORD = "resetPassword";
    public static final String TOKEN_FLAG_LOGIN = "login";

    // verification code login - name of parameter in the request
    public static final String PRINCIPAL_PARAMETER = "username";
    public static final String CREDENTIALS_PARAMETER = "verification-code";

    // RabbitMQ exchange name ,queue name and routing key
    public static final String RABBITMQ_EXCHANGE_NAME = "topics-exchange";
    public static final String RABBITMQ_QUEUE_Q1 = "sendMail-queue-q1";
    public static final String RABBITMQ_QUEUE_Q2 = "sendMail-queue-q2";
    public static final String ROUTING_KEY_RET_PASSWORD = "mail.resetPassword";
    public static final String ROUTING_KEY_VERIFICATION_CODE = "mail.verificationCode";

    // JWT token - cookie name
    public static final String JWT_TOKEN_COOKIE_NAME = "JWT-Token";

    // Redis keys for verification code and attempts
    public static final String CODE_PREFIX = "verification:code:";
    public static final String ATTEMPTS_PREFIX = "verification:attempts:";
    public static final String BLACKLIST_PREFIX = "token:blacklist:";
}
