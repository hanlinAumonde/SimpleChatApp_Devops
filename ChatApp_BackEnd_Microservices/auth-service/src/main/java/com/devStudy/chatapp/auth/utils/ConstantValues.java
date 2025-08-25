package com.devStudy.chatapp.auth.utils;

public final class ConstantValues {
	
	private ConstantValues() {}
	
	// Msg for creation of user compte
	public static final String CreationSuccess = "create compte";
	public static final String CompteExist = "compte already exists";

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