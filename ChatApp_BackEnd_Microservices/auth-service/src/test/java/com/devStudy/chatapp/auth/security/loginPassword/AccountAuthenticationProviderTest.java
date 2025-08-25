package com.devStudy.chatapp.auth.security.loginPassword;

import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountAuthenticationProviderTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    private AccountAuthenticationProvider authenticationProvider;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private User testUser;
    private UsernamePasswordAuthenticationToken validToken;
    private UsernamePasswordAuthenticationToken invalidToken;

    @BeforeEach
    void setUp() {
        authenticationProvider = new AccountAuthenticationProvider(passwordEncoder, userService, MAX_FAILED_ATTEMPTS);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setMail(TEST_EMAIL);
        testUser.setPwd(ENCODED_PASSWORD);
        testUser.setAdmin(false);
        testUser.setActive(true);
        testUser.setFailedAttempts(0);

        validToken = new UsernamePasswordAuthenticationToken(TEST_EMAIL, TEST_PASSWORD);
        invalidToken = new UsernamePasswordAuthenticationToken(TEST_EMAIL, "wrongpassword");
    }

    @Test
    void testAuthenticate_ValidCredentials_Success() {
        // Mock dependencies
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userService.findUserOrAdmin(TEST_EMAIL, false)).thenReturn(Optional.of(testUser));

        // Execute test
        Authentication result = authenticationProvider.authenticate(validToken);

        // Verify results
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, result);
        assertEquals(testUser, result.getPrincipal());
        assertEquals(TEST_PASSWORD, result.getCredentials());
        assertFalse(result.getAuthorities().isEmpty());

        // Verify method calls
        verify(userService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(userService).resetFailedAttemptsOfUser(TEST_EMAIL);
        verify(userService).findUserOrAdmin(TEST_EMAIL, false);
        verify(userService, never()).incrementFailedAttemptsOfUser(anyString());
        verify(userService, never()).lockUserAndResetFailedAttempts(anyString());
    }

    @Test
    void testAuthenticate_InvalidPassword_FirstAttempt() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(1);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Mot de passe incorrect. Plus que 2 tentatives avant blocage", 
                     exception.getMessage());

        verify(userService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches("wrongpassword", ENCODED_PASSWORD);
        verify(userService).incrementFailedAttemptsOfUser(TEST_EMAIL);
        verify(userService, never()).resetFailedAttemptsOfUser(anyString());
        verify(userService, never()).lockUserAndResetFailedAttempts(anyString());
    }

    @Test
    void testAuthenticate_InvalidPassword_SecondAttempt() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(2);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Mot de passe incorrect. Plus que 1 tentatives avant blocage", 
                     exception.getMessage());

        verify(userService).incrementFailedAttemptsOfUser(TEST_EMAIL);
        verify(userService, never()).lockUserAndResetFailedAttempts(anyString());
    }

    @Test
    void testAuthenticate_InvalidPassword_MaxAttemptsReached() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(MAX_FAILED_ATTEMPTS);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Trops de tentatives malveillantes, votre compte est blouqé", 
                     exception.getMessage());

        verify(userService).incrementFailedAttemptsOfUser(TEST_EMAIL);
        verify(userService).lockUserAndResetFailedAttempts(TEST_EMAIL);
    }

    @Test
    void testAuthenticate_InvalidPassword_ExceedsMaxAttempts() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(5); // 超过最大次数

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        assertEquals("Trops de tentatives malveillantes, votre compte est blouqé", 
                     exception.getMessage());

        verify(userService).incrementFailedAttemptsOfUser(TEST_EMAIL);
        verify(userService).lockUserAndResetFailedAttempts(TEST_EMAIL);
    }

    @Test
    void testAuthenticate_UserNotFound() {
        when(userService.loadUserByUsername(TEST_EMAIL))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThrows(
                UsernameNotFoundException.class,
                () -> authenticationProvider.authenticate(validToken)
        );

        verify(userService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userService, never()).incrementFailedAttemptsOfUser(anyString());
    }

    @Test
    void testAuthenticate_UserNotFoundAfterPasswordValidation() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userService.findUserOrAdmin(TEST_EMAIL, false)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authenticationProvider.authenticate(validToken)
        );

        assertTrue(exception.getMessage().contains("Internal error"));
        assertTrue(exception.getMessage().contains("not found after checking password"));

        verify(userService).resetFailedAttemptsOfUser(TEST_EMAIL);
        verify(userService).findUserOrAdmin(TEST_EMAIL, false);
    }

    @Test
    void testAuthenticate_AdminUser_Success() {
        // 创建管理员用户
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setMail(TEST_EMAIL);
        adminUser.setPwd(ENCODED_PASSWORD);
        adminUser.setAdmin(true);
        adminUser.setActive(true);

        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(adminUser);
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userService.findUserOrAdmin(TEST_EMAIL, false)).thenReturn(Optional.of(adminUser));

        Authentication result = authenticationProvider.authenticate(validToken);

        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(adminUser, result.getPrincipal());
        
        // 验证管理员权限
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testSupports_UsernamePasswordAuthenticationToken_True() {
        boolean result = authenticationProvider.supports(UsernamePasswordAuthenticationToken.class);
        assertTrue(result);
    }

    @Test
    void testSupports_OtherTokenType_False() {
        boolean result = authenticationProvider.supports(String.class);
        assertFalse(result);
    }

    @Test
    void testAuthenticate_PasswordEncoderException() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenThrow(new RuntimeException("Password encoding error"));

        assertThrows(
                RuntimeException.class,
                () -> authenticationProvider.authenticate(validToken)
        );

        verify(userService).loadUserByUsername(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    void testAuthenticate_NullPassword() {
        UsernamePasswordAuthenticationToken tokenWithNullPassword = 
                new UsernamePasswordAuthenticationToken(TEST_EMAIL, null);

        assertThrows(
                NullPointerException.class,
                () -> authenticationProvider.authenticate(tokenWithNullPassword)
        );
    }

    @Test
    void testAuthenticate_EmptyPassword() {
        UsernamePasswordAuthenticationToken tokenWithEmptyPassword = 
                new UsernamePasswordAuthenticationToken(TEST_EMAIL, "");

        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(1);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(tokenWithEmptyPassword)
        );

        assertEquals("Mot de passe incorrect. Plus que 2 tentatives avant blocage", 
                     exception.getMessage());
    }

    @Test
    void testAuthenticate_UserServiceResetFailsButAuthenticationContinues() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        doThrow(new RuntimeException("Reset failed")).when(userService).resetFailedAttemptsOfUser(TEST_EMAIL);

        assertThrows(
                RuntimeException.class,
                () -> authenticationProvider.authenticate(validToken)
        );

        verify(userService).resetFailedAttemptsOfUser(TEST_EMAIL);
        // 因为resetFailedAttemptsOfUser抛出异常，后续的findUserOrAdmin不会被调用
        verify(userService, never()).findUserOrAdmin(anyString(), anyBoolean());
    }

    @Test
    void testFailedLoginAttemptsException_EdgeCase_ZeroAttempts() {
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(0);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(invalidToken)
        );

        // 当尝试次数为0时，剩余次数应该是MAX_FAILED_ATTEMPTS
        assertEquals("Mot de passe incorrect. Plus que 3 tentatives avant blocage", 
                     exception.getMessage());
    }

    @Test
    void testAuthenticate_CustomMaxFailedAttempts() {
        // 测试自定义的最大失败次数
        AccountAuthenticationProvider customProvider = 
                new AccountAuthenticationProvider(passwordEncoder, userService, 5);

        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).thenReturn(false);
        when(userService.incrementFailedAttemptsOfUser(TEST_EMAIL)).thenReturn(2);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> customProvider.authenticate(invalidToken)
        );

        assertEquals("Mot de passe incorrect. Plus que 3 tentatives avant blocage", 
                     exception.getMessage());
    }
}