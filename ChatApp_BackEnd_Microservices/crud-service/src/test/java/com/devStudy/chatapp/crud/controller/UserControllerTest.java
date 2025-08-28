package com.devStudy.chatapp.crud.controller;

import com.devStudy.chatapp.crud.dto.UserDTO;
import com.devStudy.chatapp.crud.service.Implementation.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserDTO user1;
    private UserDTO user2;
    private Page<UserDTO> userPage;

    @BeforeEach
    void setUp() {
        user1 = new UserDTO();
        user1.setId(1L);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setMail("john@test.com");

        user2 = new UserDTO();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setMail("jane@test.com");
        
        List<UserDTO> userList = Arrays.asList(user1, user2);
        userPage = new PageImpl<>(userList);
    }

    @Test
    void testGetOtherUsers_Success() throws Exception {
        // 测试成功获取其他用户
        String userIdHeader = "123";
        long userId = 123L;
        
        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(userService.findAllOtherUsersNotAdminByPage(0, userId)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/others")
                .header("X-User-Id", userIdHeader)
                .param("page", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(user1.getId()))
                .andExpect(jsonPath("$.content[0].firstName").value(user1.getFirstName()))
                .andExpect(jsonPath("$.content[1].id").value(user2.getId()))
                .andExpect(jsonPath("$.content[1].firstName").value(user2.getFirstName()));
    }

    @Test
    void testGetOtherUsers_DefaultPage() throws Exception {
        // 测试默认分页参数
        String userIdHeader = "123";
        long userId = 123L;
        
        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(userService.findAllOtherUsersNotAdminByPage(0, userId)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/others")
                .header("X-User-Id", userIdHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void testGetUsersInvitedToChatroom_Success() throws Exception {
        // 测试成功获取聊天室邀请用户
        long chatroomId = 100L;
        
        when(userService.findUsersInvitedToChatroomByPage(chatroomId, 0)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/invited-to-chatroom")
                .param("chatroomId", String.valueOf(chatroomId))
                .param("page", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(user1.getId()))
                .andExpect(jsonPath("$.content[1].id").value(user2.getId()));
    }

    @Test
    void testGetUsersInvitedToChatroom_DefaultPage() throws Exception {
        // 测试默认分页参数
        long chatroomId = 100L;
        
        when(userService.findUsersInvitedToChatroomByPage(chatroomId, 0)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/invited-to-chatroom")
                .param("chatroomId", String.valueOf(chatroomId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void testGetUsersNotInvitedToChatroom_Success() throws Exception {
        // 测试成功获取未邀请到聊天室的用户
        long chatroomId = 100L;
        String userIdHeader = "123";
        long userId = 123L;
        
        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(userService.findUsersNotInvitedToChatroomByPage(chatroomId, userId, 0)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/not-invited-to-chatroom")
                .header("X-User-Id", userIdHeader)
                .param("chatroomId", String.valueOf(chatroomId))
                .param("page", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(user1.getId()))
                .andExpect(jsonPath("$.content[1].id").value(user2.getId()));
    }

    @Test
    void testGetUsersNotInvitedToChatroom_DefaultPage() throws Exception {
        // 测试默认分页参数
        long chatroomId = 100L;
        String userIdHeader = "123";
        long userId = 123L;
        
        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(userService.findUsersNotInvitedToChatroomByPage(chatroomId, userId, 0)).thenReturn(userPage);

        mockMvc.perform(get("/api/users/not-invited-to-chatroom")
                .header("X-User-Id", userIdHeader)
                .param("chatroomId", String.valueOf(chatroomId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void testGetOtherUsers_EmptyResult() throws Exception {
        // 测试空结果
        String userIdHeader = "123";
        long userId = 123L;
        Page<UserDTO> emptyPage = new PageImpl<>(List.of());
        
        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(userService.findAllOtherUsersNotAdminByPage(0, userId)).thenReturn(emptyPage);

        mockMvc.perform(get("/api/users/others")
                .header("X-User-Id", userIdHeader)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void testMissingRequiredHeaders() throws Exception {
        // 测试缺少必需的请求头
        mockMvc.perform(get("/api/users/others")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testMissingRequiredParameters() throws Exception {
        // 测试缺少必需的参数
        mockMvc.perform(get("/api/users/invited-to-chatroom")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}