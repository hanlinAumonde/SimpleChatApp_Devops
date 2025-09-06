package com.devStudy.chatapp.auth.service.Interface;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

public interface IJwtTokenService {
    String generateJwtToken(String email, String tokenFlag);
    boolean validateToken(String token);
    String validateTokenAndGetEmail(String token);
    Date getExpirationDate(String token);
    String getTokenFromCookie(HttpServletRequest request);
}
