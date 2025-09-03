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
}