package com.devStudy.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.devStudy.chat.dto.ChatroomDTO;
import com.devStudy.chat.dto.ChatroomWithOwnerAndStatusDTO;
import com.devStudy.chat.service.implementations.ChatroomService;
import com.devStudy.chat.service.implementations.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserManageControllerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserManageControllerTest.class);
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private UserService userService;
	
	@MockBean
	private ChatroomService chatroomService;
	
	@BeforeAll
	void TestStart() {
		LOGGER.info("-----------------------------------------Test UserManageController started-----------------------------------------------");
	}
	
	@AfterAll
	void TestEnd() {
		LOGGER.info("-----------------------------------------Test UserManageController ended-----------------------------------------------");
	}
	
	@BeforeEach
	void TestSetup() {
		when(userService.getUserId(any(HttpServletRequest.class))).thenReturn(1L);
	}
	
	@Test
	void testGetOtherUsers() throws Exception {
		when(userService.findAllOtherUsersNotAdminByPage(0,1L)).thenReturn(Page.empty());
		
		mockMvc.perform(get("/api/users/others")
						.with(csrf())
						.with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content").isEmpty());
		
		mockMvc.perform(get("/api/users/others")
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetChatroomsOwnedByUser() throws Exception {
		when(chatroomService.getChatroomsOwnedOfUserByPage(1L, 0)).thenReturn(
				new PageImpl<>(List.of(new ChatroomDTO()))
		);
		
		// Test with the valid user id
		mockMvc.perform(get("/api/users/1/chatrooms/owned")
						.with(csrf())
						.with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].id").exists());
		
		// Test with the invalid user id
		mockMvc.perform(get("/api/users/2/chatrooms/owned")
						.with(csrf())
						.with(user("user").roles("USER")))
				.andExpect(status().isForbidden());
		
		mockMvc.perform(get("/api/users/1/chatrooms/owned")
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetChatroomsJoinedByUser() throws Exception {
		when(chatroomService.getChatroomsJoinedOfUserByPage(1L, false, 0))
				.thenReturn(new PageImpl<>(List.of(new ChatroomWithOwnerAndStatusDTO())));

		// Test with the valid user id
		mockMvc.perform(get("/api/users/1/chatrooms/joined")
						.with(csrf())
						.with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].id").exists());

		// Test with the invalid user id
		mockMvc.perform(get("/api/users/2/chatrooms/joined")
						.with(csrf())
						.with(user("user").roles("USER")))
				.andExpect(status().isForbidden());
		
		mockMvc.perform(get("/api/users/1/chatrooms/joined")
                        .with(csrf()))
		        .andExpect(status().isUnauthorized());
	}
	
}
