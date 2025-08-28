package com.devStudy.chatapp.auth.controller;

import com.devStudy.chatapp.auth.dto.CreateCompteDTO;
import com.devStudy.chatapp.auth.dto.UserDTO;
import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.service.Implementation.BlackListService;
import com.devStudy.chatapp.auth.service.Implementation.JwtTokenService;
import com.devStudy.chatapp.auth.service.Implementation.UserService;
import com.devStudy.chatapp.auth.utils.RabbitMQUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static com.devStudy.chatapp.auth.utils.ConstantValues.CreationSuccess;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private BlackListService blackListService;

    @MockBean
    private RabbitMQUtil rabbitMQUtil;

    private UserDTO testUserDTO;
    private User testUser;
    private CreateCompteDTO createCompteDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setFirstName("John");
        testUserDTO.setLastName("Doe");
        testUserDTO.setMail("john.doe@test.com");

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setMail("john.doe@test.com");
        testUser.setPwd("encodedPassword");
        testUser.setAdmin(false);
        testUser.setActive(true);

        createCompteDTO = new CreateCompteDTO();
        createCompteDTO.setFirstName("Jane");
        createCompteDTO.setLastName("Smith");
        createCompteDTO.setMail("jane.smith@test.com");
        createCompteDTO.setPassword("password123");
    }

    @Test
    void testGetLoggedUser_ValidToken() throws Exception {
        String validToken = "valid.jwt.token";
        
        when(jwtTokenService.getTokenFromCookie(any(HttpServletRequest.class)))
                .thenReturn(validToken);
        when(blackListService.isTokenInBlackList(validToken))
                .thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(validToken))
                .thenReturn("john.doe@test.com");
        when(userService.getLoggedUser("john.doe@test.com"))
                .thenReturn(testUserDTO);
        when(userService.loadUserByUsername(anyString()))
                .thenReturn(testUser);

        mockMvc.perform(get("/api/auth/check-login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.mail").value("john.doe@test.com"));

        verify(jwtTokenService).getTokenFromCookie(any(HttpServletRequest.class));
        verify(blackListService).isTokenInBlackList(validToken);
        verify(jwtTokenService).validateTokenAndGetEmail(validToken);
        verify(userService).getLoggedUser("john.doe@test.com");
    }

    @Test
    void testGetLoggedUser_NoToken() throws Exception {
        when(jwtTokenService.getTokenFromCookie(any(HttpServletRequest.class)))
                .thenReturn(null);

        mockMvc.perform(get("/api/auth/check-login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(0L));

        verify(jwtTokenService).getTokenFromCookie(any(HttpServletRequest.class));
        verify(blackListService, never()).isTokenInBlackList(anyString());
        verify(userService, never()).getLoggedUser(anyString());
    }

    @Test
    void testGetLoggedUser_TokenInBlackList() throws Exception {
        String blacklistedToken = "blacklisted.jwt.token";
        
        when(jwtTokenService.getTokenFromCookie(any(HttpServletRequest.class)))
                .thenReturn(blacklistedToken);
        when(blackListService.isTokenInBlackList(blacklistedToken))
                .thenReturn(true);

        mockMvc.perform(get("/api/auth/check-login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(0L));

        verify(blackListService).isTokenInBlackList(blacklistedToken);
        verify(jwtTokenService, never()).validateTokenAndGetEmail(anyString());
        verify(userService, never()).getLoggedUser(anyString());
    }

    @Test
    void testGetLoggedUser_UserNotFoundButTokenValid() throws Exception {
        String validToken = "valid.jwt.token";

        when(jwtTokenService.getTokenFromCookie(any(HttpServletRequest.class)))
                .thenReturn(validToken);
        when(blackListService.isTokenInBlackList(validToken))
                .thenReturn(false);
        when(jwtTokenService.validateTokenAndGetEmail(validToken))
                .thenReturn("john.doe@test.com");
        when(userService.loadUserByUsername(eq("john.doe@test.com")))
                .thenThrow(UsernameNotFoundException.class);
        when(userService.getLoggedUser(eq("john.doe@test.com")))
                .thenReturn(new UserDTO()); // Simulate user not found
        when(jwtTokenService.getExpirationDate(validToken))
                .thenReturn(Date.from(Instant.now().plusSeconds(60)));

        mockMvc.perform(get("/api/auth/check-login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(0L));

        verify(blackListService).addTokenToBlackList(eq(validToken), anyLong());
    }

    @Test
    void testGetVerificationCode_UserExists() throws Exception {
        String email = "john.doe@test.com";
        String successMessage = "Verification code sent successfully";
        
        when(userService.findUserOrAdmin(email, false))
                .thenReturn(Optional.of(testUser));
        when(rabbitMQUtil.sendVerificationCodeRequestToMQ(email))
                .thenReturn(successMessage);

        mockMvc.perform(get("/api/auth/verification-code")
                        .param("email", email)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.msg").value(successMessage));

        verify(userService).findUserOrAdmin(email, false);
        verify(rabbitMQUtil).sendVerificationCodeRequestToMQ(email);
    }

    @Test
    void testGetVerificationCode_UserNotExists() throws Exception {
        String email = "nonexistent@test.com";
        
        when(userService.findUserOrAdmin(email, false))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/verification-code")
                        .param("email", email)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.msg").value("User n'existe pas, veuillez vous inscrire"));

        verify(userService).findUserOrAdmin(email, false);
        verify(rabbitMQUtil, never()).sendVerificationCodeRequestToMQ(anyString());
    }

    @Test
    void testPostForgetPasswordPage() throws Exception {
        String email = "john.doe@test.com";
        Map<String, String> expectedResponse = Map.of(
                "status", "success",
                "message", "Reset password email sent"
        );
        
        when(rabbitMQUtil.sendResetPwdEmailRequestToMQ(email))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/auth/forget-password")
                        .param("email", email)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Reset password email sent"));

        verify(rabbitMQUtil).sendResetPwdEmailRequestToMQ(email);
    }

    @Test
    void testValidateToken_ValidToken() throws Exception {
        String validToken = "valid.reset.token";
        
        when(jwtTokenService.validateToken(validToken))
                .thenReturn(true);

        mockMvc.perform(get("/api/auth/validate-token")
                        .param("token", validToken)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("true"));

        verify(jwtTokenService).validateToken(validToken);
    }

    @Test
    void testValidateToken_InvalidToken() throws Exception {
        String invalidToken = "invalid.reset.token";
        
        when(jwtTokenService.validateToken(invalidToken))
                .thenReturn(false);

        mockMvc.perform(get("/api/auth/validate-token")
                        .param("token", invalidToken)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("false"));

        verify(jwtTokenService).validateToken(invalidToken);
    }

    @Test
    void testResetPassword_Success() throws Exception {
        String token = "valid.reset.token";
        String newPassword = "newPassword123";
        
        when(userService.resetPassword(token, newPassword))
                .thenReturn(true);

        mockMvc.perform(put("/api/auth/reset-password")
                        .param("token", token)
                        .param("password", newPassword)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("true"));

        verify(userService).resetPassword(token, newPassword);
    }

    @Test
    void testResetPassword_Failure() throws Exception {
        String token = "invalid.reset.token";
        String newPassword = "newPassword123";
        
        when(userService.resetPassword(token, newPassword))
                .thenReturn(false);

        mockMvc.perform(put("/api/auth/reset-password")
                        .param("token", token)
                        .param("password", newPassword)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("false"));

        verify(userService).resetPassword(token, newPassword);
    }

    @Test
    void testCreateUserCompte_Success() throws Exception {
        createCompteDTO.setCreateMsg(CreationSuccess);
        
        when(userService.addUser(any(CreateCompteDTO.class)))
                .thenReturn(createCompteDTO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCompteDTO))
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.mail").value("jane.smith@test.com"))
                .andExpect(jsonPath("$.createMsg").value(CreationSuccess));

        verify(userService).addUser(any(CreateCompteDTO.class));
    }

    @Test
    void testLogout_WithValidToken() throws Exception {
        String validToken = "valid.jwt.token";
        
        when(jwtTokenService.getTokenFromCookie(any(HttpServletRequest.class)))
                .thenReturn(validToken);
        when(jwtTokenService.validateToken(validToken))
                .thenReturn(true);
        when(jwtTokenService.validateTokenAndGetEmail(validToken))
                .thenReturn("john.doe@test.com");
        when(userService.loadUserByUsername(eq("john.doe@test.com")))
                .thenReturn(testUser);
        when(jwtTokenService.getExpirationDate(validToken))
                .thenReturn(Date.from(Instant.now().plusSeconds(60)));

        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(jwtTokenService).getTokenFromCookie(any(HttpServletRequest.class));
        verify(jwtTokenService).validateToken(validToken);
        verify(blackListService).addTokenToBlackList(eq(validToken), anyLong());
    }

    @Test
    void testLogout_WithoutToken() throws Exception {
        when(jwtTokenService.getTokenFromCookie(any(HttpServletRequest.class)))
                .thenReturn(null);

        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Unnecessary logout, you are not logged in"));

        verify(jwtTokenService).getTokenFromCookie(any(HttpServletRequest.class));
        verify(blackListService, never()).addTokenToBlackList(anyString(), anyLong());
    }
}