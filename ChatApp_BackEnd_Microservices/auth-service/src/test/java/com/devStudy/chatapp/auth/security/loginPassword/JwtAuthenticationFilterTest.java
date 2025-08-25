package com.devStudy.chatapp.auth.security.loginPassword;

import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.service.BlackListService;
import com.devStudy.chatapp.auth.service.JwtTokenService;
import com.devStudy.chatapp.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserService userService;

    @Mock
    private BlackListService blackListService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TEST_TOKEN = "valid.jwt.token";
    private static final String TEST_EMAIL = "test@example.com";
    private User testUser;

    @BeforeEach
    void setUp() {
        // 重置SecurityContext
        SecurityContextHolder.clearContext();
        SecurityContextHolder.setContext(securityContext);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setMail(TEST_EMAIL);
        testUser.setPwd("encodedPassword");
        testUser.setAdmin(false);
        testUser.setActive(true);
    }

    @Test
    void testDoFilterInternal_ValidToken_AuthenticationSet() throws ServletException, IOException {
        // Mock setup
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);

        // Execute test
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify authentication was set
        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(request).setAttribute("userId", 1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_NoToken_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(blackListService, never()).isTokenInBlackList(anyString());
        verify(jwtTokenService, never()).validateTokenAndGetEmail(anyString());
        verify(userService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_TokenInBlackList_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenService, never()).validateTokenAndGetEmail(anyString());
        verify(userService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_InvalidToken_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(userService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_AlreadyAuthenticated_SkipsAuthentication() throws ServletException, IOException {
        Authentication existingAuth = mock(Authentication.class);
        
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(userService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_UserNotFound_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(userService).loadUserByUsername(TEST_EMAIL);
        verify(securityContext, never()).setAuthentication(any());
        verify(request, never()).setAttribute(eq("userId"), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_RuntimeException_PropagatesException() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL))
                .thenThrow(new RuntimeException("Service error"));

        assertThrows(
                RuntimeException.class,
                () -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)
        );

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_EmptyEmail_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(userService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WhitespaceEmail_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn("   ");
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername("   ")).thenReturn(testUser);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 虽然email是空白字符，但不为null，所以会尝试加载用户
        verify(userService).loadUserByUsername("   ");
        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_AdminUser_SetsCorrectAuthorities() throws ServletException, IOException {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setMail(TEST_EMAIL);
        adminUser.setAdmin(true);
        adminUser.setActive(true);

        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(adminUser);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(argThat(auth -> {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
            return token.getPrincipal().equals(adminUser) &&
                   token.getAuthorities().stream()
                           .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }));
        verify(request).setAttribute("userId", 2L);
    }

    @Test
    void testDoFilterInternal_InactiveUser_SetsAuthentication() throws ServletException, IOException {
        User inactiveUser = new User();
        inactiveUser.setId(3L);
        inactiveUser.setFirstName("Inactive");
        inactiveUser.setLastName("User");
        inactiveUser.setMail(TEST_EMAIL);
        inactiveUser.setActive(false);

        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(inactiveUser);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 过滤器不检查用户是否活跃，只是设置认证
        verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));
        verify(request).setAttribute("userId", 3L);
    }

    @Test
    void testDoFilterInternal_ValidToken_CorrectAuthenticationDetails() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(argThat(auth -> {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
            return token.getPrincipal().equals(testUser) &&
                   token.getCredentials() == null &&
                   token.getDetails() != null &&
                   !token.getAuthorities().isEmpty();
        }));
    }

    @Test
    void testDoFilterInternal_BlackListServiceException_PropagatesException() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN))
                .thenThrow(new RuntimeException("Redis connection error"));

        assertThrows(
                RuntimeException.class,
                () -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)
        );

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_JwtTokenServiceException_ContinuesWithoutAuthentication() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request))
                .thenThrow(new RuntimeException("JWT parsing error"));

        assertThrows(
                RuntimeException.class,
                () -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)
        );

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_UserIdAttribute_CorrectType() throws ServletException, IOException {
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(eq("userId"), argThat(userId -> 
            userId instanceof Long && userId.equals(1L)
        ));
    }

    @Test
    void testDoFilterInternal_AlwaysCallsFilterChain() throws ServletException, IOException {
        // 测试各种情况下都会调用 filterChain.doFilter
        
        // Case 1: No token
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(null);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
        
        // Case 2: Token in blacklist
        reset(filterChain);
        when(jwtTokenService.getTokenFromCookie(request)).thenReturn(TEST_TOKEN);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(true);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
        
        // Case 3: Valid token with user found
        reset(filterChain);
        when(blackListService.isTokenInBlackList(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(TEST_TOKEN)).thenReturn(TEST_EMAIL);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userService.loadUserByUsername(TEST_EMAIL)).thenReturn(testUser);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }
}