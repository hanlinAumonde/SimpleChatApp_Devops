package com.devStudy.chat.service.interfaces;

import java.util.Map;

public interface VerificationCodeServiceInt {
    void sendCode(String email);
    boolean validateCode(String email, String code);
    int incrementLoginAttempts(String email);
    void invalideteCode(String email);
}
