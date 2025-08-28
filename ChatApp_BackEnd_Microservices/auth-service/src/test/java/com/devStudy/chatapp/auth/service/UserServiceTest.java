package com.devStudy.chatapp.auth.service;

import com.devStudy.chatapp.auth.dto.UserDTO;
import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.repository.UserRepository;
import com.devStudy.chatapp.auth.service.Implementation.JwtTokenService;
import com.devStudy.chatapp.auth.service.Implementation.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService tokenService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setMail("john.doe@test.com");
        testUser.setPwd("encodedPassword");
        testUser.setAdmin(false);
        testUser.setActive(true);
        testUser.setFailedAttempts(2);
    }

    // 非事务方法 - 单元测试

    @Test
    void testGetLoggedUser_UserExists() {
        when(userRepository.findByMailAndAdmin("john.doe@test.com", false))
                .thenReturn(Optional.of(testUser));

        UserDTO result = userService.getLoggedUser("john.doe@test.com");

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getFirstName(), result.getFirstName());
        assertEquals(testUser.getLastName(), result.getLastName());
        assertEquals(testUser.getMail(), result.getMail());
        verify(userRepository).findByMailAndAdmin("john.doe@test.com", false);
    }

    @Test
    void testGetLoggedUser_UserNotExists() {
        when(userRepository.findByMailAndAdmin("nonexistent@test.com", false))
                .thenReturn(Optional.empty());

        UserDTO result = userService.getLoggedUser("nonexistent@test.com");

        assertNotNull(result);
        assertEquals(0L, result.getId());
        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getMail());
    }

    @Test
    void testFindUserOrAdmin_UserExists() {
        when(userRepository.findByMailAndAdmin("john.doe@test.com", false))
                .thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findUserOrAdmin("john.doe@test.com", false);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByMailAndAdmin("john.doe@test.com", false);
    }

    @Test
    void testFindUserOrAdmin_UserNotExists() {
        when(userRepository.findByMailAndAdmin("nonexistent@test.com", false))
                .thenReturn(Optional.empty());

        Optional<User> result = userService.findUserOrAdmin("nonexistent@test.com", false);

        assertFalse(result.isPresent());
        verify(userRepository).findByMailAndAdmin("nonexistent@test.com", false);
    }

    @Test
    void testResetPassword_ValidToken() {
        when(tokenService.validateTokenAndGetEmail("validToken"))
                .thenReturn("john.doe@test.com");
        when(passwordEncoder.encode("newPassword"))
                .thenReturn("encodedNewPassword");

        boolean result = userService.resetPassword("validToken", "newPassword");

        assertTrue(result);
        verify(tokenService).validateTokenAndGetEmail("validToken");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).updatePwd("john.doe@test.com", "encodedNewPassword");
    }

    @Test
    void testResetPassword_InvalidToken() {
        when(tokenService.validateTokenAndGetEmail("invalidToken"))
                .thenReturn(null);

        boolean result = userService.resetPassword("invalidToken", "newPassword");

        assertFalse(result);
        verify(tokenService).validateTokenAndGetEmail("invalidToken");
        verify(userRepository, never()).updatePwd(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void testResetPassword_EmptyEmail() {
        when(tokenService.validateTokenAndGetEmail("tokenWithEmptyEmail"))
                .thenReturn("");

        boolean result = userService.resetPassword("tokenWithEmptyEmail", "newPassword");

        assertFalse(result);
        verify(userRepository, never()).updatePwd(anyString(), anyString());
    }

    @Test
    void testLoadUserByUsername_Success() {
        when(userRepository.findByMailAndAdmin("john.doe@test.com", false))
                .thenReturn(Optional.of(testUser));

        UserDetails result = userService.loadUserByUsername("john.doe@test.com");

        assertNotNull(result);
        assertEquals(testUser, result);
        assertEquals("john.doe@test.com", result.getUsername());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isEnabled());
        assertFalse(result.getAuthorities().isEmpty());
        verify(userRepository).findByMailAndAdmin("john.doe@test.com", false);
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        when(userRepository.findByMailAndAdmin("nonexistent@test.com", false))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("nonexistent@test.com")
        );

        assertEquals("Identifiants incorrects", exception.getMessage());
    }

    @Test
    void testLoadUserByUsername_InactiveUser() {
        testUser.setActive(false);
        when(userRepository.findByMailAndAdmin("john.doe@test.com", false))
                .thenReturn(Optional.of(testUser));

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("john.doe@test.com")
        );

        assertEquals("Compte bloqué", exception.getMessage());
    }
}