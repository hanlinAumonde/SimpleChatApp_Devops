package com.devStudy.chatapp.auth.security.loginVerificationCode;

import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.service.UserService;
import com.devStudy.chatapp.auth.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeAuthenticationProviderTest {

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private UserService userService;

    @InjectMocks
    private VerificationCodeAuthenticationProvider authenticationProvider;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_CODE = "123456";
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private User testUser;
    private VerificationCodeAuthenticationToken validToken;
    private VerificationCodeAuthenticationToken invalidToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationProvider, "MAX_FAILED_ATTEMPTS", MAX_FAILED_ATTEMPTS);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setMail(TEST_EMAIL);
        testUser.setPwd("encodedPassword");
        testUser.setAdmin(false);
        testUser.setActive(true);

        validToken = VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);
        invalidToken = VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, "wrongcode");
    }

    @Test
    void testAuthenticate_ValidCode_Success() {
        // Mock dependencies
        when(verificationCodeService.validateCode(TEST_EMAIL, TEST_CODE)).thenReturn(true);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);

        // Execute test
        Authentication result = authenticationProvider.authenticate(validToken);

        // Verify results
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertInstanceOf(VerificationCodeAuthenticationToken.class, result);
        assertEquals(testUser, result.getPrincipal());
        assertFalse(result.getAuthorities().isEmpty());

        // Verify method calls
        verify(verificationCodeService).validateCode(TEST_EMAIL, TEST_CODE);
        verify(userService).loadUserByUsername(TEST_EMAIL);
        verify(verificationCodeService, never()).incrementLoginAttempts(anyString());
    }

    @Test
    void testAuthenticate_InvalidCode_FirstAttempt() {
        // Mock dependencies
        when(verificationCodeService.validateCode(TEST_EMAIL, "wrongcode")).thenReturn(false);
        when(verificationCodeService.incrementLoginAttempts(TEST_EMAIL)).thenReturn(1);

        // Execute test and verify exception
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Code incorrect. Plus que 2 tentatives avant l'invalidation du code", 
                     exception.getMessage());

        // Verify method calls
        verify(verificationCodeService).validateCode(TEST_EMAIL, "wrongcode");
        verify(verificationCodeService).incrementLoginAttempts(TEST_EMAIL);
        verify(verificationCodeService, never()).invalideteCode(anyString());
        verify(userService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testAuthenticate_InvalidCode_SecondAttempt() {
        when(verificationCodeService.validateCode(TEST_EMAIL, "wrongcode")).thenReturn(false);
        when(verificationCodeService.incrementLoginAttempts(TEST_EMAIL)).thenReturn(2);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Code incorrect. Plus que 1 tentatives avant l'invalidation du code", 
                     exception.getMessage());

        verify(verificationCodeService).incrementLoginAttempts(TEST_EMAIL);
        verify(verificationCodeService, never()).invalideteCode(anyString());
    }

    @Test
    void testAuthenticate_InvalidCode_MaxAttemptsReached() {
        when(verificationCodeService.validateCode(TEST_EMAIL, "wrongcode")).thenReturn(false);
        when(verificationCodeService.incrementLoginAttempts(TEST_EMAIL)).thenReturn(MAX_FAILED_ATTEMPTS);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Trops de tentatives malveillantes, le code de vérification est devenu invalide, veuillez le redemander", 
                     exception.getMessage());

        verify(verificationCodeService).incrementLoginAttempts(TEST_EMAIL);
        verify(verificationCodeService).invalideteCode(TEST_EMAIL);
    }

    @Test
    void testAuthenticate_InvalidCode_ExceedsMaxAttempts() {
        when(verificationCodeService.validateCode(TEST_EMAIL, "wrongcode")).thenReturn(false);
        when(verificationCodeService.incrementLoginAttempts(TEST_EMAIL)).thenReturn(5); // 超过最大次数

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Trops de tentatives malveillantes, le code de vérification est devenu invalide, veuillez le redemander", 
                     exception.getMessage());

        verify(verificationCodeService).incrementLoginAttempts(TEST_EMAIL);
        verify(verificationCodeService).invalideteCode(TEST_EMAIL);
    }

    @Test
    void testAuthenticate_UnsupportedTokenType_ThrowsException() {
        UsernamePasswordAuthenticationToken unsupportedToken = 
                new UsernamePasswordAuthenticationToken(TEST_EMAIL, "password");

        assertThrows(
                IllegalArgumentException.class,
                () -> authenticationProvider.authenticate(unsupportedToken)
        );

        verify(verificationCodeService, never()).validateCode(anyString(), anyString());
        verify(userService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testSupports_VerificationCodeAuthenticationToken_True() {
        boolean result = authenticationProvider.supports(VerificationCodeAuthenticationToken.class);
        assertTrue(result);
    }

    @Test
    void testSupports_UsernamePasswordAuthenticationToken_False() {
        boolean result = authenticationProvider.supports(UsernamePasswordAuthenticationToken.class);
        assertFalse(result);
    }

    @Test
    void testAuthenticate_UserServiceException() {
        when(verificationCodeService.validateCode(TEST_EMAIL, TEST_CODE)).thenReturn(true);
        when(userService.loadUserByUsername(TEST_EMAIL))
                .thenThrow(new RuntimeException("User service error"));

        assertThrows(
                RuntimeException.class,
                () -> authenticationProvider.authenticate(validToken)
        );

        verify(verificationCodeService).validateCode(TEST_EMAIL, TEST_CODE);
        verify(userService).loadUserByUsername(TEST_EMAIL);
    }

    @Test
    void testAuthenticate_VerificationCodeServiceException() {
        when(verificationCodeService.validateCode(TEST_EMAIL, TEST_CODE))
                .thenThrow(new BadCredentialsException("Verification code expired"));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(validToken)
        );

        verify(verificationCodeService).validateCode(TEST_EMAIL, TEST_CODE);
        verify(userService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testAuthenticate_TokenDetailsPreserved() {
        Object tokenDetails = "some-details";
        validToken.setDetails(tokenDetails);

        when(verificationCodeService.validateCode(TEST_EMAIL, TEST_CODE)).thenReturn(true);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);

        Authentication result = authenticationProvider.authenticate(validToken);

        assertNotNull(result);
        assertEquals(tokenDetails, result.getDetails());
    }

    @Test
    void testAuthenticate_UserWithDifferentAuthorities() {
        // 创建管理员用户
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setMail(TEST_EMAIL);
        adminUser.setAdmin(true);
        adminUser.setActive(true);

        when(verificationCodeService.validateCode(TEST_EMAIL, TEST_CODE)).thenReturn(true);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(adminUser);

        Authentication result = authenticationProvider.authenticate(validToken);

        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(adminUser, result.getPrincipal());
        
        // 验证管理员权限
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testFailedLoginAttemptsException_EdgeCase_ZeroAttempts() {
        when(verificationCodeService.validateCode(TEST_EMAIL, "wrongcode")).thenReturn(false);
        when(verificationCodeService.incrementLoginAttempts(TEST_EMAIL)).thenReturn(0);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        // 当尝试次数为0时，剩余次数应该是MAX_FAILED_ATTEMPTS
        assertEquals("Code incorrect. Plus que 3 tentatives avant l'invalidation du code", 
                     exception.getMessage());
    }
}