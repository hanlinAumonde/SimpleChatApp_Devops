package com.devStudy.chatapp.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static com.devStudy.chatapp.auth.utils.ConstantValues.JWT_TOKEN_COOKIE_NAME;
import static com.devStudy.chatapp.auth.utils.ConstantValues.TOKEN_FLAG_LOGIN;
import static com.devStudy.chatapp.auth.utils.ConstantValues.TOKEN_FLAG_RESET_PASSWORD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZHVjdGlvbg==";
    private static final Long LOGIN_TOKEN_EXPIRATION = 60L; // 60 minutes
    private static final Long RESET_PWD_TOKEN_EXPIRATION = 15L; // 15 minutes

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenService, "loginTokenExpirationTime", LOGIN_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenService, "resetPwdTokenExpirationTime", RESET_PWD_TOKEN_EXPIRATION);
    }

    @Test
    void testGenerateJwtToken_LoginToken() {
        String email = "test@example.com";
        
        String token = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_LOGIN);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // 验证token可以被解析
        String extractedEmail = jwtTokenService.validateTokenAndGetEmail(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testGenerateJwtToken_ResetPasswordToken() {
        String email = "test@example.com";
        
        String token = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_RESET_PASSWORD);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // 验证token可以被解析
        String extractedEmail = jwtTokenService.validateTokenAndGetEmail(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testValidateToken_ValidToken() {
        String email = "test@example.com";
        String token = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_LOGIN);
        
        boolean isValid = jwtTokenService.validateToken(token);
        
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        boolean isValid = jwtTokenService.validateToken(invalidToken);
        
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_EmptyToken() {
        boolean isValid = jwtTokenService.validateToken("");
        
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_NullToken() {
        boolean isValid = jwtTokenService.validateToken(null);
        
        assertFalse(isValid);
    }

    @Test
    void testValidateTokenAndGetEmail_ValidToken() {
        String email = "test@example.com";
        String token = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_LOGIN);
        
        String extractedEmail = jwtTokenService.validateTokenAndGetEmail(token);
        
        assertEquals(email, extractedEmail);
    }

    @Test
    void testValidateTokenAndGetEmail_InvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        String extractedEmail = jwtTokenService.validateTokenAndGetEmail(invalidToken);
        
        assertNull(extractedEmail);
    }

    @Test
    void testValidateTokenAndGetEmail_EmptyToken() {
        String extractedEmail = jwtTokenService.validateTokenAndGetEmail("");
        
        assertNull(extractedEmail);
    }

    @Test
    void testGetExpirationDate_ValidToken() {
        String email = "test@example.com";
        String token = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_LOGIN);
        
        Date expirationDate = jwtTokenService.getExpirationDate(token);
        
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
        
        // 验证过期时间大约是60分钟后（允许1分钟误差）
        long expectedExpiration = System.currentTimeMillis() + (LOGIN_TOKEN_EXPIRATION * 60 * 1000);
        long actualExpiration = expirationDate.getTime();
        assertTrue(Math.abs(expectedExpiration - actualExpiration) < 60000); // 1分钟误差
    }

    @Test
    void testGetExpirationDate_InvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        Date expirationDate = jwtTokenService.getExpirationDate(invalidToken);
        
        assertNull(expirationDate);
    }

    @Test
    void testGetTokenFromCookie_TokenExists() {
        String tokenValue = "test.jwt.token";
        Cookie jwtCookie = new Cookie(JWT_TOKEN_COOKIE_NAME, tokenValue);
        Cookie[] cookies = {
                new Cookie("other-cookie", "other-value"),
                jwtCookie,
                new Cookie("another-cookie", "another-value")
        };
        
        when(request.getCookies()).thenReturn(cookies);
        
        String extractedToken = jwtTokenService.getTokenFromCookie(request);
        
        assertEquals(tokenValue, extractedToken);
    }

    @Test
    void testGetTokenFromCookie_TokenNotExists() {
        Cookie[] cookies = {
                new Cookie("other-cookie", "other-value"),
                new Cookie("session-id", "session-value")
        };
        
        when(request.getCookies()).thenReturn(cookies);
        
        String extractedToken = jwtTokenService.getTokenFromCookie(request);
        
        assertNull(extractedToken);
    }

    @Test
    void testGetTokenFromCookie_NoCookies() {
        when(request.getCookies()).thenReturn(null);
        
        String extractedToken = jwtTokenService.getTokenFromCookie(request);
        
        assertNull(extractedToken);
    }

    @Test
    void testGetTokenFromCookie_EmptyCookies() {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        
        String extractedToken = jwtTokenService.getTokenFromCookie(request);
        
        assertNull(extractedToken);
    }

    @Test
    void testTokenExpirationDifference() {
        String email = "test@example.com";
        
        // 生成登录token和重置密码token
        String loginToken = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_LOGIN);
        String resetToken = jwtTokenService.generateJwtToken(email, TOKEN_FLAG_RESET_PASSWORD);
        
        Date loginExpiration = jwtTokenService.getExpirationDate(loginToken);
        Date resetExpiration = jwtTokenService.getExpirationDate(resetToken);
        
        // 登录token应该比重置密码token有更长的有效期
        assertTrue(loginExpiration.after(resetExpiration));
        
        // 验证时间差大约是45分钟 (60-15)
        long timeDifference = loginExpiration.getTime() - resetExpiration.getTime();
        long expectedDifference = (LOGIN_TOKEN_EXPIRATION - RESET_PWD_TOKEN_EXPIRATION) * 60 * 1000;
        assertTrue(Math.abs(timeDifference - expectedDifference) < 10000); // 允许10秒误差
    }
}