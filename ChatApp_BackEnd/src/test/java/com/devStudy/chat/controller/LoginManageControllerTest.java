package com.devStudy.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.devStudy.chat.dto.CreateCompteDTO;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.service.implementations.JwtTokenService;
import com.devStudy.chat.service.implementations.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LoginManageControllerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoginManageControllerTest.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	UserService userService;
	
	@MockBean
	JwtTokenService jwtTokenService;
	
	@BeforeAll
	void TestStart() {
		LOGGER.info("-----------------------------------------Test LoginManageController started-----------------------------------------------");
	}
	
	@AfterAll
	void TestEnd() {
		LOGGER.info("-----------------------------------------Test LoginManageController ended-------------------------------------------------");
	}
	
//	// Test getloggedUser() method, if the user is logged in
//	@Test
//	void testGetLoggedUser_loggedIn() throws Exception {
//        UserDTO userMocked = new UserDTO();
//        userMocked.setId(1L);
//        userMocked.setFirstName("AAA");
//        userMocked.setLastName("aaa");
//        userMocked.setMail("AAA@aaa.com");
//
//		// Mock the service
//		when(
//		when(userService.getLoggedUser()).thenReturn(userMocked);
//
//		// apply mvc test
//		mockMvc.perform(get("/api/login/check-login").with(csrf()))
//					.andExpect(status().isOk())
//					.andExpect(jsonPath("$.id").value(1))
//					.andExpect(jsonPath("$.firstName").value("AAA"))
//					.andExpect(jsonPath("$.lastName").value("aaa"))
//					.andExpect(jsonPath("$.mail").value("AAA@aaa.com"));
//
//	}
//
//	// Test getloggedUser() method, when the user is deleted in the database
//	@Test
//    void testGetLoggedUser_WhenUserIsDeleted() throws Exception {
//        // Mock userService return false
//        when(userService.checkUserLoginStatus()).thenReturn(false);
//
//        // setup security context
//        Authentication authentication = new AnonymousAuthenticationToken(
//        		"key",
//        		"anonymous",
//        		Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication());
//
//        mockMvc.perform(get("/api/login/check-login")
//                .with(csrf())
//                .with(authentication(authentication)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(0));
//
//        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
//    }
//
//	// Test getloggedUser() method, if the user is not logged in
//	@Test
//	void testGetLoggedUser_WhenUserIsNotLoggedIn() throws Exception {
//		// Mock userService return false
//		when(userService.checkUserLoginStatus()).thenReturn(false);
//
//		mockMvc.perform(get("/api/login/check-login").with(csrf()))
//				.andExpect(status().isOk())
//				.andExpect(jsonPath("$.id").value(0));
//	}

	// Test validateToken() method
    @Test
    void testValidateToken() throws Exception {
        String token = "valid-token";
        when(jwtTokenService.validateToken(token)).thenReturn(true);

        mockMvc.perform(get("/api/login/validate-token")
                .param("token", token)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
    
    // Test resetPassword() method
    @Test
    void testResetPassword() throws Exception {
        String token = "valid-token";
        String password = "newPassword123";
        
        when(userService.resetPassword(token, password)).thenReturn(true);

        mockMvc.perform(multipart("/api/login/reset-password")
                .param("token", token)
                .param("password", password)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // Test createUserCompte() method
    @Test
    void testCreateUserCompte() throws Exception {
        CreateCompteDTO expectedResponse = new CreateCompteDTO();
        expectedResponse.setMail("test@example.com");
        
        when(userService.addUser(any(CreateCompteDTO.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/login/compte/create")
        		.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expectedResponse))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mail").value("test@example.com"));
    }
}
