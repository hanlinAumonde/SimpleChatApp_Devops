package com.devStudy.chatapp.message.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.devStudy.chatapp.message.TestDataFactory;
import com.devStudy.chatapp.message.service.Implementation.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.devStudy.chatapp.message.dto.ChatMsgDTO;
import com.devStudy.chatapp.message.dto.UserDTO;
import com.devStudy.chatapp.message.model.ChatMessage;
import com.devStudy.chatapp.message.repository.ChatMessageRepository;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private UserDTO testUser;
    private ChatMessage testMessage;
    private final long testChatroomId = 1L;
    private final long testUserId = 4L;

    @BeforeEach
    void setUp() {
        // 设置配置值
        ReflectionTestUtils.setField(chatMessageService, "DefaultPageSize_Messages", 5);
        
        // 创建测试数据
        testUser = TestDataFactory.createTestUser(testUserId);
        testMessage = TestDataFactory.createTestChatMessage(
            testChatroomId, 
            testUser, 
            "Test message content", 
            TestDataFactory.createTestDate(2025, 8, 28, 10, 30)
        );
    }

    @Test
    void testSaveMsgIntoCollection() {
        // Given
        Date timestamp = new Date();
        String content = "Test message";
        
        // When
        chatMessageService.saveMsgIntoCollection(testChatroomId, testUser, content, timestamp);
        
        // Then
        verify(chatMessageRepository, times(1)).insert(any(ChatMessage.class));
    }

    @Test
    void testGetChatMessagesByChatroomId() {
        // Given
        List<ChatMessage> mockMessages = Collections.singletonList(testMessage);
        when(chatMessageRepository.findByChatroomId(testChatroomId)).thenReturn(mockMessages);
        
        // When
        List<ChatMsgDTO> result = chatMessageService.getChatMessagesByChatroomId(testChatroomId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 验证结果包含日期标志和消息内容
        boolean hasDateSign = result.stream().anyMatch(msg -> "dateSign".equals(msg.getMessageType()));
        boolean hasContent = result.stream().anyMatch(msg -> "content".equals(msg.getMessageType()));
        boolean hasLatestDateSign = result.stream().anyMatch(msg -> "latestDateSign".equals(msg.getMessageType()));
        
        assertTrue(hasDateSign, "应包含日期标志");
        assertTrue(hasContent, "应包含消息内容");
        assertTrue(hasLatestDateSign, "应包含最新日期标志");
        
        verify(chatMessageRepository, times(1)).findByChatroomId(testChatroomId);
    }

    @Test
    void testGetChatMessagesByChatroomIdByPage() {
        // Given
        List<ChatMessage> mockMessages = Collections.singletonList(testMessage);
        Page<ChatMessage> mockPage = new PageImpl<>(mockMessages);
        when(chatMessageRepository.findByChatroomId(eq(testChatroomId), any(Pageable.class)))
                .thenReturn(mockPage);
        
        // When
        List<ChatMsgDTO> result = chatMessageService.getChatMessagesByChatroomIdByPage(testChatroomId, 0);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 验证消息中的sentByUser字段为false（因为没有指定当前用户）
        ChatMsgDTO contentMsg = result.stream()
            .filter(msg -> "content".equals(msg.getMessageType()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(contentMsg);
        assertFalse(contentMsg.isSentByUser(), "未指定当前用户时，sentByUser应为false");
        
        verify(chatMessageRepository, times(1)).findByChatroomId(eq(testChatroomId), any(Pageable.class));
    }

    @Test
    void testGetChatMessagesByChatroomIdByPageWithUserId() {
        // Given
        List<ChatMessage> mockMessages = Collections.singletonList(testMessage);
        Page<ChatMessage> mockPage = new PageImpl<>(mockMessages);
        when(chatMessageRepository.findByChatroomId(eq(testChatroomId), any(Pageable.class)))
                .thenReturn(mockPage);
        
        // When - 使用消息发送者的用户ID
        List<ChatMsgDTO> result = chatMessageService.getChatMessagesByChatroomIdByPage(
            testChatroomId, 0, testUserId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 验证消息中的sentByUser字段为true（因为是当前用户发送的消息）
        ChatMsgDTO contentMsg = result.stream()
            .filter(msg -> "content".equals(msg.getMessageType()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(contentMsg);
        assertTrue(contentMsg.isSentByUser(), "当前用户发送的消息，sentByUser应为true");
        assertEquals(testUserId, contentMsg.getUserId());
        assertEquals("Test User4", contentMsg.getUsername());
        
        verify(chatMessageRepository, times(1)).findByChatroomId(eq(testChatroomId), any(Pageable.class));
    }

    @Test
    void testEmptyMessageList() {
        // Given
        when(chatMessageRepository.findByChatroomId(testChatroomId)).thenReturn(List.of());
        
        // When
        List<ChatMsgDTO> result = chatMessageService.getChatMessagesByChatroomId(testChatroomId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "空消息列表应返回空结果");
        
        verify(chatMessageRepository, times(1)).findByChatroomId(testChatroomId);
    }

    @Test
    void testMultipleMessagesWithDifferentDates() {
        // Given
        UserDTO user1 = TestDataFactory.createTestUser(1L);
        UserDTO user2 = TestDataFactory.createTestUser(2L);
        
        ChatMessage msg1 = TestDataFactory.createTestChatMessage(
            testChatroomId, user1, "Message 1", 
            TestDataFactory.createTestDate(2025, 8, 28, 9, 0));
        
        ChatMessage msg2 = TestDataFactory.createTestChatMessage(
            testChatroomId, user2, "Message 2", 
            TestDataFactory.createTestDate(2025, 8, 28, 10, 0));
        
        ChatMessage msg3 = TestDataFactory.createTestChatMessage(
            testChatroomId, user1, "Message 3", 
            TestDataFactory.createTestDate(2025, 8, 29, 9, 0)); // 不同的日期
        
        List<ChatMessage> mockMessages = Arrays.asList(msg1, msg2, msg3);
        when(chatMessageRepository.findByChatroomId(testChatroomId)).thenReturn(mockMessages);
        
        // When
        List<ChatMsgDTO> result = chatMessageService.getChatMessagesByChatroomId(testChatroomId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 验证有多个日期标志（不同日期的消息会产生多个日期标志）
        long dateSignCount = result.stream()
            .filter(msg -> "dateSign".equals(msg.getMessageType()))
            .count();
        
        assertTrue(dateSignCount >= 1, "应至少有一个日期标志");
        
        // 验证有3条内容消息
        long contentCount = result.stream()
            .filter(msg -> "content".equals(msg.getMessageType()))
            .count();
        
        assertEquals(3, contentCount, "应有3条内容消息");
    }

    @Test
    void testSentByUserFlagForDifferentUsers() {
        // Given
        UserDTO otherUser = TestDataFactory.createTestUser(999L);
        ChatMessage otherMessage = TestDataFactory.createTestChatMessage(
            testChatroomId, otherUser, "Other user message", 
            TestDataFactory.createTestDate(2025, 8, 28, 11, 0));
        
        List<ChatMessage> mockMessages = Arrays.asList(testMessage, otherMessage);
        Page<ChatMessage> mockPage = new PageImpl<>(mockMessages);
        when(chatMessageRepository.findByChatroomId(eq(testChatroomId), any(Pageable.class)))
                .thenReturn(mockPage);
        
        // When - 使用testUserId作为当前用户
        List<ChatMsgDTO> result = chatMessageService.getChatMessagesByChatroomIdByPage(
            testChatroomId, 0, testUserId);
        
        // Then
        List<ChatMsgDTO> contentMessages = result.stream()
            .filter(msg -> "content".equals(msg.getMessageType()))
            .toList();
        
        assertEquals(2, contentMessages.size(), "应有2条内容消息");
        
        // 验证第一条消息（testUserId发送）
        ChatMsgDTO userMessage = contentMessages.stream()
            .filter(msg -> msg.getUserId() == testUserId)
            .findFirst()
            .orElse(null);
        assertNotNull(userMessage);
        assertTrue(userMessage.isSentByUser(), "当前用户发送的消息应标记为true");
        
        // 验证第二条消息（其他用户发送）
        ChatMsgDTO otherUserMessage = contentMessages.stream()
            .filter(msg -> msg.getUserId() == 999L)
            .findFirst()
            .orElse(null);
        assertNotNull(otherUserMessage);
        assertFalse(otherUserMessage.isSentByUser(), "其他用户发送的消息应标记为false");
    }
}