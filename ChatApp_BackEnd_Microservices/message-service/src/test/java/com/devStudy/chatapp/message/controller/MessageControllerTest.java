package com.devStudy.chatapp.message.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.devStudy.chatapp.message.TestDataFactory;
import com.devStudy.chatapp.message.dto.ChatMsgDTO;
import com.devStudy.chatapp.message.dto.SaveMessageRequest;
import com.devStudy.chatapp.message.dto.UserDTO;
import com.devStudy.chatapp.message.service.Implementation.ChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatMessageService chatMessageService;

    @Autowired
    private ObjectMapper objectMapper;

    private final long testChatroomId = 1L;
    private final long testUserId = 4L;
    private List<ChatMsgDTO> testMessages;

    @BeforeEach
    void setUp() {
        // 创建测试消息数据
        ChatMsgDTO dateSign = new ChatMsgDTO();
        dateSign.setIndex(0);
        dateSign.setTimestamp("2025-08-28");
        dateSign.setMessageType("dateSign");

        ChatMsgDTO contentMsg = new ChatMsgDTO();
        contentMsg.setIndex(1);
        contentMsg.setUserId(testUserId);
        contentMsg.setUsername("Test User");
        contentMsg.setMessage("Test message content");
        contentMsg.setTimestamp("10:30");
        contentMsg.setSentByUser(false);
        contentMsg.setMessageType("content");

        ChatMsgDTO latestDateSign = new ChatMsgDTO();
        latestDateSign.setIndex(2);
        latestDateSign.setTimestamp("2025-08-28");
        latestDateSign.setMessageType("latestDateSign");

        testMessages = Arrays.asList(dateSign, contentMsg, latestDateSign);
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_WithoutUserId() throws Exception {
        // Given
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, 0))
                .thenReturn(testMessages);

        // When & Then
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].messageType").value("dateSign"))
                .andExpect(jsonPath("$[0].timestamp").value("2025-08-28"))
                .andExpect(jsonPath("$[1].messageType").value("content"))
                .andExpect(jsonPath("$[1].userId").value(testUserId))
                .andExpect(jsonPath("$[1].username").value("Test User"))
                .andExpect(jsonPath("$[1].message").value("Test message content"))
                .andExpect(jsonPath("$[1].timestamp").value("10:30"))
                .andExpect(jsonPath("$[1].sentByUser").value(false))
                .andExpect(jsonPath("$[2].messageType").value("latestDateSign"));

        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, 0);
        verify(chatMessageService, never())
                .getChatMessagesByChatroomIdByPage(eq(testChatroomId), eq(0), anyLong());
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_WithValidUserId() throws Exception {
        // Given
        List<ChatMsgDTO> userMessages = Arrays.asList(
                testMessages.get(0), // dateSign
                createUserMessage(testUserId, true), // 当前用户的消息
                testMessages.get(2) // latestDateSign
        );
        
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, 0, testUserId))
                .thenReturn(userMessages);

        // When & Then
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .param("page", "0")
                        .header("X-User-Id", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].sentByUser").value(true)); // 验证sentByUser为true

        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, 0, testUserId);
        verify(chatMessageService, never())
                .getChatMessagesByChatroomIdByPage(eq(testChatroomId), eq(0));
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_WithInvalidUserId() throws Exception {
        // Given
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, 0))
                .thenReturn(testMessages);

        // When & Then
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .param("page", "0")
                        .header("X-User-Id", "invalid-user-id") // 无效的用户ID
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // 应该调用不带用户ID的方法，因为解析失败
        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, 0);
        verify(chatMessageService, never())
                .getChatMessagesByChatroomIdByPage(eq(testChatroomId), eq(0), anyLong());
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_DefaultPage() throws Exception {
        // Given
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, 0))
                .thenReturn(testMessages);

        // When & Then - 不提供page参数，应使用默认值0
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3));

        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, 0);
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_SpecificPage() throws Exception {
        // Given
        int pageNumber = 2;
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, pageNumber))
                .thenReturn(testMessages);

        // When & Then
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .param("page", String.valueOf(pageNumber))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, pageNumber);
    }

    @Test
    void testSaveMessage_SuccessWithoutTimestamp() throws Exception {
        // Given
        SaveMessageRequest request = new SaveMessageRequest();
        request.setSenderId(testUserId);
        request.setSenderFirstName("Test");
        request.setSenderLastName("User");
        request.setSenderMail("testuser@test.com");
        request.setContent("Test message content");
        request.setTimestamp(null); // 没有时间戳

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/messages/chatrooms/{chatroomId}/save", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist()); // 空响应体

        verify(chatMessageService, times(1))
                .saveMsgIntoCollection(eq(testChatroomId), any(UserDTO.class), eq("Test message content"), any(Date.class));
    }

    @Test
    void testSaveMessage_SuccessWithTimestamp() throws Exception {
        // Given
        Date customTimestamp = TestDataFactory.createTestDate(2025, 8, 28, 12, 0);
        
        SaveMessageRequest request = new SaveMessageRequest();
        request.setSenderId(testUserId);
        request.setSenderFirstName("Test");
        request.setSenderLastName("User");
        request.setSenderMail("testuser@test.com");
        request.setContent("Test message with timestamp");
        request.setTimestamp(customTimestamp);

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/messages/chatrooms/{chatroomId}/save", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(chatMessageService, times(1))
                .saveMsgIntoCollection(eq(testChatroomId), any(UserDTO.class), eq("Test message with timestamp"), eq(customTimestamp));
    }

    @Test
    void testSaveMessage_VerifyUserDTOCreation() throws Exception {
        // Given
        SaveMessageRequest request = new SaveMessageRequest();
        request.setSenderId(123L);
        request.setSenderFirstName("John");
        request.setSenderLastName("Doe");
        request.setSenderMail("john.doe@example.com");
        request.setContent("Hello World");

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/messages/chatrooms/{chatroomId}/save", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // 验证UserDTO的创建和参数传递
        verify(chatMessageService, times(1)).saveMsgIntoCollection(
                eq(testChatroomId),
                argThat(user -> user.getId() == 123L 
                        && "John".equals(user.getFirstName())
                        && "Doe".equals(user.getLastName())
                        && "john.doe@example.com".equals(user.getMail())),
                eq("Hello World"),
                any(Date.class)
        );
    }

    @Test
    void testSaveMessage_EmptyContent() throws Exception {
        // Given
        SaveMessageRequest request = new SaveMessageRequest();
        request.setSenderId(testUserId);
        request.setSenderFirstName("Test");
        request.setSenderLastName("User");
        request.setSenderMail("testuser@test.com");
        request.setContent(""); // 空内容

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/messages/chatrooms/{chatroomId}/save", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(chatMessageService, times(1))
                .saveMsgIntoCollection(eq(testChatroomId), any(UserDTO.class), eq(""), any(Date.class));
    }

    @Test
    void testSaveMessage_InvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/messages/chatrooms/{chatroomId}/save", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isBadRequest());

        verify(chatMessageService, never())
                .saveMsgIntoCollection(anyLong(), any(UserDTO.class), anyString(), any(Date.class));
    }

    @Test
    void testSaveMessage_MissingFields() throws Exception {
        // Given - 缺少senderId字段的请求
        SaveMessageRequest request = new SaveMessageRequest();
        // request.setSenderId() - 故意不设置
        request.setSenderFirstName("Test");
        request.setSenderLastName("User");
        request.setSenderMail("testuser@test.com");
        request.setContent("Test message");

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/messages/chatrooms/{chatroomId}/save", testChatroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()); // 仍然成功，因为Controller没有验证

        // 验证使用默认值0作为senderId
        verify(chatMessageService, times(1)).saveMsgIntoCollection(
                eq(testChatroomId),
                argThat(user -> user.getId() == 0L), // 默认值
                eq("Test message"),
                any(Date.class)
        );
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_EmptyResult() throws Exception {
        // Given
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, 0))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, 0);
    }

    @Test
    void testGetHistoryMsgByChatroomIdAndPage_NegativePage() throws Exception {
        // Given
        int negativePage = -1;
        when(chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, negativePage))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/messages/chatrooms/{chatroomId}/history", testChatroomId)
                        .param("page", String.valueOf(negativePage))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(chatMessageService, times(1))
                .getChatMessagesByChatroomIdByPage(testChatroomId, negativePage);
    }

    private ChatMsgDTO createUserMessage(long userId, boolean sentByUser) {
        ChatMsgDTO msg = new ChatMsgDTO();
        msg.setIndex(1);
        msg.setUserId(userId);
        msg.setUsername("Test User" + userId);
        msg.setMessage("User message");
        msg.setTimestamp("10:30");
        msg.setSentByUser(sentByUser);
        msg.setMessageType("content");
        return msg;
    }
}