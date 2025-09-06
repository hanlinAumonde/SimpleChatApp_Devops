package com.devStudy.chatapp.auth.service.Interface;

public interface IVerificationCodeService {
    void sendCode(String email);
    boolean validateCode(String email, String code);
    int incrementLoginAttempts(String email);
    void invalideteCode(String email);

}
