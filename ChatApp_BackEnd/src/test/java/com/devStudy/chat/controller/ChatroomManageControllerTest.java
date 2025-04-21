package com.devStudy.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.devStudy.chat.dto.ChatMsgDTO;
import com.devStudy.chat.dto.ChatroomRequestDTO;
import com.devStudy.chat.dto.ModifyChatroomDTO;
import com.devStudy.chat.dto.ModifyChatroomRequestDTO;
import com.devStudy.chat.dto.UserDTO;
import com.devStudy.chat.service.implementations.ChatMessageService;
import com.devStudy.chat.service.implementations.ChatroomService;
import com.devStudy.chat.service.implementations.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ChatroomManageControllerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChatroomManageControllerTest.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private UserService userService;
	
	@MockBean
	private ChatroomService chatroomService;
	
	@MockBean
	private ChatMessageService chatMessageService;
	
	@BeforeAll
	void TestStart() {
		LOGGER.info("-----------------------------------------Test ChatroomManageController started-----------------------------------------------");
	}
	
	@AfterAll
	void TestEnd() {
		LOGGER.info("-----------------------------------------Test ChatroomManageController ended-----------------------------------------------");
	}
	
	@BeforeEach
	void TestSetup() {
		when(userService.getUserId(any(HttpServletRequest.class))).thenReturn(1L);
	}
	
	@Test
	void testCreateChatroom() throws Exception {
		String url = "/api/chatrooms/create";
		ChatroomRequestDTO chatroomRequestDTO = new ChatroomRequestDTO();
		chatroomRequestDTO.setTitre("Test title");
		
		when(chatroomService.createChatroom(any(ChatroomRequestDTO.class),eq(1L))).thenReturn(true);
		
		// test createChatroom successfully
		mockMvc.perform(post(url)
					.with(csrf()).with(user("user").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(chatroomRequestDTO)))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));
		
		// test createChatroom failed
		when(chatroomService.createChatroom(any(ChatroomRequestDTO.class), eq(1L))).thenReturn(false);
		mockMvc.perform(post(url)
					.with(csrf()).with(user("user").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(chatroomRequestDTO)))
				.andExpect(status().isConflict())
				.andExpect(content().string("false"));
		
		// test createChatroom with unauthorized user
		mockMvc.perform(post(url)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(chatroomRequestDTO)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testDeleteChatroom() throws Exception {
		String url = "/api/chatrooms/";
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(true);

		// test deleteChatroom successfully
		when(chatroomService.deleteChatRoom(1L)).thenReturn(true);
		mockMvc.perform(delete(url+'1')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		// test deleteChatroom failed
		when(chatroomService.deleteChatRoom(1L)).thenReturn(false);
		mockMvc.perform(delete(url+'1')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isConflict())
				.andExpect(content().string("false"));

		// test deleteChatroom with forbidden user
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(false);
		mockMvc.perform(delete(url+'2')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isForbidden())
				.andExpect(content().string("false"));
		
		// test deleteChatroom with unauthorized user
		mockMvc.perform(delete(url+'1')
					.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetChatroomForModify() throws Exception {
		String url = "/api/chatrooms/";
		ModifyChatroomDTO modifyChatroomDTO = new ModifyChatroomDTO();
		modifyChatroomDTO.setId(1L);
		when(chatroomService.findChatroom(1L)).thenReturn(Optional.of(modifyChatroomDTO));
		
		// test getChatroomForModify successfully
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(true);
		mockMvc.perform(get(url+'1')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
		
		// test getChatroomForModify failed (forbidden user)
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(false);
		mockMvc.perform(get(url+'2')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.id").value(0));
		
		// test getChatroomForModify failed (chatroom not found)
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(true);
		when(chatroomService.findChatroom(1L)).thenReturn(Optional.empty());
		mockMvc.perform(get(url+'1')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.id").value(0));
		
		// test getChatroomForModify with unauthorized user
		mockMvc.perform(get(url+'1')
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetUsersInvitedInChatroom() throws Exception {
		String url = "/api/chatrooms/";
		when(chatroomService.checkUserIsOwnerOfChatroom(1, 1)).thenReturn(true);

		// test getUsersInvitedInChatroom successfully
		UserDTO userInvited = new UserDTO();
		userInvited.setId(2L);
		when(userService.findUsersInvitedToChatroomByPage(1L, 0)).thenReturn(
			new PageImpl<>(List.of(userInvited))
		);
		mockMvc.perform(get(url+"1/users/invited")
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].id").exists())
				.andExpect(jsonPath("$.content[0].id").value(2));

		// test getUsersInvitedInChatroom failed (forbidden user)
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(false);
		mockMvc.perform(get(url+"2/users/invited")
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content").isEmpty());

		// test getUsersInvitedInChatroom with unauthorized user
		mockMvc.perform(get(url+"1/users/invited")
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetUsersNotInvitedInChatroom() throws Exception {
        String url = "/api/chatrooms/";
        when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(true);

        // test getUsersNotInvitedInChatroom successfully
        UserDTO userNotInvited = new UserDTO();
        userNotInvited.setId(2L);
        when(userService.findUsersNotInvitedToChatroomByPage(1L, 1L, 0)).thenReturn(
            new PageImpl<>(List.of(userNotInvited))
        );
        mockMvc.perform(get(url+"1/users/not-invited")
                    .with(csrf()).with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].id").value(2));

        // test getUsersNotInvitedInChatroom failed (forbidden user)
        when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(false);
        mockMvc.perform(get(url+"2/users/not-invited")
                    .with(csrf()).with(user("user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        // test getUsersNotInvitedInChatroom with unauthorized user
        mockMvc.perform(get(url+"1/users/not-invited")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
	
	@Test
	void testUpdateChatroomDetails() throws Exception {
		String url = "/api/chatrooms/";
		ModifyChatroomRequestDTO modifyChatroomDTO = new ModifyChatroomRequestDTO();
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(true);

		// test updateChatroomDetails successfully
		when(chatroomService.updateChatroom(any(ModifyChatroomRequestDTO.class), eq(1L))).thenReturn(true);
		mockMvc.perform(put(url+'1')
					.with(csrf()).with(user("user").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(modifyChatroomDTO)))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		// test updateChatroomDetails failed
		when(chatroomService.updateChatroom(any(ModifyChatroomRequestDTO.class), eq(1L))).thenReturn(false);
		mockMvc.perform(put(url+'1')
					.with(csrf()).with(user("user").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(modifyChatroomDTO)))
				.andExpect(status().isConflict())
				.andExpect(content().string("false"));

		// test updateChatroomDetails with forbidden user
		when(chatroomService.checkUserIsOwnerOfChatroom(1L, 1L)).thenReturn(false);
		mockMvc.perform(put(url+'2')
					.with(csrf()).with(user("user").roles("USER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(modifyChatroomDTO)))
				.andExpect(status().isForbidden())
				.andExpect(content().string("false"));

		// test updateChatroomDetails with unauthorized user
		mockMvc.perform(put(url+'1')
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(modifyChatroomDTO)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testLeaveChatroom() throws Exception {
		String url = "/api/chatrooms/1/users/invited/";

		// test leaveChatroom successfully
		when(chatroomService.deleteUserInvited(1L, 1L)).thenReturn(true);
		mockMvc.perform(delete(url+'1')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		// test leaveChatroom failed (server error)
		when(chatroomService.deleteUserInvited(1L, 1L)).thenReturn(false);
		mockMvc.perform(delete(url+'1')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string("false"));

		// test leaveChatroom failed (forbidden user)
		mockMvc.perform(delete(url+'2')
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isForbidden())
				.andExpect(content().string("false"));
		
		// test leaveChatroom with unauthorized user
		mockMvc.perform(delete(url+'1')
					.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetAllMembersInChatroom() throws Exception{
		String url = "/api/chatrooms/";
		UserDTO user = new UserDTO();
		user.setId(2L);
		when(chatroomService.getAllUsersInChatroom(1L)).thenReturn(List.of(user));
	
		// test getAllMembersInChatroom successfully
		mockMvc.perform(get(url+"1/members")
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].id").value(2));
		
		// test getAllMembersInChatroom failed
		when(chatroomService.getAllUsersInChatroom(1L)).thenReturn(List.of());
		mockMvc.perform(get(url+"1/members")
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
		
		// test getAllMembersInChatroom with unauthorized user
		mockMvc.perform(get(url+"1/members")
					.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	void testGetHistoryMsgByChatroomIdAndPage() throws Exception {
		String url = "/api/chatrooms/";
		ChatMsgDTO chatMsgDTO = new ChatMsgDTO();
		chatMsgDTO.setMessage("Test message");
		when(chatMessageService.getChatMessagesByChatroomIdByPage(1L, 0)).thenReturn(List.of(chatMsgDTO));

		// test getHistoryMsgByChatroomIdAndPage successfully
		mockMvc.perform(get(url+"1/history")
					.with(csrf()).with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].message").value("Test message"));

		// test getHistoryMsgByChatroomIdAndPage with unauthorized user
		mockMvc.perform(get(url+"1/history")
					.with(csrf()))
				.andExpect(status().isUnauthorized());
	}
}
