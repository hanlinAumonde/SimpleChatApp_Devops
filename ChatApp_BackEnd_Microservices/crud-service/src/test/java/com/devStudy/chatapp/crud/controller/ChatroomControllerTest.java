package com.devStudy.chatapp.crud.controller;

import com.devStudy.chatapp.crud.dto.*;
import com.devStudy.chatapp.crud.service.Implementation.ChatroomService;
import com.devStudy.chatapp.crud.service.Implementation.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatroomController.class)
class ChatroomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ChatroomService chatroomService;

    @Autowired
    private ObjectMapper objectMapper;

    private ChatroomRequestDTO chatroomRequestDTO;
    private ModifyChatroomRequestDTO modifyChatroomRequestDTO;
    private ModifyChatroomDTO modifyChatroomDTO;
    private ChatroomDTO chatroomDTO;
    private ChatroomWithOwnerAndStatusDTO chatroomWithOwnerDTO;
    private UserDTO userDTO;
    private Page<UserDTO> userPage;
    private Page<ChatroomDTO> chatroomPage;
    private Page<ChatroomWithOwnerAndStatusDTO> chatroomWithOwnerPage;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setMail("john@test.com");
        
        chatroomRequestDTO = new ChatroomRequestDTO();
        chatroomRequestDTO.setTitre("Test Chatroom");
        chatroomRequestDTO.setDescription("Test Description");
        chatroomRequestDTO.setStartDate(LocalDateTime.now().plusHours(1).toString());
        chatroomRequestDTO.setDuration_days(7);
        chatroomRequestDTO.setUsersInvited(Collections.singletonList(userDTO));

        modifyChatroomRequestDTO = new ModifyChatroomRequestDTO();
        modifyChatroomRequestDTO.setTitre("Updated Chatroom");
        modifyChatroomRequestDTO.setDescription("Updated Description");
        modifyChatroomRequestDTO.setStartDate(LocalDateTime.now().plusHours(2).toString());
        modifyChatroomRequestDTO.setDuration(10);
        modifyChatroomRequestDTO.setListAddedUsers(Collections.emptyList());
        modifyChatroomRequestDTO.setListRemovedUsers(Collections.emptyList());

        modifyChatroomDTO = new ModifyChatroomDTO();
        modifyChatroomDTO.setTitre("Test Chatroom");
        modifyChatroomDTO.setDescription("Test Description");

        chatroomDTO = new ChatroomDTO();
        chatroomDTO.setId(100L);
        chatroomDTO.setTitre("Test Chatroom");
        chatroomDTO.setDescription("Test Description");
        chatroomDTO.setIsActif(true);
        
        chatroomWithOwnerDTO = new ChatroomWithOwnerAndStatusDTO();
        chatroomWithOwnerDTO.setId(100L);
        chatroomWithOwnerDTO.setTitre("Test Chatroom");
        chatroomWithOwnerDTO.setDescription("Test Description");

        userPage = new PageImpl<>(Collections.singletonList(userDTO));
        chatroomPage = new PageImpl<>(Collections.singletonList(chatroomDTO));
        chatroomWithOwnerPage = new PageImpl<>(Collections.singletonList(chatroomWithOwnerDTO));
    }

    @Test
    void testCreateChatroom_Success() throws Exception {
        // 测试成功创建聊天室
        String userIdHeader = "123";
        long userId = 123L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.createChatroom(any(ChatroomRequestDTO.class), eq(userId))).thenReturn(true);

        mockMvc.perform(post("/api/chatrooms/create")
                .header("X-User-Id", userIdHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatroomRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCreateChatroom_Failure() throws Exception {
        // 测试创建聊天室失败
        String userIdHeader = "123";
        long userId = 123L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.createChatroom(any(ChatroomRequestDTO.class), eq(userId))).thenReturn(false);

        mockMvc.perform(post("/api/chatrooms/create")
                .header("X-User-Id", userIdHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatroomRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(content().string("false"));
    }

    @Test
    void testDeleteChatroom_Success() throws Exception {
        // 测试成功删除聊天室
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(true);
        when(chatroomService.deleteChatRoom(chatroomId)).thenReturn(true);

        mockMvc.perform(delete("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testDeleteChatroom_NotOwner() throws Exception {
        // 测试非所有者删除聊天室
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(false);

        mockMvc.perform(delete("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isForbidden())
                .andExpect(content().string("false"));
    }

    @Test
    void testDeleteChatroom_ServiceFailure() throws Exception {
        // 测试服务删除失败
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(true);
        when(chatroomService.deleteChatRoom(chatroomId)).thenReturn(false);

        mockMvc.perform(delete("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isConflict())
                .andExpect(content().string("false"));
    }

    @Test
    void testGetChatroomForModify_Success() throws Exception {
        // 测试成功获取聊天室修改信息
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(true);
        when(chatroomService.findChatroom(chatroomId)).thenReturn(Optional.of(modifyChatroomDTO));

        mockMvc.perform(get("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value(modifyChatroomDTO.getTitre()))
                .andExpect(jsonPath("$.description").value(modifyChatroomDTO.getDescription()));
    }

    @Test
    void testGetChatroomForModify_NotOwner() throws Exception {
        // 测试非所有者获取聊天室修改信息
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(false);

        mockMvc.perform(get("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetChatroomForModify_NotFound() throws Exception {
        // 测试聊天室不存在
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(true);
        when(chatroomService.findChatroom(chatroomId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUsersInvitedInChatroom_Success() throws Exception {
        // 测试成功获取聊天室邀请用户
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(true);
        when(userService.findUsersInvitedToChatroomByPage(chatroomId, 0)).thenReturn(userPage);

        mockMvc.perform(get("/api/chatrooms/{chatroomId}/users/invited", chatroomId)
                .header("X-User-Id", userIdHeader)
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void testGetUsersInvitedInChatroom_NotOwner() throws Exception {
        // 测试非所有者获取邀请用户
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(false);

        mockMvc.perform(get("/api/chatrooms/{chatroomId}/users/invited", chatroomId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void testUpdateChatroomDetails_Success() throws Exception {
        // 测试成功更新聊天室详情
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(true);
        when(chatroomService.updateChatroom(any(ModifyChatroomRequestDTO.class), eq(chatroomId))).thenReturn(true);

        mockMvc.perform(put("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifyChatroomRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testUpdateChatroomDetails_NotOwner() throws Exception {
        // 测试非所有者更新聊天室
        String userIdHeader = "123";
        long userId = 123L;
        long chatroomId = 100L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.checkUserIsOwnerOfChatroom(userId, chatroomId)).thenReturn(false);

        mockMvc.perform(put("/api/chatrooms/{chatroomId}", chatroomId)
                .header("X-User-Id", userIdHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifyChatroomRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("false"));
    }

    @Test
    void testLeaveChatroom_Success() throws Exception {
        // 测试成功离开聊天室
        String userIdHeader = "123";
        long currentUserId = 123L;
        long chatroomId = 100L;
        long userId = 123L; // 同一个用户

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(currentUserId);
        when(chatroomService.deleteUserInvited(chatroomId, userId)).thenReturn(true);

        mockMvc.perform(delete("/api/chatrooms/{chatroomId}/users/invited/{userId}", chatroomId, userId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testLeaveChatroom_NotSameUser() throws Exception {
        // 测试非本人尝试离开聊天室
        String userIdHeader = "123";
        long currentUserId = 123L;
        long chatroomId = 100L;
        long userId = 456L; // 不同用户

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(currentUserId);

        mockMvc.perform(delete("/api/chatrooms/{chatroomId}/users/invited/{userId}", chatroomId, userId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isForbidden())
                .andExpect(content().string("false"));
    }

    @Test
    void testGetAllMembersInChatroom_Success() throws Exception {
        // 测试成功获取聊天室所有成员
        long chatroomId = 100L;
        List<UserDTO> members = Collections.singletonList(userDTO);

        when(chatroomService.getAllUsersInChatroom(chatroomId)).thenReturn(members);

        mockMvc.perform(get("/api/chatrooms/{chatroomId}/members", chatroomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userDTO.getId()));
    }

    @Test
    void testGetAllMembersInChatroom_Empty() throws Exception {
        // 测试获取空成员列表
        long chatroomId = 100L;

        when(chatroomService.getAllUsersInChatroom(chatroomId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chatrooms/{chatroomId}/members", chatroomId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetChatroomsOwnedByUser_Success() throws Exception {
        // 测试成功获取用户拥有的聊天室
        String userIdHeader = "123";
        long userId = 123L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.getChatroomsOwnedOfUserByPage(userId, 0)).thenReturn(chatroomPage);

        mockMvc.perform(get("/api/chatrooms/users/{userId}/owned", userId)
                .header("X-User-Id", userIdHeader)
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(chatroomDTO.getId()));
    }

    @Test
    void testGetChatroomsOwnedByUser_DifferentUser() throws Exception {
        // 测试访问其他用户的聊天室
        String userIdHeader = "123";
        long currentUserId = 123L;
        long targetUserId = 456L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(currentUserId);

        mockMvc.perform(get("/api/chatrooms/users/{userId}/owned", targetUserId)
                .header("X-User-Id", userIdHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void testGetChatroomsJoinedByUser_Success() throws Exception {
        // 测试成功获取用户加入的聊天室
        String userIdHeader = "123";
        long userId = 123L;

        when(userService.getUserIdFromHeaders(userIdHeader)).thenReturn(userId);
        when(chatroomService.getChatroomsJoinedOfUserByPage(userId, false, 0)).thenReturn(chatroomWithOwnerPage);

        mockMvc.perform(get("/api/chatrooms/users/{userId}/joined", userId)
                .header("X-User-Id", userIdHeader)
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(chatroomWithOwnerDTO.getId()));
    }

    @Test
    void testMissingRequiredHeaders() throws Exception {
        // 测试缺少必需的请求头
        mockMvc.perform(post("/api/chatrooms/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatroomRequestDTO)))
                .andExpect(status().is4xxClientError());
    }
}